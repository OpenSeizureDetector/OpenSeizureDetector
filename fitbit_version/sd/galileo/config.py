import os

import argparse
import logging
logger = logging.getLogger(__name__)

try:
    import yaml
except ImportError:
    from . import parser as yaml

from .utils import a2x

class ConfigError(Exception): pass

class ConfigFileError(ConfigError):
    def __init__(self, filename, paramName, msg=""):
        self.filename = filename
        self.paramName = paramName
        self.msg = msg

    def __str__(self):
        s = "Error parsing parameter '%s' in file '%s'" % (
            self.paramName, self.filename)
        if self.msg:
            s += ": %s" % self.msg
        return s

class Parameter(object):
    def __init__(self, varName, name, paramName, default, paramOnly, helpText):
        # The name of the variable that will be used
        self.varName = varName
        # the internal name
        self.name = name
        # Tuple about the parameter names (short, long)
        self.paramName = paramName
        # the default Value
        self.default = default
        self.helpText = helpText
        self.paramOnly = paramOnly

    def toArgParse(self, parser):
        """ Add the parameter to the 'argparse' parser given in parameter """
        raise NotImplementedError

    def fromArgs(self, args, optdict):
        """ Take the value from the args parameter (from 'argparse'), and fill
        it in the dict """
        val = getattr(args, self.name)
        if val:
            optdict[self.varName] = val

    def fromFile(self, filedict, optdict):
        """ Take the value from the filedict parameter and fill it in the dict
        :returns: False if something went wrong
        """
        if self.paramOnly: return True
        if self.name in filedict:
            optdict[self.varName] = filedict[self.name]
        return True


class StrParameter(Parameter):
    def toArgParse(self, parser):
        parser.add_argument(*self.paramName,
                            dest=self.name,
                            help=self.helpText +
                            " (default to %s)" % self.default)


class IntParameter(Parameter):
    def toArgParse(self, parser):
        parser.add_argument(*self.paramName,
                            dest=self.name, type=int,
                            help=self.helpText +
                            " (default to %s)" % self.default)


class BoolParameter(Parameter):
    def toArgParse(self, parser):
        if self.paramOnly:
            parser.add_argument(*self.paramName,
                                action={True:  "store_false",
                                        False: "store_true"}[self.defaultVal],
                                dest=self.name,
                                help=self.helpText)
        else:
            # We need the True and False version
            assert len(self.paramName) == 1, len(self.paramName)
            self.paramName = self.paramName[0]
            if self.paramName.startswith('--'):
                self.paramName = self.paramName[2:]
            group = parser.add_argument_group(
                description="whether or not to "+self.helpText)
            mut_ex_group = group.add_mutually_exclusive_group()
            _help = {}
            if self.default:
                _help['help'] = "DEFAULT"
            mut_ex_group.add_argument("--%s" % self.paramName,
                                      action="store_true", dest=self.name,
                                      **_help)
            _help = {}
            if not self.default:
                _help['help'] = "DEFAULT"
            mut_ex_group.add_argument("--no-%s" % self.paramName,
                                      action="store_true",
                                      dest="no_%s" % self.name, **_help)

    def fromArgs(self, args, optdict):
        if self.paramOnly:
            optdict[self.varName] = getattr(args, self.name)
        else:
            if getattr(args, "no_"+self.name):
                optdict[self.varName] = False
            elif getattr(args, self.name):
                optdict[self.varName] = True


class SetParameter(Parameter):
    def toArgParse(self, parser):
        parser.add_argument(*self.paramName,
                            nargs="+", metavar="ID", dest=self.name,
                            help=self.helpText)

    def fromArgs(self, args, optdict):
        # Now make sure the list of trackers is all in upper-case to
        # make comparisons easier later.
        values = [x.upper() for x in (getattr(args, self.name) or [])]
        if optdict[self.varName] is None and values:
            optdict[self.varName] = set()
        if values:
            optdict[self.varName].update(values)

    def fromFile(self, filedict, optdict):
        if self.paramOnly: return True
        if self.name in filedict:
            values = [x.upper() for x in filedict[self.name]]
            if optdict[self.varName] is None and values:
                optdict[self.varName] = set()
            optdict[self.varName].update(values)
        return True


