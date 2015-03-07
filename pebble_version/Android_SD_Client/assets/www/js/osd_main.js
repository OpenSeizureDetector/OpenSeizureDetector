// Request settings from the server.
function get_settings() {
   $.ajax({url:"/settings",success:populate_settings});
};


function populate_settings(dataStr) {
   var dataObj = JSON.parse(dataStr);
   //alert (dataStr);
   var alarmFreqMin = dataObj['alarmFreqMin'];
   var alarmFreqMax = dataObj['alarmFreqMax'];
   var alarmThresh = dataObj['alarmThresh'];
   var alarmRatioThresh = dataObj['alarmRatioThresh'];
   var warnTime = dataObj['warnTime'];
   var alarmTime = dataObj['alarmTime'];
   var nMin = dataObj['nMin'];
   var nMax = dataObj['nMax'];
   var batteryPc = dataObj['batteryPc'];

   var htmlStr = "alarm Freq Min = "+alarmFreqMin+" Hz (bin "+nMin+").<br/>"
              + "alarm Freq Max = "+alarmFreqMax+" Hz (bin "+nMax+").<br/>"
              + "alarm Threshold = "+alarmThresh+"<br/>"
              + "alarm Ratio Threshold = "+alarmRatioThresh+" (/10)<br/>"
              + "warn Time = "+warnTime+" sec. <br/>"
              + "alarm Time = "+alarmTime+" sec. <br/>"
              + "Pebble Battery = "+batteryPc+" %. <br/>"
              ;
   $("#sdSettings").html(htmlStr);

   $("#pebStat3").html("Pebble Battery Charge = "+batteryPc+" %<br/>");

   bgCol = "blue";
   if (batteryPc<30) bgCol = "orange";
   if (batteryPc<20) bgCol = "red";
   $("#pebStat3").css("background-color",bgCol);
}

// Request data from the server.
function get_data() {
   $.ajax({url:"/data",success:process_data});
};

function process_data(dataStr) {
   var dataObj = JSON.parse(dataStr);
   //alert (dataStr);
   var timeStr = dataObj['Time'];
   var maxFreq = dataObj['maxFreq'];
   var maxVal = dataObj['maxVal'];
   var specPow = dataObj['specPower'];
   var roiPow = dataObj['roiPower'];
   var alarmState = dataObj['alarmState'];
   var alarmPhrase = dataObj['alarmPhrase'];
   var pebCon = dataObj['pebCon'];
   var pebAppRun = dataObj['pebAppRun'];
   $("#debugInfo").html(dataStr);
   $("#maxFreq").html("Max Freq = "+maxFreq);
   $("#maxVal").html("Max Val = "+maxVal);
   $("#specPow").html("Spec Pow = "+specPow);
   $("#roiPow").html("ROI Pow = "+roiPow+" - 10xRatio="+Math.round(10*roiPow/specPow));
   $("#alarmState").html("Alarm State = "+alarmState);
   $("#alarmPhrase").html("Alarm Phrase = "+alarmPhrase);

   $("#benStat").html(alarmPhrase+"      -    ("+timeStr+")");

   switch(alarmState) {
       case 0:
           $("#benStat").css("background-color","blue");
           break;
       case 1: 
           $("#benStat").css("background-color","orange");
           if (!sd_muted) jBeep('js/jBeep.wav');
           break;
       case 2:
           $("#benStat").css("background-color","red");
           if (!sd_muted) jBeep('js/jBeep.wav');
           break;
   }

   if (typeof alarmPhrase == 'undefined')
           $("#benStat").css("background-color","orange");

   switch(pebCon) {
      case false:
          $("#pebStat1").html("*** Pebble Watch Not Connected ***")
                      .css("background-color","red");
          break;
      case true:
          $("#pebStat1").html("Pebble Watch Connected OK")
                      .css("background-color","blue");
          break;
   }

   switch(pebAppRun) {
      case false:
          $("#pebStat2").html("*** Pebble Watch App Not Running ***")
                      .css("background-color","red");
          break;
      case true:
          $("#pebStat2").html("Pebble Watch App Running OK")
                      .css("background-color","blue");
          break;
   }
          

};

// Request spectrum from the server.
function get_spectrum() {
   $.ajax({url:"/spectrum",success:process_spectrum});
};

function process_spectrum(dataStr) {
   var dataObj = JSON.parse(dataStr);

   var chartData = {
       labels:["1","2","3","4","5","6","7","8","9","10"],
       datasets: [
         { 
             label:"Spectrum",
             data:dataObj['simpleSpec']
         }
       ]
       };

   var chartWidth = 400;
   var chartHeight = 300;
   var ctx1 = $("#specChart1").get(0).getContext("2d");
   ctx1.canvas.width = chartWidth;
   ctx1.canvas.height = chartHeight;
   var specChart1 = new Chart(ctx1);
    //alert(JSON.stringify(chartData));
    var chartOpts = {
		     animation: false,
		     showScale: true,
		     scaleOverride: true,
		     scaleSteps: 10,
		     scaleStepWidth: 30,
		     scaleStartValue: 0
		     };
    var lineChart1 = new Chart(ctx1).Line(chartData,chartOpts);

   var ctx2 = $("#specChart2").get(0).getContext("2d");
   ctx2.canvas.width = chartWidth;
   ctx2.canvas.height = chartHeight;
   var specChart2 = new Chart(ctx2);
    var chartOpts = {
		     animation: false,
		     showScale: true,
		     scaleOverride: false,
		     };
    var lineChart2 = new Chart(ctx2).Line(chartData,chartOpts);
}


// toggle the global sd_muted flag and change button text.
toggleMute = function() {
   if (sd_muted) {
       sd_muted = 0;
       $("#muteButton").text("Mute Audible Alarm");
   }
   else {
       sd_muted = 1;
       $("#muteButton").text("Un-mute Audible Alarm");
   }
}

$(document).ready(function() {  
   sd_muted = 0;
   get_settings();
   get_data();
   get_spectrum();
   setInterval("get_data();",2000);
   setInterval("get_settings();",10000);
   setInterval("get_spectrum();",5000);
   $("#muteButton").click(toggleMute);
});
