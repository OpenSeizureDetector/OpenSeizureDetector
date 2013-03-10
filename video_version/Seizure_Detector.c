/*
*/

#include <stdio.h>
#include <opencv2/imgproc/imgproc_c.h>
#include <highgui.h>

CvCapture *camera = NULL;
CvMemStorage *memstore = NULL;
CvSeq *seq = NULL;

#define IMG_STACK_LEN 15 // How many images to store in memory for analysis.
#define BLUR_LEVEL 2     // How many times to down-sample the images to blur them.

IplImage *imgList[IMG_STACK_LEN];
int imgCount = 0;
IplImage *origImg = 0;
IplImage *blurImg1 = 0;
IplImage *blurImg2 = 0;

char *window1 = "Original";
char *window2 = "Processed";

IplImage *doPyrDown();
IplImage *preProcessImg();

int main() {
  int i,blurCount;
  camera = cvCaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp");
  if(camera!=NULL) {
    cvNamedWindow(window1,CV_WINDOW_AUTOSIZE);
    cvNamedWindow(window2,CV_WINDOW_AUTOSIZE);
    
    while((origImg=cvQueryFrame(camera)) != NULL) {
      // Handle stack of last 15 images
      if (imgCount < IMG_STACK_LEN) {
	// We do not have all the images, so add to the end of the array imgList
	imgList[imgCount] = preProcessImg(origImg);
	printf("Adding Image Number %d\n",imgCount);
	cvShowImage(window1,origImg);
	cvShowImage(window2,imgList[imgCount]);
	imgCount++;
      } else {
	printf("Replacing image with new one\n");
       	// drop the oldest image, shuffle the rest forward
	//   and add the new one to the end of the list.
	cvReleaseImage(&imgList[0]);
	for (i = 1;i<IMG_STACK_LEN;i++) {
	  imgList[i-1]=imgList[i];
	  imgList[IMG_STACK_LEN-1] = preProcessImg(origImg);
	}
	cvShowImage(window1,origImg);
	cvShowImage(window2,imgList[IMG_STACK_LEN-1]);

	/*
	//Now process each of the images in the stack in turn
	for (i=0;i<IMG_STACK_LEN;i++) {
	  blurImg1 = cvCloneImage(imgList[i]);
	  for(blurCount=0;blurCount<BLUR_LEVEL;blurCount++) {
	    blurImg2 = doPyrDown(blurImg1);
	    fprintf(stderr,"5\n");
	    cvReleaseImage(&blurImg1);
	    blurImg1 = blurImg2;
	  }
	  cvShowImage(window1,imgList[i]);
	  cvShowImage(window2,blurImg1);
	}
	*/
      }
      if(cvWaitKey(1)==(char)27)break;
    }
    
    cvReleaseImage(&origImg);
    
    cvDestroyWindow(window1);
    cvDestroyWindow(window2);
    
    cvReleaseCapture(&camera);
    cvWaitKey(0);
  }
  return 0;
  
}


/* Standard pre-processing done to all images */
IplImage* preProcessImg(IplImage *inImg) {
  IplImage *outImg;
  outImg = cvCreateImage(cvGetSize(inImg),8,1);
  cvCvtColor(inImg,outImg,CV_BGR2GRAY);
  return(outImg);
}

/* Downsamples inImge once, returning the smaller image */
IplImage* doPyrDown(IplImage *inImg) {
  CvSize inSize;
  CvSize outSize;
  IplImage *outImg;
  fprintf(stderr,"doPyrDown\n");
  inSize = cvGetSize(inImg);
  fprintf(stderr,"1\n");
  outSize = cvSize(inSize.width/2,inSize.height/2);
  fprintf(stderr,"2\n");
  outImg = cvCreateImage(outSize,8,1);
  fprintf(stderr,"3\n");
  cvPyrDown(inImg,outImg,CV_GAUSSIAN_5x5);
  fprintf(stderr,"4\n");
  return(outImg);
}
