
function get_filelist() {
    //alert("get_filelist()");
    $.ajax({url:"/logs",success:populate_filelist});
};


function populate_filelist(dataStr) {
   var dataObj = JSON.parse(dataStr);
   //alert (dataStr);
   $("#logfilelist").append("<h2>Available Log Files</h2>");
   $("#logfilelist").append("<ul>");

   $.each(dataObj['logFileList'],function(index,value) {
       $("#logfilelist").append('<li><a href="/logs/'+value+'">'+value+'</a></li>');
   });
   $("#logfilelist").append("</uk>");


}



$(document).delegate("#logs","pageinit", function() {  
    //alert("logs page opened");
    get_filelist();
});
