import unittest
import sys

from galileo import __version__

import galileo.net
from galileo.net import GalileoClient, SyncError, BackOffException

class requestResponse(object):
    def __init__(self, text, server_version='<server-version>\n\n</server-version>'):
        self.text = """<?xml version="1.0" encoding="utf-8" standalone="yes"?><galileo-server version="2.0">%s%s</galileo-server>""" % (server_version, text)
    def raise_for_status(self): pass

class testStatus(unittest.TestCase):

    def testOk(self):
        def mypost(url, data, headers):
            self.assertEqual(url, 'scheme://host:8888/path/to/stuff')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>status</client-mode></client-info></galileo-client>""" % {
    'id': GalileoClient.ID, 'version': __version__})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('')

        galileo.net.requests.post = mypost
        gc = GalileoClient('scheme', 'host', 'path/to/stuff', 8888)
        gc.requestStatus()

    def testError(self):
        def mypost(url, data, headers):
            self.assertEqual(url, 'h://c:8/p')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>status</client-mode></client-info></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('<error>Something is Wrong</error>')

        galileo.net.requests.post = mypost
        gc = GalileoClient('h', 'c', 'p', 8)
        self.assertRaises(SyncError, gc.requestStatus)

    def testBackOff(self):
        # no support for ``with assertRaises`` in python 2.6
        if sys.version_info < (2,7): return
        def mypost(url, data, headers):
            self.assertEqual(url, 'h://c:4/p')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>status</client-mode></client-info></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse("""
    <back-off>
        <min>1800000</min>
        <max>3600000</max>
    </back-off>
    <ui-request action="login">
        <client-display height="450" width="650" minDisplayTimeMs="20000" containsForm="false">
            Server is in maintenance mode. We'll be back soon!
        </client-display>
    </ui-request>""", '')

        galileo.net.requests.post = mypost
        gc = GalileoClient('h', 'c', 'p', 4)
        with self.assertRaises(BackOffException) as cm:
            gc.requestStatus()
        e = cm.exception
        self.assertEqual(e.min, 1800000)
        self.assertEqual(e.max, 3600000)
        val = e.getAValue()
        self.assertTrue(e.min <= val <= e.max)


    def testStatusRequests082(self):
        """ Older versions of requests only have ``content`` and no ``text`` """
        def mypost(url, data, headers):
            self.assertEqual(url, 'scheme://host:8888/path/to/stuff')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>status</client-mode></client-info></galileo-client>""" % {
    'id': GalileoClient.ID, 'version': __version__})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            res = requestResponse('')
            res.content = res.text
            delattr(res, 'text')
            return res

        galileo.net.requests.post = mypost
        gc = GalileoClient('scheme', 'host', 'path/to/stuff', 8888)
        gc.requestStatus()

class MyDongle(object):
    def __init__(self, M, m): self.major=M; self.minor=m; self.hasVersion=True
class MyMegaDump(object):
    def __init__(self, b64): self.b64 = b64
    def toBase64(self): return self.b64

