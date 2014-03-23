/**
 *  BenFinder javascript.
 *
 * App modes (BF.mode) are:
 *   0 - multi image view.
 *   1 - single image view.
 *
 * In Single image view, BF.imgNo defines the image to show
 *   0 - raw
 *   1 - masked
 *   2 - chart
 *   3 - webcam
*/

BF = [];  // Namespace for the BenFinder app data

BF.modes = {0:"Multi Image",
	    1:"Single Image"
	    };

// Info on the images to be updated.  Each entry is:
//   [Text Description, URL, ID of div to display image]
BF.images = {0:["raw","/raw_img.png","#rawImg"],
	     1:["masked","/masked_img.png","#maskedImg"],
	     2:["chart","/masked_img.png","#chartImg"],
	     3:["webcam","http://guest:guest@192.168.1.24/tmpfs/auto.jpg","#webCamImg"]
	     };

BF.timerPeriod = 2000;  // ms
BF.timerID = -1;


$(document).ready(function() {
    BF.mode = 0;  // Application mode.
    BF.imgNo = 0; // Image to display (in single image mode)
    BF.setMode(0,0)
    //alert("BF.mode="+BF.mode+" = "+BF.modes[BF.mode]);
    BF.startTimer(BF.timerPeriod);
});


BF.startTimer = function(periodms) {
    BF.stopTimer();
    BF.timerID = setInterval(BF.updateImages,periodms);
};

BF.stopTimer = function() {
   if (BF.timerID >= 0) {
	alert("stopping timer");
	clearInterval(BF.timerID);
   }
}

BF.setMode = function(mode,imgNo) {
    //alert("BF.setMode("+mode+")");
    BF.mode = mode;
    BF.imgNo = imgNo;
};

BF.updateImages = function() {

    if (BF.mode == 0) {
	//alert("mode 0");
	//BF.updateImage(0,BF.images[0][2]);
	//BF.updateImage(1,BF.images[1][2]);
	//BF.updateImage(2,BF.images[2][2]);
	BF.updateImage(3,BF.images[3][2]);
    }
    else {
	//alert("mode 1 - BF.imgNo="+BF.imgNo);
	BF.updateImage(BF.imgNo,"imgDiv");	
    }
};

BF.updateImage = function(imgNo,imgID) {
    url = BF.images[imgNo][1]+ "?" + (new Date()).getTime();;
    imgID = BF.images[imgNo][2];
    //alert("url="+url+" id="+imgID);
    $(imgID).attr("src",url);

};
