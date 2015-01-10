"""\
The conversationnal part between the server and the client ...
"""

import base64
import time
import uuid

import logging
logger = logging.getLogger(__name__)

from .dongle import FitBitDongle
from .net import GalileoClient
from .tracker import FitbitClient, MICRODUMP, MEGADUMP
from .ui import MissingConfigError
from .utils import a2x, s2a


FitBitUUID = uuid.UUID('{ADAB0000-6E7D-4601-BDA2-BFFAA68956BA}')


class Conversation(object):
    def __init__(self, mode, ui):
        self.mode = mode
        self.ui = ui

    def __call__(self, config):
        self.dongle = FitBitDongle(config.logSize)
        if not self.dongle.setup():
            logger.error("No dongle connected, aborting")
            return

        self.fitbit = FitbitClient(self.dongle)

        self.galileo = GalileoClient('https', 'client.fitbit.com',
                                'tracker/client/message')

        if self.mode == 'firmware':
            # Fake the version to let him believe we can handle that ...
            self.galileo._version = '1.0.0.2575'

        self.fitbit.disconnect()

        self.trackers = {}  # Dict indexed by trackerId
        self.connected = None

        if not self.fitbit.getDongleInfo():
            logger.warning('Failed to get connected Fitbit dongle information')

        action = ''
        uiresp = []
        resp = [('ui-response', {'action': action}, uiresp)]

        while True:
            answ = self.galileo.post(self.mode, self.dongle, resp)
            html = ''
            commands = None
            trackers = []
            action = None
            containsForm = False
            for tple in answ:
                tag, attribs, childs, _ = tple
                if tag == "ui-request":
                    action = attribs['action']
                    for child in childs:
                        tag, attribs, _, body = child
                        if tag == "client-display":
                            containsForm = attribs.get('containsForm', 'false') == 'true'
                            html = body
                elif tag == 'tracker':
                    trackers.append(tple)
                elif tag == 'commands':
                    commands = childs
            if ((not containsForm) and (len(trackers) == 0) and
                (commands is None)):
                break
            resp = []
            if trackers:
                # First: Do what is asked
                for tracker in trackers:
                    self.do_tracker(tracker)
            if commands:
                # Prepare an answer for the server
                res = []
                for command in commands:
                    r = self.do_command(command)
                    print(r)
                    if r is not None:
                        res.append(r)
                if res:
                    resp.extend(res)
            if containsForm:
                # Get an answer from the ui
                try:
                    ui_resp = self.ui.request(action, html)
                except MissingConfigError as mce:
                    print(mce)
                    break
                resp.append(('ui-response', {'action': action}, ui_resp))

        print('Done')

    def __connect(self, id):
        tracker = self.trackers[id]
        self.fitbit.establishLink(tracker)
        self.fitbit.toggleTxPipe(True)
        self.fitbit.initializeAirlink(tracker)
        self.connected = tracker


    #-------- The commands

    def do_command(self, cmd):
        tag, elems, childs, body = cmd
        f = {'pair-to-tracker': self._pair,
            'connect-to-tracker': self._connect,
            'list-trackers': self._list,
            'ack-tracker-data': self._ack}[tag]
        return f(*childs, **elems)

    def _pair(self, **params):
        """ Establish a connection with the tracker.
            :returns: the minidump
        """
        displayCode = bool(params['displayCode'])
        waitForUserInput = bool(params['waitForUserInput'])
        trackerId = params['tracker-id']
        self.__connect(trackerId)
        if displayCode:
            self.fitbit.displayCode()
            if waitForUserInput:
                # XXX: That's waiting, but not for user input ...
                time.sleep(10)
        dump = self.fitbit.getDump(MICRODUMP)
        return ('tracker', {'tracker-id':trackerId},
                 [('data', {}, [], dump.toBase64())])

    def _connect(self, **params):
        """ :returns: nothing
        """
        trackerId = params['tracker-id']
        if self.connected is None:
            self.__connect(trackerId)

        if a2x(self.connected.id, delim="") != trackerId:
            raise ValueError(trackerId)
        if 'connection' in params:
            disconnect = params['connection'] == 'disconnect'
            if disconnect:
                self.fitbit.terminateAirlink()
                self.fitbit.toggleTxPipe(False)
                self.fitbit.ceaseLink()
                self.connected = None
            return
        elif 'response-data' in params:
            responseData = params['response-data']
            dumptype = {'megadump': MEGADUMP,
                        'microdump': MICRODUMP}[responseData]
            dump = self.fitbit.getDump(dumptype)
            return ('tracker', {'tracker-id': trackerId},
                     [('data', {}, [], dump.toBase64())])
        else:
            raise ValueError(params)


    def _list(self, *childs, **params):
        immediateRsi = int(params['immediateRsi'])
        minDuration = int(params['minDuration'])
        maxDuration = int(params['maxDuration'])

        self.trackers = {}
        res = []
        for tracker in self.fitbit.discover(FitBitUUID, minRSSI=immediateRsi,
                                             minDuration=minDuration):
            trackerId = a2x(tracker.id, delim="")
            self.trackers[trackerId] = tracker
            res.append(('available-tracker', {},
                        [('tracker-id', {}, [], trackerId),
                         ('tracker-attributes', {}, [], a2x(tracker.attributes, delim="")),
                         ('rsi', {}, [], str(tracker.RSSI))]))
        return ('command-response', {}, [('list-trackers', {}, res)])

    def _ack(self, **params):
        trackerId = params['tracker-id']
        # Not much to do here as our ack is part of our upload ...
        return ('command-response', {}, [('ack-tracker-data', {'tracker-id':trackerId})])

    # ------

    def do_tracker(self, tracker):
        tag, elems, childs, body = tracker
        trackerId = elems['tracker-id']
        if a2x(self.connected.id, delim="") != trackerId:
            raise ValueError(trackerId)
        _type = elems['type']
        if _type != 'megadumpresponse':
            raise NotImplementedError(_type)
        data = None
        for child in childs:
            tag, _, _, body = child
            if tag == 'data':
                data = s2a(base64.b64decode(body))
        self.fitbit.uploadResponse(data)
