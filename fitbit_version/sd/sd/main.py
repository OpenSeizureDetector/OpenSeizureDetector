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

#from .conversation import Conversation
from .config import Config, ConfigError
from .tracker import FitbitClient
from .utils import a2x
from . import dongle as dgl

import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np
import Image

FitBitUUID = uuid.UUID('{ADAB0000-6E7D-4601-BDA2-BFFAA68956BA}')


def syncAllTrackers(config):
    logger.debug('%s initialising', os.path.basename(sys.argv[0]))
    dongle = dgl.FitBitDongle(config.logSize)
    if not dongle.setup():
        logger.error("No dongle connected, aborting")
        return

    fitbit = FitbitClient(dongle)

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

        if (trackerid=="3C971E6292CA"):
            logger.info('Attempting to synchronize tracker %s', trackerid)

            logger.debug('Establishing link with tracker')
            if not (fitbit.establishLink(tracker) and fitbit.toggleTxPipe(True)
                and fitbit.initializeAirlink(tracker)):
                logger.warning('Unable to connect with tracker %s. Skipping',
                           trackerid)
                tracker.status = 'Unable to establish a connection.'
                yield tracker
                continue


            # create output directory if necessary
            dirname = os.path.expanduser(os.path.join(config.dumpDir,
                                                      trackerid))
            if not os.path.exists(dirname):
                logger.debug("Creating non-existent directory for dumps %s",
                             dirname)
                os.makedirs(dirname)

            # Collect several dumps in rapid succession.
            dumparr = []
            for ndump in range(0,300):
                logger.info('Getting data from tracker')
                dump = fitbit.getDump(fitbit.MICRODUMP)
                if dump is None:
                    logger.error("Error downloading the dump from tracker")
                    tracker.status = "Failed to download the dump"
                    yield tracker
                    continue


                filename = os.path.join(dirname, 'dump-%d-%d.txt' % 
                                        (int(time.time()),ndump))
                #dump.toFile(filename)
                dumparr.append(dump.data)
                
            dumparr_np = np.array(dumparr)
            print(dumparr_np.shape)
            img = Image.fromarray(dumparr_np.astype(np.uint8))
            img.save("image.png")
            dumparr_np = dumparr_np/255.
            print(dumparr_np)
            imgplot = plt.imshow(dumparr_np)
            plt.show()



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



def sync(config):
    statuses = []
    try:
        for tracker in syncAllTrackers(config):
            statuses.append("Tracker: %s: %s" % (a2x(tracker.id, ''),
                                                 tracker.status))
    except dgl.PermissionDeniedException:
        print(PERMISSION_DENIED_HELP)
        return
    print('\n'.join(statuses))



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

    #ui = InteractiveUI(config.hardcoded_ui)

    try:
        sync(config)
    except:
        logger.critical("# A serious error happened, which is probably due to a")
        logger.critical("# programming error. Please open a new issue with the following")
        logger.critical("# information on the galileo bug tracker:")
        logger.critical("#    https://bitbucket.org/benallard/galileo/issues/new")
        if hasattr(dgl, 'log'):
            logger.critical('# Last communications:')
            for comm in dgl.log.getData():
                dir, dat = comm
                logger.critical('# %s %s' % ({dgl.IN: '<', dgl.OUT: '>'}.get(dir, '-'), a2x(dat or [])))
        logger.critical("#", exc_info=True)
        sys.exit(os.EX_SOFTWARE)
