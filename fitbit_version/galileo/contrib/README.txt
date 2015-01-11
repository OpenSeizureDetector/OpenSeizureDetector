Miscellaneous Contributions
===========================

This directory contains a number of additional files that may be of
interest. These are not necessary to use Galileo, but can help those
packaging or customising the utility.

galileo.service
---------------

This is a **systemd unit** file that can be used to start and stop the
Galileo utility when running in 'daemon mode'. This was originally
created for the Gentoo package and will likely need customization for
other distributions. In particular, the user and group that the daemon
runs as, as well as the location of the configuration file, may need
to be changed.

This service unit file should be installed into the lib/systemd/system
directory.


galileo.upstart
---------------

This is an **upstart** file that can be used to start and stop the galileo
utility when running in 'daemon mode'. This was originally created for the
Ubuntu distribution, and might need some customization to run elsewhere.
