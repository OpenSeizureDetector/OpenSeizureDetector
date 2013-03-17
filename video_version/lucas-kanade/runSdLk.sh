#!/bin/sh

FIFO=seizure_test.mpg
if [ -e $FIFO ]; then 
    rm $FIFO
fi

mkfifo $FIFO

./sd_lk.py &