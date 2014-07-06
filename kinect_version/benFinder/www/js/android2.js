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
	     3:["webcam","/getCamImg","#webCamImg"]
	     };

BF.statusStrs=['OK','Warning','ALARM!!!!','Not Found']


BF.timerPeriod = 2000;  // ms
BF.timerID = -1;

BF.audibleAlarm = true;


$(document).ready(function() {
    //w = window.open(BF.images[3][1],null);


    BF.mode = 0;  // Application mode.
    BF.imgNo = 0; // Image to display (in single image mode)
    BF.setMode(0,0);  // Set to multi-image mode first.
    BF.updateImages();
    //alert("BF.mode="+BF.mode+" = "+BF.modes[BF.mode]);
    BF.startTimer(BF.timerPeriod);
    $(".moveCamLink").click(BF.moveCam);
    $(".saveBgImgLink").buttonMarkup({ inline: true, mini:true });;
    $(".moveCamLink").buttonMarkup({ inline: true, mini:true });;
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
    // Update the images and seizure detector output by reloading them 
    // from the network.

    BF.getSeizureData();
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

BF.getSeizureData = function() {
    $.ajax({url:"/jsonData"}).done(function(dataStr) {
	data = JSON.parse(dataStr);
	$("#summaryData").html(
		"Rate="+data['rate']+" bpm"
	    + " - " + BF.statusStrs[data['status']]+".");
	$("#summaryData").css("font-size","xx-large");
	$("#summaryData").css("font-weight","bold");

	switch (data['status']) {
	case 0:  // ok
	    $("#summaryData").css("background-color","blue");
	    $("#summaryData").css("color","white");
	    break;
	case 1:  // warning
	    $("#summaryData").css("background-color","orange");
	    $("#summaryData").css("color","black");
	    break;
	case 2: // alarm
	    $("#summaryData").css("background-color","red");	    
	    $("#summaryData").css("color","white");
	    if (BF.audibleAlarm) jBeep();
	    break;
	case 3: // not found
	    $("#summaryData").css("background-color","gray");	    	    
	    $("#summaryData").css("color","black");
	    break;
	    
	default:
	}

	
    });
    //            "Summary Data <br>"
    //		+"Status="+data['status']+" - "+
    //		BF.statusStrs[data['status']]+"<br>"
    //		+"Rate="+data['rate']+" bpm <br>"
    //		+"Brightness="+data['bri']+"<br>"
    //		+"Area="+data['area']);
};

BF.updateImage = function(imgNo,imgID) {
    // Update an image - imageNo is the entry in the BF.images[] array
    // imgId is the ID of the image element to update.
    url = BF.images[imgNo][1]+ "?" + (new Date()).getTime();;
    //imgID = BF.images[imgNo][2];
    //alert("url="+url+" id="+imgID);
    $(imgID).attr("src",url);
};

BF.moveCam = function(e) {
    // move the video camera to the position number given in the 'value'
    // attribute of the clicked link.
    pos =$(e.target).attr("value");
    urlStr = "/moveCamera?pos="+pos;
    //alert(urlStr);
    $.ajax({url:urlStr}).done(function(dataStr) {
	//alert(dataStr);
    });
};
