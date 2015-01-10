
import base64
import random
import socket

from io import BytesIO


import xml.etree.ElementTree as ET

import logging
logger = logging.getLogger(__name__)

import requests

from . import __version__
from .utils import s2a


class SyncError(Exception):
    def __init__(self, errorstring='Undefined'):
        self.errorstring = errorstring


class BackOffException(Exception):
    def __init__(self, min, max):
        self.min = min
        self.max = max

    def getAValue(self):
        return random.randint(self.min, self.max)


def toXML(name, attrs={}, childs=[], body=None):
    elem = ET.Element(name, attrib=attrs)
    if childs:
        for XMLElem in tuplesToXML(childs):
            elem.append(XMLElem)
    if body is not None:
        elem.text = body
    return elem


def tuplesToXML(tuples):
    """ tuples is an array (or not) of (name, attrs, childs, body) """
    if isinstance(tuples, tuple):
        tuples = [tuples]
    for tpl in tuples:
        yield toXML(*tpl)


def XMLToTuple(elem):
    """ Transform an XML element into the following tuple:
    (tagname, attributes, subelements, text) where:
     - tagname is the element tag as string
     - attributes is a dictionnary of the element attributes
     - subelements are the sub elements as an array of tuple
     - text is the content of the element, as string or None if no content is
       there
    """
    childs = []
    for child in elem:
        childs.append(XMLToTuple(child))
    return elem.tag, elem.attrib, childs, elem.text


def ConnectionErrorToMessage(ce):
    excpt = ce.args[0]
    if isinstance(excpt, socket.error):
        return excpt.reason.strerror
    return 'ConnectionError'


class GalileoClient(object):
    ID = '6de4df71-17f9-43ea-9854-67f842021e05'

    def __init__(self, scheme, host, path, port=None):
        self.scheme = scheme
        self.host = host
        self.path = path
        self._port = port
        self.server_state = None
        self._version = None

    @property
    def port(self):
        if self._port is None:
            return {'http': 80, 'https': 443}[self.scheme]
        return self._port

    @property
    def url(self):
        return "%(scheme)s://%(host)s:%(port)d/%(path)s" % {
            'scheme': self.scheme,
            'host': self.host,
            'port': self.port,
            'path': self.path}

    @property
    def version(self):
        if self._version is not None:
            # We're not completely lying ;)
            return self._version + ' (really: %s)' % __version__
        return __version__

    def post(self, mode, dongle=None, data=None):
        client = toXML('galileo-client', {'version': "2.0"})
        info = toXML('client-info', childs=[
            ('client-id', {}, [], self.ID),
            ('client-version', {}, [], self.version),
            ('client-mode', {}, [], mode)])
        if (dongle is not None) and dongle.hasVersion:
            info.append(toXML(
                'dongle-version',
                {'major': str(dongle.major),
                 'minor': str(dongle.minor)}))
        client.append(info)
        if self.server_state is not None:
            client.append(toXML('server-state', body=self.server_state))
        if data is not None:
            for XMLElem in tuplesToXML(data):
                client.append(XMLElem)

        f = BytesIO()

        tree = ET.ElementTree(client)
        tree.write(f, "utf-8", xml_declaration=True)

        logger.debug('HTTP POST=%s', f.getvalue())
        r = requests.post(self.url,
                          data=f.getvalue(),
                          headers={"Content-Type": "text/xml"})
        f.close()
        r.raise_for_status()

        try:
            answer = r.text
        except AttributeError:
            answer = r.content

        logger.debug('HTTP response=%s', answer)

        tag, attrib, childs, body = XMLToTuple(ET.fromstring(
            answer.encode('utf-8')))

        if tag != 'galileo-server':
            logger.error("Unexpected root element: %s", tag)

        if attrib['version'] != "2.0":
            logger.warning("Unexpected server version: %s", attrib['version'])

        for child in childs:
            stag, _, schilds, sbody = child
            if stag == 'error':
                raise SyncError(sbody)
            elif stag == 'back-off':
                minD = 0
                maxD = 0
                for schild in schilds:
                    sstag, _, _, ssbody = schild
                    if sstag == 'min': minD = int(ssbody)
                    if sstag == 'max': maxD = int(ssbody)
                raise BackOffException(minD, maxD)
            elif stag == 'server-state':
                self.server_state = sbody
            elif stag == 'redirect':
                for schild in schilds:
                    sstag, _, _, ssbody = schild
                    if sstag == 'protocol': self.scheme = ssbody
                    if sstag == 'host': self.host = ssbody
                    if sstag == 'port': self._port = int(ssbody)
                logger.info('Found redirect to %s' % self.url)

        return childs

    def requestStatus(self, allowHTTP=False):
        try:
            self.post('status')
        except requests.exceptions.ConnectionError as ce:
            error_msg = ConnectionErrorToMessage(ce)
            # No internet connection or fitbit server down
            logger.error("Not able to connect to the Fitbit server using %s:"
                         " %s.", self.scheme.upper(), error_msg)
        else:
            return True

        if self.scheme == 'https' and not allowHTTP:
            logger.warning('Config disallow the fallback to HTTP, you might'
                           ' want to give it a try (--no-https-only)')

        if self.scheme == 'http' or not allowHTTP:
            return False

        logger.info('Trying http as a backup.')
        self.scheme = 'http'
        try:
            self.post('status')
        except requests.exceptions.ConnectionError as ce:
            error_msg = ConnectionErrorToMessage(ce)
            # No internet connection or fitbit server down
            logger.error("Not able to connect to the Fitbit server using"
                         " either HTTP or HTTPS (%s). Check your internet"
                         " connection", error_msg)
        else:
            return True

        return False

    def sync(self, dongle, trackerId, megadump):
        try:
            server = self.post('sync', dongle, (
                'tracker', {'tracker-id': trackerId}, (
                    'data', {}, [], megadump.toBase64())))
        except requests.exceptions.ConnectionError as ce:
            error_msg = ConnectionErrorToMessage(ce)
            raise SyncError('ConnectionError: %s' % error_msg)

        tracker = None
        for elem in server:
            if elem[0] == 'tracker':
                tracker = elem
                break

        if tracker is None:
            raise SyncError('no tracker')

        _, a, c, _ = tracker
        if a['tracker-id'] != trackerId:
            logger.error("Got the response for tracker %s, expected tracker"
                         " %s", a['tracker-id'], trackerId)
        if a['type'] != 'megadumpresponse':
            logger.error('Not a megadumpresponse: %s', a['type'])

        if not c:
            raise SyncError('no data')
        if len(c) != 1:
            logger.error("Unexpected childs length: %d", len(c))
        t, _, _, d = c[0]
        if t != 'data':
            raise SyncError('not data: %s' % t)

        return s2a(base64.b64decode(d))
