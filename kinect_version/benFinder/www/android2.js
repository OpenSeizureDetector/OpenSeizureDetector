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
BF.images = {0:["raw","/rawImg","#rawImg"],
	     1:["masked","/maskedImg","#maskedImg"],
	     2:["chart","/chartImg","#chartImg"],
	     3:["webcam","http://guest:guest@192.168.1.24/tmpfs/auto.jpg","#webCamImg"]
	     };

BF.timerPeriod = 2000;  // ms
BF.timerID = -1;


$(document).ready(function() {
    BF.mode = 0;  // Application mode.
    BF.imgNo = 0; // Image to display (in single image mode)
    BF.setMode(0,0);  // Set to multi-image mode first.
    BF.updateImages();
    //alert("BF.mode="+BF.mode+" = "+BF.modes[BF.mode]);
    BF.startTimer(BF.timerPeriod);
    $(".rawImgLink").click(function() {BF.setMode(1,0);});
    $(".maskedImgLink").click(function() {BF.setMode(1,1);});
    $(".chartImgLink").click(function() {BF.setMode(1,2);});
    $(".webCamLink").click(function() {BF.setMode(1,3);});
    $(".homeLink").click(function() {BF.setMode(0,0);});

    // There are lots of ways of getting back to the home page, so 
    // capture them here, just in case they are missed.
    $( document ).on( "pageshow", function( event, data ){
	if (event.target.id=="home") {
	    //alert("returning to multi-image mode")
	    BF.setMode(0,0);
	}
    });
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
    // Change the mode of the application
    // 0 = multi image view
    // 1 = single image view
    //alert("BF.setMode("+mode+","+imgNo+")");
    BF.mode = mode;
    BF.imgNo = imgNo;
    if (BF.mode==0) {
	$( ":mobile-pagecontainer" ).pagecontainer( "change", "#summaryData");
    } else {
	$( ":mobile-pagecontainer" ).pagecontainer( "change", "#singleImg");
    }
};


BF.updateImages = function() {
    // Update the images by reloading them from the network.
    // Which images to update depends on the application mode.
    if (BF.mode == 0) {
	//alert("mode 0");
	BF.updateImage(0,BF.images[0][2]);
	BF.updateImage(1,BF.images[1][2]);
	BF.updateImage(2,BF.images[2][2]);
	BF.updateImage(3,BF.images[3][2]);
    }
    else {
	//alert("mode 1 - BF.imgNo="+BF.imgNo);
	BF.updateImage(BF.imgNo,"#imgDiv");	
    }
};

BF.updateImage = function(imgNo,imgID) {
    // Update an image - imageNo is the entry in the BF.images[] array
    // imgId is the ID of the image element to update.
    url = BF.images[imgNo][1]+ "?" + (new Date()).getTime();;
    //imgID = BF.images[imgNo][2];
    //alert("url="+url+" id="+imgID);
    $(imgID).attr("src",url);
};
