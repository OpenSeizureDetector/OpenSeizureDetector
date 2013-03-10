/*#include <highgui.h>*/

/*int main (int argc, char** argv) {
  IplImage* img = cvLoadImage(argv[1],CV_LOAD_IMAGE_UNCHANGED);
  cvNamedWindow("Seizure_Detector",CV_WINDOW_AUTOSIZE);
  cvShowImage("Seizure_Detector",img);
  cvWaitKey(0);
  cvReleaseImage(&img);
  cvDestroyWindow("Seizure_Detector");
			      
}
*/


#include <opencv2/imgproc/imgproc_c.h>
#include <highgui.h>

CvCapture *camera = NULL;
CvMemStorage *memstore = NULL;
CvSeq *seq = NULL;

IplImage *origImg = 0;
IplImage *procImg = 0;
IplImage *smoothImg = 0;
IplImage *edgeImg = 0;

char *window1 = "Original";
char *window2 = "Processed";
char *window3 = "Smoothed";
int main()

{
    camera = cvCaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp");
    if(camera!=NULL)
    {
    cvNamedWindow(window1,CV_WINDOW_AUTOSIZE);
    cvNamedWindow(window2,CV_WINDOW_AUTOSIZE);
    memstore = cvCreateMemStorage(0);

    while((origImg=cvQueryFrame(camera)) != NULL) {
      procImg = cvCreateImage(cvGetSize(origImg),8,1);
      cvCvtColor(origImg,procImg,CV_BGR2GRAY);
      //cvSmooth(procImg, procImg, CV_GAUSSIAN_5x5,9,9,0,0);
      smoothImg = cvCreateImage(cvGetSize(origImg),8,1);
      cvSmooth(procImg, smoothImg, CV_GAUSSIAN,9,9,0,0);
      cvCanny(smoothImg,procImg,0,20,3);
      
      cvShowImage(window1,origImg);
      cvShowImage(window2,procImg);
      //cvShowImage(window3,smoothImg);

      if(cvWaitKey(1)==(char)27)break;

    }

    cvReleaseImage(&procImg);
    cvReleaseImage(&origImg);
    cvReleaseImage(&smoothImg);

    cvReleaseMemStorage(&memstore);
    cvDestroyWindow(window1);
    cvDestroyWindow(window2);
    cvDestroyWindow(window3);


    cvReleaseCapture(&camera);
    cvWaitKey(0);
    }


  return 0;//bo jestem miły dla systemu i informuję go o braku błędów

}
