import unittest

from galileo.dump import Dump

class testDump(unittest.TestCase):

    def testEmptyNonValid(self):
        d = Dump(6)
        self.assertFalse(d.isValid())

    def testAddIncreasesLen(self):
        d = Dump(5)
        self.assertEqual(d.len, 0)
        d.add(range(10))
        self.assertEqual(d.len, 10)

    def testFooterIsSet(self):
        d = Dump(0)
        self.assertEqual(d.footer, [])
        d.add([0xc0] + list(range(5)))
        self.assertEqual(d.len, 0)
        self.assertEqual(d.footer, [0xc0] + list(range(5)))

    def testOnlyFooterInvalid(self):
        """ A dump with only a footer is an invalid dump """
        d = Dump(0)
        d.add([0xc0] + list(range(5)))
        self.assertFalse(d.isValid())

    def testEsc1(self):
        d = Dump(0)
        self.assertEqual(d.esc[0], 0)
        d.add([0xdb, 0xdc])
        self.assertEqual(d.len, 1)
        self.assertEqual(d.esc[0], 1)
        self.assertEqual(d.data, [0xc0])

    def testEsc2(self):
        d = Dump(0)
        self.assertEqual(d.esc[1], 0)
        d.add([0xdb, 0xdd])
        self.assertEqual(d.len, 1)
        self.assertEqual(d.esc[1], 1)
        self.assertEqual(d.data, [0xdb])

    def testToBase64(self):
        d = Dump(0)
        d.add(range(10))
        d.add([0xc0] + list(range(8)))
        self.assertEqual(d.toBase64(), 'AAECAwQFBgcICcAAAQIDBAUGBw==')

    def testNonValidDataType(self):
        d = Dump(0)
        d.add(range(10))
        d.add([0xc0]+[0, 3])
        self.assertFalse(d.isValid())

    def testNonValidCRC(self):
        d = Dump(0)
        d.add(range(10))
        d.add([0xc0]+[0, 0, 0, 0])
        self.assertFalse(d.isValid())

    def testNonValidLen(self):
        d = Dump(0)
        d.add(range(10))
        d.add([0xc0]+[0, 0, 0x78, 0x23, 0, 0])
        self.assertFalse(d.isValid())

    def testValid(self):
        d = Dump(0)
        d.add(range(10))
        d.add([0xc0]+[0, 0, 0x78, 0x23, 10, 0])
        self.assertTrue(d.isValid())
