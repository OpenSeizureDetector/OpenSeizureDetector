import unittest

from galileo.dongle import DataRing

class testRing(unittest.TestCase):
    def testEmpty(self):
        r = DataRing(5)
        self.assertEqual([], r.getData())
        self.assertTrue(r.empty)
        self.assertFalse(r.full)

    def testCapaNull(self):
        r = DataRing(0)
        r.add(5)
        self.assertEqual([], r.getData())
        self.assertTrue(r.empty)
        self.assertTrue(r.full)

    def testOneElement(self):
        r = DataRing(10)
        r.add('data')
        self.assertEqual(['data'], r.getData())
        self.assertFalse(r.empty)
        self.assertFalse(r.full)
        self.assertEqual(r.queue + 1, r.head)
        self.assertEqual(1, r.fill)

    def testTwoElement(self):
        r = DataRing(10)
        r.add('data1')
        r.add('data2')
        self.assertFalse(r.empty)
        self.assertEqual(['data1', 'data2'], r.getData())
        self.assertEqual(2, r.fill)

    def testThreeElement(self):
        r = DataRing(10)
        r.add('data1')
        r.add('data2')
        r.add('data3')
        self.assertFalse(r.empty)
        self.assertEqual(['data1', 'data2', 'data3'], r.getData())
        self.assertEqual(3, r.fill)

    def testOverflow(self):
        r = DataRing(2)
        self.assertFalse(r.full)
        self.assertEqual(0, r.fill)
        r.add('data1')
        self.assertFalse(r.full)
        self.assertEqual(1, r.fill)
        r.add('data2')
        self.assertTrue(r.full)
        self.assertEqual(2, r.fill)
        r.add('data3')
        self.assertFalse(r.empty)
        self.assertTrue(r.full)
        self.assertEqual(2, r.fill)
        self.assertEqual(['data2', 'data3'], r.getData())
