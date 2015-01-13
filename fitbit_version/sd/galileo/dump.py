import base64

import logging
logger = logging.getLogger(__name__)

from .utils import a2x, a2lsbi, a2s


class CRC16(object):
    """ A rather generic CRC16 class """
    def __init__(self, poly=0x1021, Invert=True, IV=0x0000, FV=0x0000):
        self.poly = poly
        self.value = IV
        self.FV = FV
        if Invert:
            self.update_byte = self.update_byte_MSB
        else:
            self.update_byte = self.update_byte_LSB

    def update_byte_MSB(self, byte):
        self.value ^= byte << 8
        for i in range(8):
            if self.value & 0x8000:
                self.value = (self.value << 1) ^ self.poly
            else:
                self.value <<= 1
        self.value &= 0xffff

    def update_byte_LSB(self, byte):
        self.value ^= byte
        for i in range(8):
            if self.value & 0x0001:
                self.value = (self.value >> 1) ^ self.poly
            else:
                self.value >>= 1

    def update(self, array):
        for c in array:
            self.update_byte(c)

    def final(self):
        return self.value ^ self.FV


class Dump(object):
    def __init__(self, _type):
        self._type = _type
        self.data = []
        self.footer = []
        self.crc = CRC16()
        self.esc = [0, 0]

    def unSLIP1(self, data):
        """ The protocol uses a particular version of SLIP (RFC 1055) applied
        only on the first byte of the data"""
        END = 0xC0
        ESC = 0xDB
        ESC_ = {0xDC: END,
                0xDD: ESC}
        if data[0] == ESC:
            # increment the escape counter
            self.esc[data[1] - 0xDC] += 1
            # return the escaped value
            return [ESC_[data[1]]] + data[2:]
        return data

    def add(self, data):
        if data[0] == 0xc0:
            assert self.footer == []
            self.footer = data
            return
        data = self.unSLIP1(data)
        self.crc.update(data)
        self.data.extend(data)

    @property
    def len(self):
        return len(self.data)

    def isValid(self):
        if not self.footer:
            return False
        dataType = self.footer[2]
        if dataType != self._type:
            logger.error('Dump is not of requested type: %x != %x',
                         dataType, self._type)
            return False
        crcVal = self.crc.final()
        transportCRC = a2lsbi(self.footer[3:5])
        if transportCRC != crcVal:
            logger.error("Error in communication, Expected CRC: 0x%04X,"
                         " received 0x%04X", crcVal, transportCRC)
            return False
        nbBytes = a2lsbi(self.footer[5:7])
        if self.len != nbBytes:
            logger.error("Error in communication, Expected length: %d bytes,"
                         " received %d bytes", nbBytes, self.len)
            return False
        return True

    def toFile(self, filename):
        logger.debug("Dumping megadump to %s", filename)
        with open(filename, 'wt') as dumpfile:
            for i in range(0, self.len, 20):
                dumpfile.write(a2x(self.data[i:i + 20]) + '\n')
            dumpfile.write(a2x(self.footer) + '\n')

    def toBase64(self):
        return base64.b64encode(a2s(self.data + self.footer, False))
