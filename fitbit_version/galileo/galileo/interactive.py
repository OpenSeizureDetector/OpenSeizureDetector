"""\
This is the implementation of the interactive mode

This is the same idea as ifitbit I wrote for libfitbit some years ago

https://bitbucket.org/benallard/libfitbit/src/tip/python/ifitbit.py?at=default

"""

from __future__ import print_function

#---------------------------
# The engine

import readline
import traceback
import sys

exit = None

cmds = {}
helps = {}

def command(cmd, help):
    def decorator(fn):
        cmds[cmd] = fn
        helps[cmd] = help
        def wrapped(*args):
            return fn(*args)
        return wrapped
    return decorator


@command('x', "Quit")
def quit():
    print('Bye !')
    global exit
    exit = True


@command('?', 'Print possible commands')
def print_help():
    for cmd in sorted(helps.keys()):
        print('%s\t%s' % (cmd, helps[cmd]))
    print("""Note:
 - You can enter multiple commands separated by ';'
 - To establish a link with the tracker, enter the following command:
      c ; d ; l ; tx 1 ; al
""")

def main(config):
    global exit
    exit = False
    print_help()
    while not exit:
        orders = raw_input('> ').strip()
        if ';' in orders:
            orders = orders.split(';')
        else:
            orders = [orders]
        for order in orders:
            order = order.strip().split(' ')
            try:
                f = cmds[order[0]]
            except KeyError:
                if order[0] == '':
                    continue
                print('Command %s not known' % order[0])
                print_help()
                continue
            try:
                f(*order[1:])
            except TypeError as te:
                print("Wrong number of argument given: %s" % te)
            except Exception as e:
                # We need that to be able to close the connection nicely
                print("BaD bAd BAd", e)
                traceback.print_exc(file=sys.stdout)
                return


#---------------------------
# The commands

from .dongle import FitBitDongle, CM, DM
from .tracker import FitbitClient
from .utils import x2a

import uuid

dongle = None
fitbit = None
trackers = []
tracker = None

@command('c', "Connect")
def connect():
    global dongle
    dongle = FitBitDongle(0)  # No DataRing needed
    if not dongle.setup():
        print("No dongle connected, aborting")
        quit()
    global fitbit
    fitbit = FitbitClient(dongle)
    print('Ok')


def needfitbit(fn):
    def wrapped(*args):
        if dongle is None:
            print("No connection, connect (c) first")
            return
        return fn(*args)
    return wrapped

@command('->', "Send on the control channel")
@needfitbit
def send_ctrl(INS, *payload):
    if payload:
        payload = x2a(' '.join(payload))
    else:
        payload = []
    m = CM(int(INS, 16), payload)
    dongle.ctrl_write(m)

@command('<-', "Receive once on the control channel")
@needfitbit
def receive_ctrl(param='1'):
    if param == '-':
        goOn = True
        while goOn:
            goOn = dongle.ctrl_read() is not None
    else:
        for i in range(int(param)):
            dongle.ctrl_read()

@command('=>', "Send on the control channel")
@needfitbit
def send_data(*payload):
    m = DM(x2a(' '.join(payload)))
    dongle.data_write(m)

@command('<=', "Receive once on the control channel")
@needfitbit
def receive_data(param='1'):
    if param == '-':
        goOn = True
        while goOn:
            goOn = dongle.data_read() is not None
    else:
        for i in range(int(param)):
            dongle.data_read()

@command('d', "Discovery")
@needfitbit
def discovery(UUID="{ADAB0000-6E7D-4601-BDA2-BFFAA68956BA}"):
    UUID = uuid.UUID(UUID)
    global trackers
    trackers = [t for t in fitbit.discover(UUID)]


def needtrackers(fn):
    def wrapped(*args):
        if not trackers:
            print("No trackers, run a discovery (d) first")
            return
        return fn(*args)
    return wrapped

@command('l', "establishLink")
@needtrackers
def establishLink(idx='0'):
    global tracker
    tracker = trackers[int(idx)]
    if fitbit.establishLink(tracker):
        print('Ok')
    else:
        tracker = None

@command('L', "ceaseLink")
@needfitbit
def ceaseLink():
    if not fitbit.ceaseLink():
        print('Bad')
    else:
        print('Ok')

def needtracker(fn):
    def wrapped(*args):
        if tracker is None:
            print("No tracker, establish a Link (l) first")
            return
        return fn(*args)
    return wrapped

@command('tx', "toggle Tx Pipe")
@needfitbit
def toggleTxPipe(on):
    if fitbit.toggleTxPipe(bool(int(on))):
        print('Ok')

@command('al', "initialise airLink")
@needtracker
def initialiseAirLink():
    if fitbit.initializeAirlink(tracker):
        print('Ok')

@command('AL', "terminate airLink")
@needfitbit
def terminateairLink():
    if fitbit.terminateAirlink():
        print('Ok')

@command('D', 'getDump')
@needfitbit
def getDump(type="13"):
    fitbit.getDump(int(type))

@command('R', 'uploadResponse')
@needfitbit
def uploadResponse(*response):
    response = x2a(' '.join(response))
    fitbit.uploadResponse(response)
