def a2x(a, delim=' '):
    """ array to string of hexa
    delim is the delimiter between the hexa
    """
    return delim.join('%02X' % x for x in a)


def x2a(hexstr):
    """ String of hex a to array """
    return [int(x, 16) for x in hexstr.split(' ')]


def a2s(a, toPrint=True):
    """ array to string
    toPrint indicates that the resulting string is to be printed (stop at the
    first \0)
    """
    s = []
    for c in a:
        if toPrint and (c == 0):
            break
        s.append(chr(c))
    return ''.join(s)


def a2lsbi(array):
    """ array to int (LSB first) """
    integer = 0
    for i in range(len(array) - 1, -1, -1):
        integer *= 256
        integer += array[i]
    return integer


def a2msbi(array):
    """ array to int (MSB first) """
    integer = 0
    for i in range(len(array)):
        integer *= 256
        integer += array[i]
    return integer


def i2lsba(value, width):
    """ int to array (LSB first) """
    a = [0] * width
    for i in range(width):
        a[i] = (value >> (i*8)) & 0xff
    return a


def s2a(s):
    """ string to array """
    return [ord(c) for c in s]
