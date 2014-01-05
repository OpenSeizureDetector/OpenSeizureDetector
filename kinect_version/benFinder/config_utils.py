#!/usr/bin/python
import ConfigParser
class ConfigUtil:
    def __init__(self,configFname,section):
        print "ConfigUtil.__init__()"
        self.config = self.getConfigSectionMap(configFname,section)

    def getConfigBool(self,configName):
        if (configName in self.config):
            try:
                retVal = bool(self.config[configName])
            except ValueError:
                print "configName is not a boolean"
                retVal = False
        else:
            print "key %s not found" % configName
            retVal = False
        return retVal

    def getConfigInt(self,configName):
        if (configName in self.config):
            try:
                retVal = int(self.config[configName])
            except ValueError:
                print "configName is not an integer!!!"
                retVal = -999
        else:
            print "key %s not found" % configName
            retVal = -999
        return retVal

    def getConfigFloat(self,configName):
        if (configName in self.config):
            try:
                retVal = float(self.config[configName])
            except ValueError:
                print "configName is not a float!!!"
                retVal = -999
        else:
            print "key %s not found" % configName
            retVal = -999
        return retVal

    def getConfigStr(self,configName):
        if (configName in self.config):
            retVal = str(self.config[configName])
        else:
            print "key %s not found" % configName
            retVal = "NULL"
        return retVal

    def getConfigSectionMap(self,configFname, section):
        '''Returns a dictionary containing the config file data in the section
        specified by the parameter 'section'.   
        configFname should be a string that is the path to a configuration file.'''
        dict1 = {}
        config = ConfigParser.ConfigParser()
        config.read(configFname)
        options = config.options(section)
        for option in options:
            try:
                dict1[option] = config.get(section, option)
                if dict1[option] == -1:
                    DebugPrint("skip: %s" % option)
            except:
                print("exception on %s!" % option)
                dict1[option] = None
        return dict1

