from __future__ import print_function

import datetime
import os
import sys
import time
import uuid

import logging
import logging.handlers
logger = logging.getLogger(__name__)

import requests

from . import __version__
from .config import Config, ConfigError
from .conversation import Conversation
from .net import GalileoClient, SyncError, BackOffException
from .tracker import FitbitClient
from .ui import InteractiveUI
from .utils import a2x
from . import dongle as dgl
from . import interactive

FitBitUUID = uuid.UUID('{ADAB0000-6E7D-4601-BDA2-BFFAA68956BA}')


def syncAllTrackers(config):
    logger.debug('%s initialising', os.path.basename(sys.argv[0]))
    dongle = dgl.FitBitDongle(config.logSize)
    if not dongle.setup():
        logger.error("No dongle connected, aborting")
        return

    fitbit = FitbitClient(dongle)

    galileo = GalileoClient('https', 'client.fitbit.com',
                            'tracker/client/message')

    if not fitbit.disconnect():
        logger.error("Dirty state, not able to start synchronisation.")
        fitbit.exhaust()
        return

    if not fitbit.getDongleInfo():
        logger.warning('Failed to get connected Fitbit dongle information')

    logger.info('Discovering trackers to synchronize')

    trackers = [t for t in fitbit.discover(FitBitUUID)]

    logger.info('%d trackers discovered', len(trackers))
    for tracker in trackers:
        logger.debug('Discovered tracker with ID %s',
                     a2x(tracker.id, delim=""))

    for tracker in trackers:

        trackerid = a2x(tracker.id, delim="")

        # Skip this tracker based on include/exclude lists.
        if config.shouldSkip(tracker):
            logger.info('Tracker %s skipped due to configuration', trackerid)
            yield tracker
            continue

        logger.info('Attempting to synchronize tracker %s', trackerid)

        if config.doUpload:
            logger.debug('Connecting to Fitbit server and requesting status')
            if not galileo.requestStatus(not config.httpsOnly):
                yield tracker
                break

        logger.debug('Establishing link with tracker')
        if not (fitbit.establishLink(tracker) and fitbit.toggleTxPipe(True)
                and fitbit.initializeAirlink(tracker)):
            logger.warning('Unable to connect with tracker %s. Skipping',
                           trackerid)
            tracker.status = 'Unable to establish a connection.'
            yield tracker
            continue

        #fitbit.displayCode()
        #time.sleep(5)

        logger.info('Getting data from tracker')
        dump = fitbit.getDump()
        if dump is None:
            logger.error("Error downloading the dump from tracker")
            tracker.status = "Failed to download the dump"
            yield tracker
            continue

        if config.keepDumps:
            # Write the dump somewhere for archiving ...
            dirname = os.path.expanduser(os.path.join(config.dumpDir,
                                                      trackerid))
            if not os.path.exists(dirname):
                logger.debug("Creating non-existent directory for dumps %s",
                             dirname)
                os.makedirs(dirname)

            filename = os.path.join(dirname, 'dump-%d.txt' % int(time.time()))
            dump.toFile(filename)
        else:
            logger.debug("Not dumping anything to disk")

        if not config.doUpload:
            logger.info("Not uploading, as asked ...")
        else:
            logger.info('Sending tracker data to Fitbit')
            try:
                response = galileo.sync(fitbit.dongle, trackerid, dump)

                if config.keepDumps:
                    logger.debug("Appending answer from server to %s",
                                 filename)
                    with open(filename, 'at') as dumpfile:
                        dumpfile.write('\n')
                        for i in range(0, len(response), 20):
                            dumpfile.write(a2x(response[i:i + 20]) + '\n')

                # Even though the next steps might fail, fitbit has accepted
                # the data at this point.
                tracker.status = "Dump successfully uploaded"
                logger.info('Successfully sent tracker data to Fitbit')

                logger.info('Passing Fitbit response to tracker')
                if not fitbit.uploadResponse(response):
                    logger.warning("Error while trying to give Fitbit response"
                                   " to tracker %s", trackerid)
                    tracker.status = "Failed to upload fitbit response to tracker"
                else:
                    tracker.status = "Synchronisation successful"

            except SyncError as e:
                logger.error("Fitbit server refused data from tracker %s,"
                             " reason: %s", trackerid, e.errorstring)
                tracker.status = "Synchronisation failed: %s" % e.errorstring

        logger.debug('Disconnecting from tracker')
        if not (fitbit.terminateAirlink() and fitbit.toggleTxPipe(False) and fitbit.ceaseLink()):
            logger.warning('Error while disconnecting from tracker %s',
                           trackerid)
            tracker.status += " (Error disconnecting)"
        yield tracker

PERMISSION_DENIED_HELP = """
To be able to run the fitbit utility as a non-privileged user, you first
should install a 'udev rule' that lower the permissions needed to access the
fitbit dongle. In order to do so, as root, create the file
/etc/udev/rules.d/99-fitbit.rules with the following content (in one line):

SUBSYSTEM=="usb", ATTR{idVendor}=="%(VID)x", ATTR{idProduct}=="%(PID)x", SYMLINK+="fitbit", MODE="0666"

The dongle must then be removed and reinserted to receive the new permissions.""" % {
    'VID': dgl.FitBitDongle.VID, 'PID': dgl.FitBitDongle.PID}


