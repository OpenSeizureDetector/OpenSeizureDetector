#!/bin/bash

# remove old one
rm -f EVM_Matlab.zip

# create subdirectory and copy files there
mkdir -p EVM_Matlab
cp *.m EVM_Matlab
cp EVM_Matlab_README EVM_Matlab
cp LICENSE EVM_Matlab
cp -r matlabPyrTools EVM_Matlab

# create ZIP archive
zip -r EVM_Matlab.zip EVM_Matlab

# remove subdirectory
rm -fr EVM_Matlab
