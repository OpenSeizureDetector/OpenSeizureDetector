/**
 * 
 */
package uk.org.openseizuredetector.bentv;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * @author graham
 *
 */
public class ImageHandler extends AsyncTask<Object, String, Boolean> {
   ImageView imView;

   @Override
   protected Boolean doInBackground(Object... params) {
	// TODO Auto-generated method stub
	String imgURLStr = (String) params[0];
	imView = (ImageView) params[1];
	try {
		URL imgURL = new URL(imgURLStr);
		imgURL.openStream();
		// TODO - read image from stream.
		publishProgress();
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return(false);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return(false);
	}
	
	return(true);
   }
   
   @Override
   protected void onProgressUpdate(String...bm) {
	   //imView.setImageBitmap((Bitmap)bm[0])
   }
}
