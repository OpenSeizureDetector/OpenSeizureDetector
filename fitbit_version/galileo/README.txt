Galileo
=======

:author: Benoît Allard <benoit.allard@gmx.de>
:version: 0.5dev
:license: LGPLv3+
:bug tracker: https://bitbucket.org/benallard/galileo/issues
:mailing list: galileo@freelists.org (subscribe_, archive_)
:build status: |droneio_badge|_

.. _subscribe: mailto:galileo-request@freelists.org?subject=subscribe
.. _archive: http://freelists.org/archive/galileo/
.. |droneio_badge| image:: https://drone.io/bitbucket.org/benallard/galileo/status.png
.. _droneio_badge: https://drone.io/bitbucket.org/benallard/galileo

Introduction
------------

Galileo is a Python utility to securely synchronize a Fitbit device with the
Fitbit web service. It allows you to browse your data on their website, and
compatible applications.

All Bluetooth-based trackers are supported. Those are:

- Fitbit One
- Fitbit Zip
- Fitbit Flex
- Fitbit Force
- Fitbit Charge

.. note:: The Fitbit Ultra tracker is **not supported** as it communicates
          using the ANT protocol. To synchronize it, please use libfitbit_.

This utility is mainly targeted at Linux because Fitbit does not
provide any Linux-compatible software, but as Python is
cross-platform and the libraries used are available on a broad variety
of platforms, it should not be too difficult to port it to other
platforms.

.. _libfitbit: https://github.com/openyou/libfitbit

Main features
-------------

- Synchronize your fitbit tracker with the fitbit server using the provided
  dongle.
- Securely communicate (using HTTPS) with the fitbit server.
- Save all your dumps locally for possible later analyse.

Installation
------------

The easy way
~~~~~~~~~~~~

.. warning:: If you want to run the utility as a non-root user, you will have
             to install the udev rules manually (See `The more complicated
             way`_, or follow the instructions given when it fails).

::

    $ pip install galileo
    $ galileo

.. note:: If you don't want to install this utility system-wide, you
          may want to install it inside a virtualenv_, the behaviour
          will not be affected.

.. _virtualenv: http://www.virtualenv.org

Distribution packages
~~~~~~~~~~~~~~~~~~~~~

The following Linux distributions have packages available for installation:

**Arch**:
  The utility is available from AUR_. You can install it using the yaourt_ package manager: ``yaourt -S galileo``.
**Fedora**:
  The utility is packaged in a `COPR repo`_.  Download the relevant repo
  for your version of Fedora, and then ``yum install galileo``.
**Gentoo**:
  The utility is packaged as ``app-misc/galileo`` within the
  `squeezebox <http://git.overlays.gentoo.org/gitweb/?p=user/squeezebox.git>`_
  overlay. See https://wiki.gentoo.org/wiki/Layman for details of how
  to use Gentoo overlays.
**Ubuntu**:
  The utility is available over the ppa ``ppa:cwayne18/fitbit``. Use the
  following commands to install it and start the daemon::

    sudo add-apt-repository ppa:cwayne18/fitbit
    sudo apt-get update && sudo apt-get install galileo
    start galileo﻿

.. _AUR: https://aur.archlinux.org/packages/galileo/
.. _yaourt: https://wiki.archlinux.org/index.php/yaourt

.. _`COPR repo`: https://copr.fedoraproject.org/coprs/stbenjam/galileo/

The more complicated way
~~~~~~~~~~~~~~~~~~~~~~~~

First, you need to clone this repository locally, and install the required
dependencies:

**pyusb**:
  Need at least a 1.0 version, 0.4 and earlier are not compatible.
  Please use a tagged release as development version might contains bug or
  interface breakage.
**requests**:
  Newer versions (2.x) preferred, although older should also work.

You should copy the file ``99-fitbit.rules`` to the directory
``/etc/udev/rules.d`` in order to be able to run the utility as a
non-root user.

Don't forget to:

- restart the udev service: ``sudo service udev restart``
- unplug and re-insert the dongle to activate the new rule.

Then simply run the ``run`` script located at the root of this repository.

If your system uses systemd then there is an example unit file in the
``contrib`` directory that you may wish to customize.

Documentation
-------------

For the moment, this README (and the ``--help`` command line option) is the
main documentation we have. The wiki_ is meant to gather technical
information about the project like the communication protocol, or the format
of the dump. Once this information reached a suffficient level of maturation,
the goal is to integrate it into the project documentation. So head-on there,
and start sharing your findings !

Manual pages for the galileo_\(1) utility and the galileorc_\(5) configuration
file are provided within the ``doc`` directory.

.. _wiki: https://bitbucket.org/benallard/galileo/wiki
.. _galileo: https://pythonhosted.org/galileo/galileo.1.html
.. _galileorc: https://pythonhosted.org/galileo/galileorc.5.html

Acknowledgements
----------------

Special thanks to the folks present @ the `issue 46`_ of libfitbit.

Especialy to `sansneural <https://github.com/sansneural>`_ for
https://docs.google.com/file/d/0BwJmJQV9_KRcSE0ySGxkbG1PbVE/edit and
`Ingo Lütkebohle`_ for http://pastebin.com/KZS2inpq.

.. _`issue 46`: https://github.com/openyou/libfitbit/issues/46
.. _`Ingo Lütkebohle`: https://github.com/iluetkeb

Disclaimer
----------

Fitbit is a registered trademark and service mark of Fitbit, Inc.  galileo is
designed for use with the Fitbit platform.  This product is not put out by
Fitbit, and Fitbit does not service or warrant the functionality of this
product.
