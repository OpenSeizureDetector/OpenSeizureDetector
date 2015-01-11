from __future__ import print_function

import errno

import logging
logger = logging.getLogger(__name__)

try:
    import usb.core
except ImportError as ie:
    # if ``usb`` is there, but not ``usb.core``, a pre-1.0 version of pyusb
    # is installed.
    try:
        import usb
    except ImportError:
        pass
    else:
        print("You have an older pyusb version installed. This utility needs")
        print("at least version 1.0.0a2 to work properly.")
        print("Please upgrade your system to a newer version.")
    raise ie

from .utils import a2x, a2s

IN, OUT = 1, -1

class DataRing(object):
    """ A 'stupid' data structure that store not more that capacity elements,
    and keeps them in order

    head points to the next spot
    queue points to the last spot
    fill tell us how much is filled
    """
    def __init__(self, capacity):
        self.capacity = capacity
        self.ring = [None] * self.capacity
        self.head = 0
        self.queue = 0
        # We can't distinguish empty from full without the fillage
        self.fill = 0

    @property
    def empty(self):
        return self.fill == 0

    @property
    def full(self):
        return self.fill == self.capacity

    def add(self, data):
        if self.capacity == 0:
            # Special case, do nothing
            return
        if self.full:
            # full, don't forget to increase the queue
            self.queue = (self.queue + 1) % self.capacity
        self.ring[self.head] = data
        self.head = (self.head + 1) % self.capacity
        self.fill = min(self.fill + 1, self.capacity)

    def remove(self):
        """ For the fun, doesnt fit into our use case """
        if self.empty:
            # NOOP
            return
        self.queue = (self.queue - 1)  % self.capacity

    def getData(self):
        if self.empty:
            return []
        elif self.queue < self.head:
            return self.ring[self.queue:self.head]
        else:
            return self.ring[self.queue:] + self.ring[:self.head]


class USBDevice(object):
    def __init__(self, vid, pid):
        self.vid = vid
        self.pid = pid
        self._dev = None

    @property
    def dev(self):
        if self._dev is None:
            self._dev = usb.core.find(idVendor=self.vid, idProduct=self.pid)
        return self._dev

    def __del__(self):
        pass


class CtrlMessage(object):
    """ A message that get communicated over the ctrl link """
    def __init__(self, INS, data=[]):
        if INS is None:  # incoming
            self.len = data[0]
            self.INS = data[1]
            self.payload = data[2:self.len]
        else:  # outgoing
            self.len = len(data) + 2
            self.INS = INS
            self.payload = data

    def asList(self):
        return [self.len, self.INS] + self.payload

    def __eq__(self, other):
        if other is None: return False
        return self.asList() == other.asList()

    def __ne__(self, other):
        return not self == other

    def __str__(self):
        d = []
        if self.payload:
            d = ['(', a2x(self.payload), ')']
        return ' '.join(['%02X' % self.INS] + d + ['-', str(self.len)])

CM = CtrlMessage


class DataMessage(object):
    """ A message that get communicated over the data link """
    LENGTH = 32

    def __init__(self, data, out=True):
        if out:  # outgoing
            if len(data) > (self.LENGTH - 1):
                raise ValueError('data %s (%d) too big' % (data, len(data)))
            self.data = data
            self.len = len(data)
        else:  # incoming
            if len(data) != self.LENGTH:
                raise ValueError('data %s with wrong length' % data)
            # last byte is length
            self.len = data[-1]
            self.data = list(data[:self.len])

    def asList(self):
        return self.data + [0] * (self.LENGTH - 1 - self.len) + [self.len]

    def __eq__(self, other):
        if other is None: return False
        return self.data == other.data

    def __ne__(self, other):
        return not self == other

    def __str__(self):
        return ' '.join(['[', a2x(self.data), ']', '-', str(self.len)])

DM = DataMessage


def isATimeout(excpt):
    if excpt.errno == errno.ETIMEDOUT:
        return True
    elif excpt.errno is None and excpt.args == ('Operation timed out',):
        return True
    elif excpt.errno is None and excpt.strerror == 'Connection timed out':
        return True
    else:
        return False


class DongleWriteException(Exception): pass


class PermissionDeniedException(Exception): pass