def version(verbose, delim='\n'):
    s = ['%s: %s' % (sys.argv[0], __version__)]
    if verbose:
        import usb
        import platform
        from .config import yaml
        # To get it on one line
        s.append('Python: %s' % ' '.join(sys.version.split()))
        s.append('Platform: %s' % ' '.join(platform.uname()))
        if not hasattr(usb, '__version__'):
            s.append('pyusb: < 1.0.0b1')
        else:
            s.append('pyusb: %s' % usb.__version__)
        s.append('requests: %s' % requests.__version__)
        if hasattr(yaml, '__with_libyaml__'):
            # Genuine PyYAML
            s.append('yaml: %s (%s libyaml)' % (
                yaml.__version__,
                yaml.__with_libyaml__ and 'with' or 'without'))
        else:
            # Custom version
            s.append('yaml: own version')
    return delim.join(s)


def version_mode(config):
    print(version(config.logLevel in (logging.INFO, logging.DEBUG)))


def sync(config):
    statuses = []
    try:
        for tracker in syncAllTrackers(config):
            statuses.append("Tracker: %s: %s" % (a2x(tracker.id, ''),
                                                 tracker.status))
    except BackOffException as boe:
        print("The server requested that we come back between %d and %d"\
            " minutes." % (boe.min / 60*1000, boe.max / 60*1000))
        later = datetime.datetime.now() + datetime.timedelta(
            microseconds=boe.getAValue()*1000)
        print("I suggest waiting until %s" % later)
        return
    except dgl.PermissionDeniedException:
        print(PERMISSION_DENIED_HELP)
        return
    print('\n'.join(statuses))


def daemon(config):
    goOn = True
    while goOn:
        try:
            # TODO: Extract the initialization part, and do it once for all
            try:
                for tracker in syncAllTrackers(config):
                    logger.info("Tracker %s: %s" % (a2x(tracker.id, ''),
                                                    tracker.status))
            except BackOffException as boe:
                logger.warning("Received a back-off notice from the server,"
                               " waiting for a bit longer.")
                time.sleep(boe.getAValue() / 1000.)
            else:
                logger.info("Sleeping for %d seconds before next sync",
                            config.daemonPeriod / 1000)
                time.sleep(config.daemonPeriod / 1000.)
        except KeyboardInterrupt:
            logger.info("Ctrl-C, caught, stopping ...")
            goOn = False


def main():
    """ This is the entry point """

    # Set the null handler to avoid complaining about no handler presents
    import galileo
    logging.getLogger(galileo.__name__).addHandler(logging.NullHandler())

    try:
        config = Config()

        config.parseSystemConfig()
        config.parseUserConfig()

        # This gives us the config file name
        config.parseArgs()

        if config.rcConfigName:
            config.load(config.rcConfigName)
            # We need to re-apply our arguments as last
            config.applyArgs()
    except ConfigError as e:
        print(e, file=sys.stderr)
        sys.exit(os.EX_CONFIG)

    # --- All logging actions before this line are not active ---
    # This means that the whole Config parsing is not logged because we don't
    # know which logLevel we should use.
    if config.syslog:
        # Syslog messages must have the time/name first.
        format = ('%(asctime)s ' + galileo.__name__ + ': '
                  '%(levelname)s: %(module)s: %(message)s')
        # TODO: Make address into a config option.
        handler = logging.handlers.SysLogHandler(
            address='/dev/log',
            facility=logging.handlers.SysLogHandler.LOG_DAEMON)
        handler.setFormatter(logging.Formatter(fmt=format))
        core_logger = logging.getLogger(galileo.__name__)
        core_logger.handlers = []
        core_logger.addHandler(handler)
        core_logger.setLevel(config.logLevel)
    else:
        format = '%(asctime)s:%(levelname)s: %(message)s'
        logging.basicConfig(format=format, level=config.logLevel)
    # --- All logger actions from now on will be effective ---

    logger.debug("Configuration: %s", config)

    ui = InteractiveUI(config.hardcoded_ui)

    try:
        {
            'version': version_mode,
            'sync': sync,
            'daemon': daemon,
            'pair': Conversation('pair', ui),
            'firmware': Conversation('firmware', ui),
            'interactive': interactive.main,
        }[config.mode](config)
    except:
        logger.critical("# A serious error happened, which is probably due to a")
        logger.critical("# programming error. Please open a new issue with the following")
        logger.critical("# information on the galileo bug tracker:")
        logger.critical("#    https://bitbucket.org/benallard/galileo/issues/new")
        logger.critical('# %s', version(True, '\n# '))
        if hasattr(dgl, 'log'):
            logger.critical('# Last communications:')
            for comm in dgl.log.getData():
                dir, dat = comm
                logger.critical('# %s %s' % ({dgl.IN: '<', dgl.OUT: '>'}.get(dir, '-'), a2x(dat or [])))
        logger.critical("#", exc_info=True)
        sys.exit(os.EX_SOFTWARE)
