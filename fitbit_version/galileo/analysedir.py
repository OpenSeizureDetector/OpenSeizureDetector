#!/usr/bin/env python

import os
import re

from analysedump import readdump
from analysedump import analyse as longanalyse

TYPES = {0xF4: 'Zip', 0x26: 'One', 0x28: 'Flex'}


def analyse(filename):
    s = []
    with open(filename, 'rt') as dump:
        data, response = readdump(dump)
    s.append(TYPES[data[0]])
    s.append(str(len(data)))
    s.append(str(len(response)))
    print ' '.join(s)
    try:
        longanalyse(data)
    except:
        print filename
        raise


def main(dirname):
    for root, dirs, files in os.walk(dirname):
        for dump in sorted(files):
            if not re.match('dump-\d{10}.txt', dump):
                continue
            filename = os.path.join(root, dump)
            print dump
            analyse(filename)

if __name__ == "__main__":
    import sys
    try:
        os.path.exists(sys.argv[1])
        filename = sys.argv[1]
        print "Single file mode: ", filename
        analyse(filename) 

    except:
        main(sys.argv[1])
