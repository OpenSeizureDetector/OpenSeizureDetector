import unittest

from galileo.utils import a2x, a2s, a2lsbi, a2msbi, i2lsba, s2a, x2a

class testa2x(unittest.TestCase):

    def testSimple(self):
        self.assertEqual(a2x(range(10)), '00 01 02 03 04 05 06 07 08 09')

    def testNotShorten(self):
        self.assertEqual(a2x([0] * 5), '00 00 00 00 00')

    def testDelim(self):
        self.assertEqual(a2x(range(190, 196), '|'), 'BE|BF|C0|C1|C2|C3')


class testx2a(unittest.TestCase):

    def testSimple(self):
        self.assertEqual(x2a('2'), [2])
        self.assertEqual(x2a('02'), [2])
        self.assertEqual(x2a('2 3'), [2, 3])

class testa2s(unittest.TestCase):

    def testSimple(self):
        self.assertEqual(a2s(range(ord('a'), ord('d') + 1)), 'abcd')

    def testWithNUL(self):
        self.assertEqual(
            a2s(list(range(ord('a'), ord('d')+1)) + [0]*3 + list(range(ord('e'), ord('i')+1))),
            'abcd')

    def testWithNULNotPrint(self):
        self.assertEqual(
            a2s(list(range(ord('a'), ord('d')+1)) + [0]*3 + list(range(ord('e'), ord('i')+1)), False),
            'abcd\0\0\0efghi')


class testa2lsbi(unittest.TestCase):

    def test0(self):
        self.assertEqual(a2lsbi([0]), 0)
        self.assertEqual(a2lsbi([0]*3), 0)
        self.assertEqual(a2lsbi([0]*10), 0)

    def test1byte(self):
        self.assertEqual(a2lsbi([8]), 8)
        self.assertEqual(a2lsbi([0xff]), 0xff)
        self.assertEqual(a2lsbi([0x80]), 0x80)

    def test2bytes(self):
        self.assertEqual(a2lsbi([1, 0]), 1)
        self.assertEqual(a2lsbi([0xff, 0]), 0xff)
        self.assertEqual(a2lsbi([0x80, 0]), 0x80)
        self.assertEqual(a2lsbi([0, 1]), 0x100)
        self.assertEqual(a2lsbi([0, 0xff]), 0xff00)
        self.assertEqual(a2lsbi([0, 0x80]), 0x8000)


class testa2msbi(unittest.TestCase):

    def test0(self):
        self.assertEqual(a2msbi([0]), 0)
        self.assertEqual(a2msbi([0]*3), 0)
        self.assertEqual(a2msbi([0]*10), 0)

    def test1byte(self):
        self.assertEqual(a2msbi([8]), 8)
        self.assertEqual(a2msbi([0xff]), 0xff)
        self.assertEqual(a2msbi([0x80]), 0x80)

    def test2bytes(self):
        self.assertEqual(a2msbi([1, 0]), 0x100)
        self.assertEqual(a2msbi([0xff, 0]), 0xff00)
        self.assertEqual(a2msbi([0x80, 0]), 0x8000)
        self.assertEqual(a2msbi([0, 1]), 0x1)
        self.assertEqual(a2msbi([0, 0xff]), 0xff)
        self.assertEqual(a2msbi([0, 0x80]), 0x80)


class testi2lsba(unittest.TestCase):

    def test0(self):
        self.assertEqual(i2lsba(0, 1), [0])
        self.assertEqual(i2lsba(0, 3), [0]*3)
        self.assertEqual(i2lsba(0, 5), [0]*5)

    def test1byte(self):
        self.assertEqual(i2lsba(1, 1), [1])
        self.assertEqual(i2lsba(0xff, 1), [0xff])
        self.assertEqual(i2lsba(0x80, 1), [0x80])

    def test2bytes(self):
        self.assertEqual(i2lsba(1, 2), [1, 0])
        self.assertEqual(i2lsba(0xff, 2), [0xff, 0])
        self.assertEqual(i2lsba(0x80, 2), [0x80, 0])
        self.assertEqual(i2lsba(0x100, 2), [0, 1])
        self.assertEqual(i2lsba(0xff00, 2), [0, 0xff])
        self.assertEqual(i2lsba(0x8000, 2), [0, 0x80])


class tests2a(unittest.TestCase):

    def testSimple(self):
        self.assertEqual(s2a('abcd'), list(range(ord('a'), ord('d')+1)))

    def testWithNUL(self):
        self.assertEqual(s2a('abcd\0\0\0efghi'),
                         list(range(ord('a'), ord('d')+1)) +
                        [0] * 3 + list(range(ord('e'), ord('i') + 1)))
