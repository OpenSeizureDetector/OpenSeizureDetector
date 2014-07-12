#!/bin/sh
OF=/home/disk2/benFinder/benFinder.log
echo "Starting benFinder" > $OF
cd /home/graham/benFinder
python ./benFinder.py > $OF 2>&1
