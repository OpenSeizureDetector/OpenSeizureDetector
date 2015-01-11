import unittest

from galileo.config import Config

class MyTracker(object):
    def __init__(self, id, syncedRecently):
        self.id = id
        self.syncedRecently = syncedRecently

class MyParam(object):
    def __init__(self, name, value):
        self.varName = name
        self.default = value
P=MyParam

class testShouldSkip(unittest.TestCase):

    def testRecentForce(self):
        t = MyTracker([42], True)
        c = Config([P('forceSync', True),
                    P('includeTrackers', None),
                    P('excludeTrackers', set())])
        self.assertFalse(c.shouldSkip(t))

    def testRecentNotForce(self):
        t = MyTracker([42], True)
        c = Config([P('forceSync', False),
                    P('includeTrackers', None),
                    P('excludeTrackers', set())])
        self.assertTrue(c.shouldSkip(t))

    def testIncludeNotExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', set(['42'])),
                    P('excludeTrackers', set())])
        self.assertFalse(c.shouldSkip(t))
    def testIncludeNoneExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', None),
                    P('excludeTrackers', set(['42']))])
        self.assertTrue(c.shouldSkip(t))
    def testNotIncludeExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', set(['21'])),
                    P('excludeTrackers', set(['42']))])
        self.assertTrue(c.shouldSkip(t))
    def testIncludeExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', set(['42'])),
                    P('excludeTrackers', set(['42']))])
        self.assertTrue(c.shouldSkip(t))
    def testIncludeNoneNotExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', None),
                    P('excludeTrackers', set())])
        self.assertFalse(c.shouldSkip(t))
    def testNotIncludeNotExclude(self):
        t = MyTracker([0x42], False)
        c = Config([P('forceSync', False),
                    P('includeTrackers', set(['21'])),
                    P('excludeTrackers', set())])
        self.assertTrue(c.shouldSkip(t))