class LogLevelParameter(Parameter):
    """ A class extra for setting the LogLevel """
    def __init__(self):
        Parameter.__init__(self, 'logLevel', 'logging', (),  logging.WARNING,
                           False, "logging Verbosity")
        self.__logLevelMap = {'quiet': logging.WARNING,
                              'verbose': logging.INFO,
                              'debug': logging.DEBUG}
        self.__logLevelMapReverse = {}
        for key, value in self.__logLevelMap.items():
            self.__logLevelMapReverse[value] = key
        self.default = logging.WARNING

    def toArgParse(self, parser):
        verbosity_arggroup = parser.add_argument_group(title=self.helpText)
        verbosity_arggroup2 = verbosity_arggroup.add_mutually_exclusive_group()
        verbosity_arggroup2.add_argument("-v", "--verbose",
                                         action="store_true",
                                         help="display synchronization progress")
        verbosity_arggroup2.add_argument("-d", "--debug",
                                         action="store_true",
                                         help="show internal activity (implies verbose)")
        verbosity_arggroup2.add_argument("-q", "--quiet",
                                         action="store_true",
                                         help="only show errors and summary (default)")

    def fromArgs(self, args, optdict):
        value = None
        if args.verbose:
            value = self.__logLevelMap['verbose']
        elif args.debug:
            value = self.__logLevelMap['debug']
        elif args.quiet:
            value = self.__logLevelMap['quiet']
        if value is not None:
            optdict[self.varName] = value

    def fromFile(self, filedict, optdict):
        if self.paramOnly: return
        if self.name in filedict:
            loglevel = filedict[self.name].lower()
            try:
                optdict[self.varName] = self.__logLevelMap[loglevel]
            except KeyError:
                return False
        return True


class Argument(StrParameter):
    """ Extra class for the positional argument """
    def __init__(self):
        StrParameter.__init__(self, 'mode', 'mode', ('mode',), 'sync', True,
                              'The mode to run')

    def toArgParse(self, parser):
        parser.add_argument(*self.paramName,
                            nargs='?', choices=['version', 'sync', 'daemon',
                                                'pair', 'firmware',
                                                'interactive'],
                            help=self.helpText +
                            " (default to %s)" % self.default)


class HardCodedUIConfig(Parameter):
    """\
    A Config parameter for the config of the HardCodedUI class
    """
    def __init__(self):
        self.name = 'hardcoded-ui'
        self.varName = self.name.replace('-', '_')
        self.default = {}
    def toArgParse(self, parser):
        """ no-op """
    def fromArgs(self, args, optdict):
        """ no-op """
    def fromFile(self, filedict, optdict):
        optdict[self.varName] = filedict.get(self.name, {})
        return True


