cmake_minimum_required(VERSION 2.8)
PROJECT( sd )
FIND_PACKAGE( OpenCV REQUIRED )
ADD_EXECUTABLE( sd Seizure_Detector.c )
TARGET_LINK_LIBRARIES( sd ${OpenCV_LIBS} )

