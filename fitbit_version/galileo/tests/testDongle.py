import errno
import unittest

import galileo.dongle
from galileo.dongle import isStatus, FitBitDongle, CM, DM, isATimeout

USBError = galileo.dongle.usb.core.USBError

class MyCM(object):
    def __init__(self, ins, payload):
        self.INS = ins
        self.payload = payload

class testisStatus(unittest.TestCase):

    def testNotAStatus(self):
        self.assertFalse(isStatus(MyCM(3, [])))

    def testIsaStatus(self):
        self.assertTrue(isStatus(MyCM(1, [])))

    def testEquality(self):
        self.assertTrue(isStatus(MyCM(1, [0x61, 0x62, 0x63, 0x64 , 0]), 'abcd'))

    def testStartsWith(self):
        self.assertTrue(isStatus(MyCM(1, [0x61, 0x62, 0x63, 0x64 , 0]), 'ab'))


class testCM(unittest.TestCase):
    r2 = list(range(2))
    r5 = list(range(5))

    def testEquals(self):
        self.assertTrue(CM(8) == CM(8))
        self.assertTrue(CM(5) == CM(5, []))
        self.assertTrue(CM(2, self.r5), CM(2, self.r5))
        self.assertEqual(CM(8), CM(8))
        self.assertEqual(CM(5), CM(5, []))
        self.assertEqual(CM(2, self.r5), CM(2, self.r5))

    def testNotEquals(self):
        self.assertFalse(CM(7) == CM(8))
        self.assertFalse(CM(9) == CM(9, [5]))
        self.assertFalse(CM(3, self.r2) == CM(3, self.r5))
        self.assertFalse(None == CM(3, self.r5))


class testDM(unittest.TestCase):

    def testEquals(self):
        self.assertTrue(DM(range(3)) == DM(range(3)))
        self.assertEqual(DM(range(8)), DM(range(8)))

    def testNotEquals(self):
        self.assertFalse(DM([87]) == DM([42]))
        self.assertFalse(DM(range(2)) == DM(range(5)))
        self.assertFalse(None == DM(range(5)))


class MyDev(object):
    """ Minimal object to reproduce issue#75 """
    def is_kernel_driver_active(self, a): raise NotImplementedError()
    def get_active_configuration(self): return {(0,0): None, (1,0): None}
    def set_configuration(self): pass

class testDongle(unittest.TestCase):

    def testNIE(self):
        def myFind(*args, **kwargs):
            return MyDev()
        galileo.dongle.usb.core.find = myFind
        d = FitBitDongle(0)
        d.setup()

class testisATimeout(unittest.TestCase):

    def testErrnoTIMEOUT(self):
        """ usb.core.USBError: [Errno 110] Operation timed out """
        self.assertTrue(isATimeout(USBError('Operation timed out', errno=errno.ETIMEDOUT)))

    def testpyusb1a2(self):
        """\
        issue#17
        usb.core.USBError: Operation timed out """
        self.assertTrue(isATimeout(IOError('Operation timed out')))

    def testlibusb0(self):
        """\
        issue#82
        usb.core.USBError: [Errno None] Connection timed out """
        self.assertTrue(isATimeout(USBError('Connection timed out')))
