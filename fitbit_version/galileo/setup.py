#!/usr/bin/env python
# -*- coding: utf-8 -*-

import re
import sys

try:
    from setuptools import setup, find_packages, Command
except ImportError:
    import distribute_setup
    distribute_setup.use_setuptools()
    from setuptools import setup

from galileo import __version__


class CheckVersion(Command):
    """ Check that the version in the docs is the correct one """
    description = "Check the version consistency"
    user_options = []
    def initialize_options(self):
        """init options"""
        pass

    def finalize_options(self):
        """finalize options"""
        pass

    def run(self):
        readme_re = re.compile(r'^:version:\s+' + __version__ + r'\s*$',
                               re.MULTILINE | re.IGNORECASE)
        man_re = re.compile(r'^\.TH.+[\s"]+' + __version__ + r'[\s"]+',
                            re.MULTILINE | re.IGNORECASE)
        for filename, regex in (
                ('README.txt', readme_re),
                ('doc/galileo.1', man_re),
                ('doc/galileorc.5', man_re)):
            with open(filename) as f:
                content = f.read()
            if regex.search(content) is None:
                raise ValueError('file %s mention the wrong version' % filename)

with open('README.txt') as file:
    long_description = file.read()

setup(
    name="galileo",
    version=__version__,
    description="Utility to securely synchronize a Fitbit tracker with the"
                " Fitbit server",
    long_description=long_description,
    author="Benoît Allard",
    author_email="benoit.allard@gmx.de",
    url="https://bitbucket.org/benallard/galileo",
    platforms=['any'],
    keywords=['fitbit', 'synchronize', 'health', 'tracker'],
    license="LGPL",
    install_requires=[
        "requests",
        "pyusb>=1a"],  # version 1a doesn't exists, but is smaller than 1.0.0a2
    test_suite="tests",
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'License :: OSI Approved :: GNU Lesser General Public License v3 or'
        ' later (LGPLv3+)',
        'Environment :: Console',
        'Topic :: Utilities',
        'Topic :: Internet',
        'Operating System :: OS Independent',
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.6',
        'Programming Language :: Python :: 2.7',
    ],
    packages=find_packages(exclude=["tests"]),
    entry_points={
        'console_scripts': [
            'galileo = galileo.main:main'
        ],
    },
    cmdclass={
        'checkversion': CheckVersion,
    },
)
