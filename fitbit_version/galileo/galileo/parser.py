"""\
This is a custom implementation of the yaml parser in order to prevent an
extra dependency in the PyYAML module. This implementation will be used when
the PyYAML module will not be found.

The configurability of galileo should not be based on the possibility of this
parser. This parser should be adapted to allow the correct configuration.

Known limitations:
- Only spaces, no tabs
- Blank lines in the middle of an indented block is pretty bad ...
"""

from __future__ import print_function  # for the __main__ block

import json
import textwrap

def _stripcomment(line):
    s = []
    for c in line:
        if c == '#':
            break
        s.append(c)
    # And we strip the trailing spaces
    return ''.join(s).rstrip()


def _getident(line):
    i = 0
    for c in line:
        if c != ' ':
            break
        i += 1
    return i


def _addKey(d, key):
    if d is None and key:
        d = {}
    d[key] = None
    return d


def unJSONize(s):
    """ json is not good enough ...
    "'a'" doesn't get decoded,
    even worst, "a" neither """
    try:
        return json.loads(s)
    except ValueError:
        s = s.strip()
        if s[0] == "'" and s[-1] == "'":
            return s[1:-1]
        return s


def _dedent(lines, start):
    res = [lines[start]]
    idx = start + 1
    minident = _getident(lines[start])
    while idx < len(lines):
        curident = _getident(lines[idx])
        if curident < minident:
            break
        res.append(lines[idx])
        idx += 1
    return res


def loads(s):
    res = None
    current_key = None
    lines = s.split('\n')
    i = 0
    while i < len(lines):
        line = _stripcomment(lines[i])
        i += 1
        if not line: continue
        if _getident(line) == 0:
            if line.startswith('-'):
                if res is None:
                    res = []
                line = line[1:].strip()
                if line:
                    res.append(loads(line))
                elif i == len(lines):
                    res.append(None)
            elif ':' in line:
                current_key = None
                k, v = line.split(':')
                res = _addKey(res, k)
                if not v:
                    current_key = k
                else:
                    res[k] = unJSONize(v)
            else:
                return unJSONize(line)
        else:
            subblock = _dedent(lines, i-1)
            subres = loads(textwrap.dedent('\n'.join(subblock)))
            if isinstance(res, dict):
                res[current_key] = subres
            elif isinstance(res, list):
                res.append(subres)
            else:
                raise ValueError(res, subres)
            i += len(subblock) - 1

    return res


def load(f):
    return loads(f.read())

if __name__ == "__main__":
    import sys
    # For fun and quick test
    with open(sys.argv[1], 'rt') as f:
        print(load(f))
