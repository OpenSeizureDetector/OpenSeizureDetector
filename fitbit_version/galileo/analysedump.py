#!/usr/bin/env python

import os
import sys
import time
import base64


def readlog(f):
    """ input is from f in the format of lines of long string starting
    with a tab ('\t') representing the hexadcimal representation of the data
    (megadump)"""
    d = []
    for line in f:
        if line[0] != '\t':
            if d:
                return d
            continue
        line = line.strip()
        for i in range(0, len(line), 2):
            d.append(int(line[i:i + 2], 16))
    return d


def readdump(f):
    """ imput is from ./galileo.py """
    d = []
    r = []
    current = d
    for line in f:
        if line.strip() == '':
            current = r
            continue
        current.extend(int(x, 16) for x in line.strip().split())
    return d, r


def a2s(array):
    """ array of int to string """
    return ''.join(chr(c) for c in array)


def a2x(array):
    """ array of int to hex representation """
    return ' '.join("%02X" % i for i in array)


def a2lsbi(array):
    """ array to int (LSB first) """
    integer = 0
    for i in range(len(array) - 1, -1, -1):
        integer *= 256
        integer += array[i]
#        print a2x(array), hex(integer)
    return integer


def a2msbi(array):
    """ array to int (MSB first) """
    integer = 0
    for i in range(len(array)):
        integer *= 256
        integer += array[i]
    return integer


def header(data):
    index = 40
    walkStrideLen = a2lsbi(data[index:index + 2])
    index += 2
    runStrideLen = a2lsbi(data[index:index + 2])
    index += 2
    print "Stride lengths: %dmm, %dmm" % (walkStrideLen, runStrideLen)
    print a2x(data[index:index + 4])
    index += 4
    # empirical value
    index += 12

    if index >= len(data): return
    # Greetings
    print "Greetings: '%s'" % a2s(data[index:index + 10])
    index += 10

    # Cheering
    print "Cheering"
    for i in range(3):
        print "'%s'" % a2s(data[index:index + 10])
        index += 10


def first_field(rec_len):
    def unknown(data):
        assert data[:3] == [END, END, 0xdd], a2x(data[:3])
        index = 3
        while index < len(data) - 1:
            tstamp = a2lsbi(data[index:index + 4])
            print time.strftime("%x %X", time.localtime(tstamp)), hex(tstamp)
            index += 4
            print "\t%s" % a2x(data[index:index + rec_len])
            index += rec_len
    return unknown


def minutely(rec_len):
    """ this analyses the minute-by-minute information
    """
    def minutes(data):
        assert data[:3] == [END, END, 0xdd], a2x(data[:3])
        index = 3
        tstamp = 0
        while index < len(data) - 1:
            if not (data[index] & 0x80):
                tstamp = a2msbi(data[index:index + 4])
                index += 4
            else:
                print time.strftime("%x %X", time.localtime(tstamp)), a2x(data[index:index + rec_len])
                tstamp += 60
                index += rec_len
    return minutes


def stairs(data):
    """ Looks like stairs informations are put here """
    assert data[:3] == [END, END, 0xdd], a2x(data)
    index = 3
    index = 3
    tstamp = 0
    while index < len(data) - 1:
        if not (data[index] & 0x80):
            tstamp = a2msbi(data[index:index + 4])
            index += 4
        else:
            if data[index] != 0x80:
                #print a2x([array[index]])
                index += 1
            print time.strftime("%x %X", time.localtime(tstamp)), a2x(data[index:index + 2])
            tstamp += 60
            index += 2


def daily(data):
    if len(data) == 2:
        assert data == [END, END], a2x(data)
        return
    assert data[:3] == [END, END, 0xdd], a2x(data[:3])
    index = 3
    while index < len(data) - 1:
        tstamp = a2lsbi(data[index:index + 4])
        index += 4
        print time.strftime("%x %X", time.localtime(tstamp)), a2x(data[index:index + 12])
        index += 12


def footer(data):
    assert len(data) == 9, data
    print 'Dump size: %d bytes' % a2lsbi(data[5:7])
    print a2x(data)

ESC = 0xdb
END = 0xc0
ESC_ = {0xdc: END, 0xdd: ESC}


def unSLIP(data):
    """ This remove SLIP escaping and yield the parts
    The magic are: The first part doesn't ends with 0xC0
    there are empty parts
    >>> list(unSLIP([1, 2, 0xc0, 5, 4, 0xc0, 0xc0, 8, 4]))
    [[1, 2], [192, 5, 4, 192], [192, 8, 4]]
    >>> list(unSLIP([12, 0xc0, 0, 0, 0xc0, 1, 2, 0xc0, 8, 9]))
    """
    first = True
    part = []
    escape = False
    for c in data:
#        print "%x" % c
        if not escape:
            if c == ESC and part and part[0] == END:
                escape = True
            else:
                part.append(c)
                if c == END:
                    if len(part) != 1:
                        if first or (part[0] != END):
                            yield part[:-1]
                            part = [part[-1]]
                            first = False
                        else:
                            yield part
                            part = []
        else:
            part.append(ESC_[c])
            escape = False
    yield part


def analyse(data):

    def onscreen(data):
        print a2x(data)

    def skip(data):
        pass

    display = [onscreen] * 20

    analyses_ZIP = [header, first_field(9), minutely(3), daily, footer]

    analyses_ONE = [header, first_field(11), minutely(4), stairs, daily, skip, onscreen, skip, footer]

    analyses = {
        0x26: analyses_ONE,
        0xF4: analyses_ZIP,
    }.get(data[0], display)

    for i, part in enumerate(unSLIP(data)):
        f = analyses[i]
        print "%s (%d): %d bytes" % (f.__name__, i, len(part))
        f(part)


def analysedump(dump_dir, index):
    for root, dirs, files in os.walk(dir):
        file = sorted(files)[idx]
    print "Analysing %s" % file
    with open(os.path.join(root, file)) as f:
        dump, response = readdump(f)
        analyse(dump)

if __name__ == "__main__":
    if len(sys.argv) == 1:
        dump, response = readdump(sys.stdin)
        analyse(dump)
    else:
        dir = sys.argv[1]
        idx = -1
        if len(sys.argv) > 2:
            idx = int(sys.argv[2])
        analysedump(dir, idx)
