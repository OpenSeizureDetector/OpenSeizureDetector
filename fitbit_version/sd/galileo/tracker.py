from ctypes import c_byte

import logging
logger = logging.getLogger(__name__)

from .dongle import CM, DM, isStatus
from .dump import Dump
from .utils import a2s, a2x, i2lsba, a2lsbi

MICRODUMP = 3
MEGADUMP = 13


class Tracker(object):
    def __init__(self, Id, addrType, attributes, RSSI, serviceUUID=None):
        self.id = Id
        self.addrType = addrType
        if serviceUUID is None:
            self.serviceUUID = [Id[1] ^ Id[3] ^ Id[5], Id[0] ^ Id[2] ^ Id[4]]
        else:
            self.serviceUUID = serviceUUID
        self.attributes = attributes
        self.RSSI = RSSI
        self.status = 'unknown'  # If we happen to read it before anyone set it

    @property
    def syncedRecently(self):
        return self.attributes[1] != 4


class FitbitClient(object):
    def __init__(self, dongle):
        self.dongle = dongle

    def disconnect(self):
        logger.info('Disconnecting from any connected trackers')

        self.dongle.ctrl_write(CM(2))
        if not isStatus(self.dongle.ctrl_read(), 'CancelDiscovery'):
            return False
        # Next one is not critical. It can happen that it does not comes
        isStatus(self.dongle.ctrl_read(), 'TerminateLink')

        self.exhaust()

        return True

    def exhaust(self):
        """ We exhaust the pipe, then we know that we have a clean state """
        logger.debug("Exhausting the communication pipe")
        goOn = True
        while goOn:
            goOn = self.dongle.ctrl_read() is not None

    def getDongleInfo(self):
        self.dongle.ctrl_write(CM(1))
        d = self.dongle.ctrl_read()
        if (d is None) or (d.INS != 8):
            return False
        self.dongle.setVersion(d.payload[0], d.payload[1])
        self.dongle.address = d.payload[2:8]
        return True

    def discover(self, uuid, service1=0xfb00, write=0xfb01, read=0xfb02,
                 minRSSI=-255, minDuration=4000):
        """\
        The uuid is a mask on the service (characteristics ?) we understand
        service1 parameter is unused (at lease for the 'One')
        read and write are the uuid of the characteristics we use for
        transmission and reception.
        """
        logger.debug('Discovering for UUID %s: %s', uuid,
                     ', '.join(hex(s) for s in (service1, write, read)))
        data = i2lsba(uuid.int, 16)
        for i in (service1, write, read, minDuration):
            data += i2lsba(i, 2)
        self.dongle.ctrl_write(CM(4, data))
        amount = 0
        while True:
            d = self.dongle.ctrl_read(minDuration)
            if d is None: break
            elif isStatus(d, 'StartDiscovery', False):
                # We know this can happen almost any time during 'discovery'
                continue
            elif d.INS == 2:
                # Last instruction of a discovery sequence has INS==1
                break
            elif (d.INS != 3) or (len(d.payload) < 17):
                logger.error('payload unexpected: %s', d)
                break
            trackerId = d.payload[:6]
            addrType = d.payload[6]
            RSSI = c_byte(d.payload[7]).value
            attributes = d.payload[9:11]
            sUUID = d.payload[15:17]
            serviceUUID = [trackerId[1] ^ trackerId[3] ^ trackerId[5],
                           trackerId[0] ^ trackerId[2] ^ trackerId[4]]
            tracker = Tracker(trackerId, addrType, attributes, RSSI, sUUID)
            if not tracker.syncedRecently and (serviceUUID != sUUID):
                logger.debug("Cannot acknowledge the serviceUUID: %s vs %s",
                             a2x(serviceUUID, ':'), a2x(sUUID, ':'))
            logger.debug('Tracker: %s, %s, %s, %s', a2x(trackerId, ':'),
                         addrType, RSSI, a2x(attributes, ':'))
            if RSSI < -80:
                logger.info("Tracker %s has low signal power (%ddBm), higher"
                            " chance of miscommunication",
                            a2x(trackerId, delim=""), RSSI)

            if not tracker.syncedRecently:
                logger.debug('Tracker %s was not recently synchronized',
                             a2x(trackerId, delim=""))
            amount += 1
            if RSSI < minRSSI:
                logger.warning("Tracker %s below power threshold (%ddBm),"
                               "dropping", a2x(trackerId, delim=""), minRSSI)
                #continue
            yield tracker

        if d != CM(2, [amount]):
            logger.error('%d trackers discovered, dongle says %s', amount, d)
        # tracker found, cancel discovery
        self.dongle.ctrl_write(CM(5))
        d = self.dongle.ctrl_read()
        if isStatus(d, 'StartDiscovery', False):
            # We had not received the 'StartDiscovery' yet
            d = self.dongle.ctrl_read()
        isStatus(d, 'CancelDiscovery')

    def establishLink(self, tracker):
        self.dongle.ctrl_write(CM(6, tracker.id + [tracker.addrType] +
                                  tracker.serviceUUID))
        if not isStatus(self.dongle.ctrl_read(), 'EstablishLink'):
            return False
        d = self.dongle.ctrl_read(5000)
        if d != CM(4, [0]):
            logger.error('Unexpected message: %s', d)
            return False
        # established, waiting for service discovery
        # - This one takes long
        if not isStatus(self.dongle.ctrl_read(8000),
                        'GAP_LINK_ESTABLISHED_EVENT'):
            return False
        # This one can also take some time (Charge tracker)
        d = self.dongle.ctrl_read(5000)
        if d != CM(7):
            logger.error('Unexpected 2nd message: %s', d)
            return False
        return True

    def toggleTxPipe(self, on):
        """ `on` is a boolean that dictate the status of the pipe
        :returns: a boolean about the sucessfull execution
        """
        self.dongle.ctrl_write(CM(8, [int(on)]))
        d = self.dongle.data_read(5000)
        return d == DM([0xc0, 0xb])

    def initializeAirlink(self, tracker=None):
        """ :returns: a boolean about the successful execution """
        nums = [0xa, 6, 6, 0, 200]
        #nums = [1, 8, 16, 0, 200]
        data = []
        for n in nums:
            data.extend(i2lsba(n, 2))
        #data = data + [1]
        self.dongle.data_write(DM([0xc0, 0xa] + data))
        d = self.dongle.ctrl_read(10000)
        if d != CM(6, data[-6:]):
            logger.error("Unexpected message: %s != %s", d, CM(6, data[-6:]))
            return False
        d = self.dongle.data_read()
        if d is None:
            return False
        if d.data[:2] != [0xc0, 0x14]:
            logger.error("Wrong header: %s", a2x(d.data[:2]))
            return False
        if (tracker is not None) and (d.data[6:12] != tracker.id):
            logger.error("Connected to wrong tracker: %s", a2x(d.data[6:12]))
            return False
        logger.debug("Connection established: %d, %d",
                     a2lsbi(d.data[2:4]), a2lsbi(d.data[4:6]))
        return True

    def displayCode(self):
        """ :returns: a boolean about the sucessfull execution """
        logger.debug('Displaying code on tracker')
        self.dongle.data_write(DM([0xc0, 6]))
        r = self.dongle.data_read()
        return (r is not None) and (r.data == [0xc0, 2])

    def getDump(self, dumptype=MEGADUMP):
        """ :returns: a `Dump` object or None """
        logger.debug('Getting dump type %d', dumptype)

        # begin dump of appropriate type
        self.dongle.data_write(DM([0xc0, 0x10, dumptype]))
        r = self.dongle.data_read()
        if r != DM([0xc0, 0x41, dumptype]):
            logger.error("Tracker did not acknowledged the dump type: %s", r)
            return None

        dump = Dump(dumptype)
        # Retrieve the dump
        d = self.dongle.data_read()
        if d is None:
            return None
        dump.add(d.data)
        while d.data[0] != 0xc0:
            d = self.dongle.data_read()
            if d is None:
                return None
            dump.add(d.data)
        # Analyse the dump
        if not dump.isValid():
            logger.error('Dump not valid')
            return None
        logger.debug("Dump done, length %d, transportCRC=0x%04x, esc1=0x%02x,"
                     " esc2=0x%02x", dump.len, dump.crc.final(), dump.esc[0],
                     dump.esc[1])
        return dump

    def uploadResponse(self, response):
        """ 4 and 6 are magic values here ...
        :returns: a boolean about the success of the operation.
        """
        dumptype = 4  # ???
        self.dongle.data_write(DM([0xc0, 0x24, dumptype] + i2lsba(len(response), 6)))
        d = self.dongle.data_read()
        if d != DM([0xc0, 0x12, dumptype, 0, 0]):
            logger.error("Tracker did not acknowledgded upload type: %s", d)
            return False

        CHUNK_LEN = 20

        for i in range(0, len(response), CHUNK_LEN):
            self.dongle.data_write(DM(response[i:i + CHUNK_LEN]))
            d = self.dongle.data_read()
            expected = DM([0xc0, 0x13, ((((i // CHUNK_LEN) + 1) % 16) << 4) + dumptype, 0, 0])
            if d != expected:
                logger.error("Wrong sequence number: %s", d)
                return False

        self.dongle.data_write(DM([0xc0, 2]))
        # Next one can be very long. He is probably erasing the memory there
        d = self.dongle.data_read(60000)
        if d != DM([0xc0, 2]):
            logger.error("Unexpected answer from tracker: %s", d)
            return False

        return True

    def terminateAirlink(self):
        """ contrary to ``initializeAirlink`` """

        self.dongle.data_write(DM([0xc0, 1]))
        d = self.dongle.data_read()
        if d != DM([0xc0, 1]):
            return False
        return True

    def ceaseLink(self):
        """ contrary to ``establishLink`` """

        self.dongle.ctrl_write(CM(7))
        if not isStatus(self.dongle.ctrl_read(), 'TerminateLink'):
            return False

        d = self.dongle.ctrl_read()
        if (d is None) or (d.INS != 5):
            # Payload can be either 0x16 or 0x08
            return False
        if not isStatus(self.dongle.ctrl_read(), 'GAP_LINK_TERMINATED_EVENT'):
            return False
        if not isStatus(self.dongle.ctrl_read()):
            # This one doesn't always return '22'
            return False
        return True