def isStatus(data, msg=None, logError=True):
    if data is None:
        return False
    if data.INS != 1:
        if logError:
            logging.warning("Message is not a status message: %x", data.INS)
        return False
    if msg is None:
        return True
    message = a2s(data.payload)
    if not message.startswith(msg):
        if logError:
            logging.warning("Message '%s' (received) is not '%s' (expected)",
                            message, msg)
        return False
    return True


class FitBitDongle(USBDevice):
    VID = 0x2687
    PID = 0xfb01

    def __init__(self, logsize):
        USBDevice.__init__(self, self.VID, self.PID)
        self.hasVersion = False
        self.newerPyUSB = None
        global log
        log = DataRing(logsize)

    def setup(self):
        if self.dev is None:
            return False

        try:
            if self.dev.is_kernel_driver_active(0):
                self.dev.detach_kernel_driver(0)
            if self.dev.is_kernel_driver_active(1):
                self.dev.detach_kernel_driver(1)
        except usb.core.USBError as ue:
            if ue.errno == errno.EACCES:
                logger.error('Insufficient permissions to access the Fitbit'
                             ' dongle')
                raise PermissionDeniedException
            raise
        except NotImplementedError as nie:
            logger.error("Hit some 'Not Implemented Error': '%s', moving on ...", nie)

        cfg = self.dev.get_active_configuration()
        self.DataIF = cfg[(0, 0)]
        self.CtrlIF = cfg[(1, 0)]
        self.dev.set_configuration()
        return True

    def setVersion(self, major, minor):
        self.major = major
        self.minor = minor
        self.hasVersion = True
        logger.debug('Fitbit dongle version major:%d minor:%d', self.major,
                     self.minor)

    def write(self, endpoint, data, timeout):
        if self.newerPyUSB:
            params = (endpoint, data, timeout)
        else:
            interface = {0x02: self.CtrlIF.bInterfaceNumber,
                         0x01: self.DataIF.bInterfaceNumber}[endpoint]
            params = (endpoint, data, interface, timeout)
        log.add((OUT, data))
        try:
            return self.dev.write(*params)
        except TypeError:
            if self.newerPyUSB is not None:
                # Already been there, something else is happening ...
                raise
            logger.debug('Switching to a newer pyusb compatibility mode')
            self.newerPyUSB = True
            return self.write(endpoint, data, timeout)
        except usb.core.USBError as ue:
            if ue.errno != errno.EIO:
                raise
            logger.info('Caught an I/O Error while writing, trying again ...')
            # IO Error, try again ...
            return self.dev.write(*params)

    def read(self, endpoint, length, timeout):
        if self.newerPyUSB:
            params = (endpoint, length, timeout)
        else:
            interface = {0x82: self.CtrlIF.bInterfaceNumber,
                         0x81: self.DataIF.bInterfaceNumber}[endpoint]
            params = (endpoint, length, interface, timeout)
        data = None
        try:
            data = self.dev.read(*params)
        except TypeError:
            if self.newerPyUSB is not None:
                # Already been there, something else is happening ...
                raise
            logger.debug('Switching to a newer pyusb compatibility mode')
            self.newerPyUSB = True
            return self.read(endpoint, length, timeout)
        except usb.core.USBError as ue:
            if not isATimeout(ue):
                raise
        log.add((IN, data))
        return data

    def ctrl_write(self, msg, timeout=2000):
        logger.debug('--> %s', msg)
        l = self.write(0x02, msg.asList(), timeout)
        if l != msg.len:
            logger.error('Bug, sent %d, had %d', l, msg.len)
            raise DongleWriteException

    def ctrl_read(self, timeout=2000, length=32):
        msg = None
        data = self.read(0x82, length, timeout)
        if data is not None:
            # 'None' parameter in next line means incoming
            msg = CM(None, list(data))
        if msg is None:
            logger.debug('<-- ...')
        elif isStatus(msg, logError=False):
            logger.debug('<-- %s', a2s(msg.payload))
        else:
            logger.debug('<-- %s', msg)
        return msg

    def data_write(self, msg, timeout=2000):
        logger.debug('==> %s', msg)
        l = self.write(0x01, msg.asList(), timeout)
        if l != msg.LENGTH:
            logger.error('Bug, sent %d, had %d', l, msg.LENGTH)
            raise DongleWriteException

    def data_read(self, timeout=2000):
        msg = None
        data = self.read(0x81, DM.LENGTH, timeout)
        if data is not None:
            msg = DM(data, out=False)
        logger.debug('<== %s', msg or '...')
        return msg
