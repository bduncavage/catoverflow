package org.duncavage.catoverflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;
import android.util.Log;

public class CatTransporter {
	
	private static final String TAG = "CatTransporter";
	
	public static Vector<String> beamCats(String url, String currentVersion)
	{
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(url); 

	    HttpResponse response;
	    try {
	        response = httpClient.execute(httpGet);

	        HttpEntity entity = response.getEntity();
	        Header eTag = response.getFirstHeader("ETag");
	        
	        if(currentVersion.equals(eTag.getValue())) {
	        	// no new cats!
	        	return null;
	        }
	        
	        if (entity != null) {
	            InputStream instream = entity.getContent();
	            Vector<String> cats = convertStreamToCats(instream);
	            cats.add(0, eTag.getValue()); // put the version in the first element
	            instream.close();
	            return cats;
	        } else {
	        	return null;
	        }
	    } catch (Exception e) {
	    	Log.e(TAG, "Exception when beaming cats! Oh noes!: " + e.getLocalizedMessage());
	    	return null;
	    }
	}

	private static Vector<String> convertStreamToCats(InputStream is) {

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    Vector<String> cats = new Vector<String>();
	    
	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            cats.add(line);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return cats;
	}
	
	public static void adoptCat(String catUrl, File catStore) {
		HttpClient httpClient = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(catUrl); 

	    HttpResponse response;
	    try {
	        response = httpClient.execute(httpGet);

	        HttpEntity entity = response.getEntity();
	        
	        if(entity != null) {
	        	String catName = Uri.parse(catUrl).getLastPathSegment();
	        	FileOutputStream fos = new FileOutputStream(new File(catStore.getAbsolutePath() + catName));
	        	InputStream is = entity.getContent();
	        	
	        	byte[] buffer = new byte[4096];
	            int bytesRead = 0;
	            while((bytesRead = is.read(buffer)) > 0 ) {
	                 fos.write(buffer, 0, bytesRead);
	            }
	            fos.close();
	        }
	    } catch(Exception e) {
	    	
	    }
	}
}