class Config(object):
    """Class holding the configuration to be applied during synchronization.
    The configuration can be loaded from a file in which case the defaults
    can be overridden; loading from multiple files allows the settings from
    later files to override those defined in earlier files. Finally, each
    configuration option can also be set directly, which is used to allow
    overriding of file-based configuration settings with those explicitly
    specified on the command line.
    """

    DEFAULT_RCFILE_NAME = "~/.galileorc"
    DEFAULT_DUMP_DIR = "~/.galileo"

    # NOTE TO SELF: When modifying something here, don't forget to propagate the
    # modifications to the man-pages (under /doc)

    def __init__(self, opts=None):
        """ The opts parameter is used by the testsuite """
        if opts is None:
            opts = [
                StrParameter('rcConfigName', 'rcconfigname', ('-c', '--config'), None, True, "use alternative configuration file"),
                StrParameter('dumpDir', 'dump-dir', ('--dump-dir',), "~/.galileo", False, "directory for storing dumps"),
                IntParameter('daemonPeriod', 'daemon-period', ('--daemon-period',), 15000, False, "sleep time in msec between sync runs when in daemon mode"),
                SetParameter('includeTrackers', 'include', ('-I', '--include'), None, False, "list of tracker IDs to sync (all if not specified)"),
                SetParameter('excludeTrackers', 'exclude', ('-X', '--exclude'), set(), False, "list of tracker IDs to not sync"),
                LogLevelParameter(),
                BoolParameter('forceSync', 'force-sync', ('force',), False, False, "synchronize even if tracker reports a recent sync"),
                BoolParameter('keepDumps', 'keep-dumps', ('dump',), True, False, "enable saving of the megadump to file"),
                BoolParameter('doUpload', 'do-upload',  ('upload',), True, False, "upload the dump to the server"),
                BoolParameter('httpsOnly', 'https-only', ('https-only',), True, False, "use http if https is not available"),
                IntParameter('logSize', 'log-size', ('--log-size',), 10, False, "Amount of communication to display in case of error"),
                BoolParameter('syslog', 'syslog', ('syslog',), False, False, "send output to syslog instead of stderr"),
                Argument(),
                HardCodedUIConfig(),
                ]
        self.__opts = opts
        self.__optdict = {}
        for opt in self.__opts:
            self.__optdict[opt.varName] = opt.default

        logger.debug("Config default values: %s", self)  # not logged

    def __getattr__(self, name):
        """ Allow accessing the attributes as config.XXX """
        if name not in self.__optdict:
            raise AttributeError(name)
        return self.__optdict[name]

    def parseSystemConfig(self):
        """ Load the system-wide configuration file """
        self.load('/etc/galileo/config')

    def parseUserConfig(self):
        """ Load the user based configuration file """
        self.load(os.path.join(
            os.environ.get('XDG_CONFIG_HOME', '~/.config'),
            'galileo', 'config'))
        self.load('~/.galileorc')

    def load(self, filename):
        """Load configuration settings from the named YAML-format
        configuration file. This configuration file can include a
        subset of possible parameters in which case only those
        parameters are changed by the load operation.

        Arguments:
        - `filename`: The name of the file to load parameters from.

        """
        filename = os.path.expanduser(filename)
        if not os.path.exists(filename):
            # Not logged
            logger.warning('Config file %s does not exists' % filename)
            return

        logger.debug('Reading config file %s' % filename)  # not logged

        with open(filename, 'rt') as f:
            config = yaml.load(f)

        for param in self.__opts:
            if not param.fromFile(config, self.__optdict):
                raise ConfigFileError(filename, param.name)

    def parseArgs(self):
        argparser = argparse.ArgumentParser(description="synchronize Fitbit trackers with Fitbit web service",
                                            epilog="""Access your synchronized data at http://www.fitbit.com.""")
        for param in self.__opts:
            param.toArgParse(argparser)

        self.cmdlineargs = argparser.parse_args()

        # And we apply them immediately
        self.applyArgs()

    def applyArgs(self):
        for param in self.__opts:
            param.fromArgs(self.cmdlineargs, self.__optdict)

    def shouldSkip(self, tracker):
        """Method to check, based on the configuration, whether a particular
        tracker should be skipped and not synchronized. The
        includeTrackers and excludeTrackers properties are checked to
        determine this.

        Arguments:
        - `tracker`: Tracker (object), to check.

        """
        trackerid = a2x(tracker.id, delim='')

        # If a list of trackers to sync is configured then was
        # provided then ignore this tracker if it's not in that list.
        if (self.includeTrackers is not None) and (trackerid not in self.includeTrackers):
            logger.info("Include list not empty, and tracker %s not there, skipping.", trackerid)
            tracker.status = "Skipped because not in include list"
            return True

        # If a list of trackers to avoid syncing is configured then
        # ignore this tracker if it is in that list.
        if trackerid in self.excludeTrackers:
            logger.info("Tracker %s in exclude list, skipping.", trackerid)
            tracker.status = "Skipped because in exclude list"
            return True

        if tracker.syncedRecently:
            if not self.forceSync:
                logger.info('Tracker %s was recently synchronized; skipping for now', trackerid)
                tracker.status = "Skipped because recently synchronised"
                return True
            logger.info('Tracker %s was recently synchronized, but forcing synchronization anyway', trackerid)

        return False

    def __str__(self):
        return str(self.__optdict)
