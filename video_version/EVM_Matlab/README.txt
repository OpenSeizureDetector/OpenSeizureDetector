This package is a MATLAB implementation of our paper

Eulerian Video Magnification for Revealing Subtle Changes in the World
ACM Transaction on Graphics, Volume 31, Number 4 (Proceedings SIGGRAPH
2012)

The paper and example videos can be found on the project web page
http://people.csail.mit.edu/mrub/vidmag/

The code is supplied for educational purposes only. Please refer to
the enclosed LICENSE.pdf file regarding permission to use the
software.  Please cite our paper if you use any part of the code or
data on the project web page. Please contact the authors below if you
wish to use the code commercially.

The code includes the following combination of spatial and temporal
filters, which we used to generate all the results in the paper:

	Spatial					Temporal
=========================================================================
	Laplacian pyramid		Ideal bandpass
	Laplacian pyramid		Butterworth bandpass
	Laplacian pyramid		Second-order IIR bandpass
	Gaussian pyramid		Ideal bandpass

The code was written in MATLAB R2011b, and tested on Windows 7, Mac OSX and
Linux. It uses the pyramid toolbox by Eero Simoncelli (matlabPyrTools),
available at http://www.cns.nyu.edu/~eero/software.php.
For convenience, we have included a copy of version 1.4 (updated Dec. 2009)
of their toolbox here.

To reproduce the results shown in the paper:

1) Download the source videos from the project web page into a directory
"data" inside the directory containing this code.
2) Start up MATLAB and change directory to the location of this code.
3) Run "install.m"
4) Run "reproduceResults.m" to reproduce all the results in the paper.
See the "reproduceResults.m" script for more details.

NOTE: Generating each of the results will take a few minutes. We
have selected parameters that result in better looking videos,
however, depending on your application, you may not need such high
quality results.

The parameters we used to generate the results presented in the
paper can be found in the script "reproduceResults.m". Please refer to the
paper for more detail on selecting the values for the parameters. In some
cases, the parameters reported in the paper do not exactly match the ones
in the script, as we have refined our parameters through experimentation.
Feel free to experiment on your own!

For questions/feedback/bugs, or if you would like to make commercial use of
this software, please contact
Hao-Yu Wu <haoyu@mit.edu> or Michael Rubinstein <mrub@mit.edu>
Computer Science and Artificial Intelligence Lab, Massachusetts Institute
of Technology

Sep 10, 2012