class testSync(unittest.TestCase):

    def testOk(self):
        T_ID = 'abcd'
        D = MyDongle(0, 0)
        d = MyMegaDump('YWJjZA==')
        def mypost(url, data, headers):
            self.assertEqual(url, 'a://b:0/c')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>sync</client-mode><dongle-version major="%(M)d" minor="%(m)d" /></client-info><tracker tracker-id="%(t_id)s"><data>%(b64dump)s</data></tracker></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__,
    'M': D.major,
    'm': D.minor,
    't_id': T_ID,
    'b64dump': d.toBase64()})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('<tracker tracker-id="abcd" type="megadumpresponse"><data>ZWZnaA==</data></tracker>')

        galileo.net.requests.post = mypost
        gc = GalileoClient('a', 'b', 'c', 0)
        self.assertEqual(gc.sync(D, T_ID, d),
                         [101, 102, 103, 104])

    def testNoTracker(self):
        T_ID = 'aaaabbbb'
        D = MyDongle(34, 88)
        d = MyMegaDump('base64Dump')
        def mypost(url, data, headers):
            self.assertEqual(url, 'z://y:42/u')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>sync</client-mode><dongle-version major="%(M)d" minor="%(m)d" /></client-info><tracker tracker-id="%(t_id)s"><data>%(b64dump)s</data></tracker></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__,
    'M': D.major,
    'm': D.minor,
    't_id': T_ID,
    'b64dump': d.toBase64()})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('')

        galileo.net.requests.post = mypost
        gc = GalileoClient('z', 'y', 'u', 42)
        self.assertRaises(SyncError, gc.sync, D, T_ID, d)

    def testNoData(self):
        T_ID = 'aaaa'
        D = MyDongle(-2, 42)
        d = MyMegaDump('base64Dump')
        def mypost(url, data, headers):
            self.assertEqual(url, 'y://t:8000/v')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>sync</client-mode><dongle-version major="%(M)d" minor="%(m)d" /></client-info><tracker tracker-id="%(t_id)s"><data>%(b64dump)s</data></tracker></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__,
    'M': D.major,
    'm': D.minor,
    't_id': T_ID,
    'b64dump': d.toBase64()})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('<tracker tracker-id="abcd" type="megadumpresponse"></tracker>')

        galileo.net.requests.post = mypost
        gc = GalileoClient('y', 't', 'v', 8000)
        self.assertRaises(SyncError, gc.sync, D, T_ID, d)

    def testNotData(self):
        T_ID = 'aaaabbbbccccdddd'
        D = MyDongle(-2, 42)
        d = MyMegaDump('base64Dump')
        def mypost(url, data, headers):
            self.assertEqual(url, 'rsync://ssh:22/a/b/c')
            self.assertEqual(data.decode('utf-8'), """\
<?xml version='1.0' encoding='utf-8'?>
<galileo-client version="2.0"><client-info><client-id>%(id)s</client-id><client-version>%(version)s</client-version><client-mode>sync</client-mode><dongle-version major="%(M)d" minor="%(m)d" /></client-info><tracker tracker-id="%(t_id)s"><data>%(b64dump)s</data></tracker></galileo-client>""" % {
    'id': GalileoClient.ID,
    'version': __version__,
    'M': D.major,
    'm': D.minor,
    't_id': T_ID,
    'b64dump': d.toBase64()})
            self.assertEqual(headers['Content-Type'], 'text/xml')
            return requestResponse('<tracker tracker-id="abcd" type="megadumpresponse"><not_data /></tracker>')

        galileo.net.requests.post = mypost
        gc = GalileoClient('rsync', 'ssh', 'a/b/c', 22)
        self.assertRaises(SyncError, gc.sync, D, T_ID, d)

    def testConnectionError(self):
        T_ID = 'abcd'
        D = MyDongle(0, 0)
        d = MyMegaDump('YWJjZA==')
        def mypost(url, data, headers):
            class Reason(object):
                class Error(object): strerror = ''
                reason = Error()
            raise galileo.net.requests.exceptions.ConnectionError(Reason())

        galileo.net.requests.post = mypost
        gc = GalileoClient('a', 'b', 'c', 0)
        self.assertRaises(SyncError, gc.sync,D, T_ID, d)

class testURL(unittest.TestCase):

    def testWithPort(self):
        gc = GalileoClient('scheme', 'host', 'path/to/stuff', 8000)
        self.assertEqual(gc.url, 'scheme://host:8000/path/to/stuff')

    def testHTTPPort(self):
        gc = GalileoClient('http', 'h', 'a/b/c')
        self.assertEqual(gc.url, 'http://h:80/a/b/c')

    def testHTTPSPort(self):
        gc = GalileoClient('https', 'h', 'a/b/c')
        self.assertEqual(gc.url, 'https://h:443/a/b/c')

    def testUnknownPort(self):
        # no support for ``with assertRaises`` in python 2.6
        if sys.version_info < (2,7): return
        gc = GalileoClient('blah', 'h', 'a')
        with self.assertRaises(KeyError):
            gc.url
