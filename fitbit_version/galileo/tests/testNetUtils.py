import unittest

import xml.etree.ElementTree as ET
from io import BytesIO

from galileo.net import toXML, tuplesToXML, XMLToTuple

class testtoXML(unittest.TestCase):

    def _testEqual(self, xml, xmlStr):
        tree = ET.ElementTree(xml)
        f = BytesIO()
        tree.write(f)
        self.assertEqual(f.getvalue().decode('utf-8'), xmlStr)
        f.close()

    def testSimple(self):
        self._testEqual(toXML('elem'), '<elem />')
    def testSimpleWithAttrs(self):
        self._testEqual(toXML('elem', {'attr1': 'val',
                                       'attr2': 'val'}),
                        '<elem attr1="val" attr2="val" />')
    def testSimpleWithBody(self):
        self._testEqual(toXML('elem', body="body"), '<elem>body</elem>')
    def testSimpleWithChilds(self):
        self._testEqual(toXML('parent', childs=[('child1',), ('child2',)]),
                        '<parent><child1 /><child2 /></parent>')
    def testFull(self):
        self._testEqual(toXML('parent', {'a':'c'}, [('c',),
                                                    ('c', {}, [], 'b')], 'b'),
                        '<parent a="c">b<c /><c>b</c></parent>')

    def testOnetuplesToXML(self):
        xmls = list(tuplesToXML(('p',{'a':'a'}, [], 'b')))
        self.assertEqual(len(xmls), 1)
        self._testEqual(xmls[0], '<p a="a">b</p>')
    def testOnetuplesToXML2(self):
        xmls = list(tuplesToXML([('p',{'a':'a'}, [], 'b')]))
        self.assertEqual(len(xmls), 1)
        self._testEqual(xmls[0], '<p a="a">b</p>')

    def testMultipletuplesToXML(self):
        xmls = list(tuplesToXML([('p',{'a':'a'}, [], 'b'),
                                 ('p'),
                                 ('p', {}, [], 'b')]))
        self.assertEqual(len(xmls), 3)
        self._testEqual(xmls[0], '<p a="a">b</p>')
        self._testEqual(xmls[1], '<p />')
        self._testEqual(xmls[2], '<p>b</p>')

class testtoTuple(unittest.TestCase):
    def _testEqual(self, xmlStr, tpls):
        tpl =  XMLToTuple(ET.fromstring(xmlStr))
        self.assertEqual(tpl, tpls)

    def testSimple(self):
        self._testEqual('<e />', ('e', {}, [], None))

    def testSimpleWithAttrs(self):
        self._testEqual('<e a1="v1" a2="v2"/>', ('e', {'a1': 'v1', 'a2': 'v2'},
                                                 [], None))

    def testSimpleWithBody(self):
        self._testEqual('<e>b</e>', ('e', {}, [], 'b'))

    def testSimpleWithChilds(self):
        self._testEqual('<p><c1 /><c2>b</c2></p>', ('p',
                                                    {},
                                                    [('c1', {}, [], None),
                                                     ('c2', {}, [], 'b')],
                                                    None))

    def testFull(self):
        self._testEqual('<p a1="v1">b1<c1 /><c2><sc1 /></c2></p>',
                        ('p',
                         {'a1':'v1'},
                         [('c1', {}, [], None),
                          ('c2', {}, [('sc1', {}, [], None)], None)],
                         'b1'))
