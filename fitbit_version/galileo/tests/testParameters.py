import unittest
import logging

from galileo.config import (
    StrParameter, IntParameter, BoolParameter, SetParameter, LogLevelParameter
)


class MyArgParse(object):
    def __init__(self, tester):
        self.tester = tester
    def add_argument(self, *args, **kwargs):
        self.name = kwargs['dest']
    def parse_args(self, args):
        class Args(object): pass
        a = Args()
        setattr(a, self.name, args[1])
        return a


class testStrParameter(unittest.TestCase):
    def testArgParse(self):
        p = StrParameter('varName', 'name', ('--paramNames'), 'default', False,
                         "Some help text")
        ap = MyArgParse(self)
        p.toArgParse(ap)
        args = ap.parse_args(['--paramNames', 'value'])
        d = {}
        p.fromArgs(args, d)
        self.assertTrue('varName' in d)
        self.assertEqual(d['varName'], 'value')

    def testFile(self):
        p = StrParameter('varName', 'name', ('--paramNames'), 'default', False,
                         "Some help text")
        ap = MyArgParse(self)
        d = {}
        c = {'name': 'abcd'}
        self.assertTrue(p.fromFile(c, d))
        self.assertTrue('varName' in d)
        self.assertEqual(d['varName'], 'abcd')

    def testFileparamOnly(self):
        p = StrParameter('varName', 'name', ('--paramNames'), 'default', True,
                         "Some help text")
        ap = MyArgParse(self)
        d = {}
        c = {'name': 'abcd'}
        self.assertTrue(p.fromFile(c, d))
        self.assertFalse('varName' in d)


class testBoolParameter(unittest.TestCase): pass


class testLogLevelParameter(unittest.TestCase):
    def testWrongValuefromFile(self):
        p = LogLevelParameter()
        d = {}
        c = {'logging': 'foo'}
        self.assertFalse(p.fromFile(c, d))

    def testCorrectValueUpper(self):
        p = LogLevelParameter()
        d = {}
        c = {'logging': 'DEBUG'}
        self.assertTrue(p.fromFile(c, d))
        self.assertEqual(d['logLevel'], logging.DEBUG)

    def testCorrectValueLower(self):
        p = LogLevelParameter()
        d = {}
        c = {'logging': 'quiet'}
        self.assertTrue(p.fromFile(c, d))
        self.assertEqual(d['logLevel'], logging.WARNING)
