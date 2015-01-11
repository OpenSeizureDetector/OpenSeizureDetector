import unittest

from galileo.dump import CRC16

class testCRC(unittest.TestCase):
    """ CRC unit tests """

    def test_XMODEM_123456789(self):
        # Default values, used by Fitbit
        crc = CRC16(0x1021, True, 0x0000, 0x0000)
        a = [0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39]
        crc.update(a)
        self.assertEqual(crc.final(), 0x31c3)

    def test_XMODEM_two_parts(self):
        crc = CRC16()
        crc.update([0x31, 0x32, 0x33, 0x34, 0x35])
        crc.update([0x36, 0x37, 0x38, 0x39])
        self.assertEqual(crc.final(), 0x31c3)
