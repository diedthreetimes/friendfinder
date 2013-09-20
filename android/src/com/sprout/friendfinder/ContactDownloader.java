package com.sprout.friendfinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.brickred.socialauth.util.Base64.InputStream;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;

public class ContactDownloader extends AsyncTask {

	@Override
	protected Object doInBackground(Object... arg0) {
		// TODO Auto-generated method stub
		
		BufferedReader bufReader = null;
		
		
		
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet("http://10.0.2.2:3000/authority/download_profile.json"); //http://athena.ics.uci.edu:3000/authority/download_profile.json
			HttpResponse httpResponse = httpClient.execute(httpGet);
			
			
			
			/*
			Header[] header = httpResponse.getAllHeaders();
			for (int i = 0; i < header.length; i++) {
				Log.d("header", header[i].toString());
			}
			*/
			
			
		
			
			
					/*
			
			bufReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		
			
			StringBuffer buffer = new StringBuffer();
			String line = "";
			
			while ((line = bufReader.readLine()) != null) {
				buffer.append(line + "\n");
			}
			
			String result = buffer.toString(); 
			
			Log.d("result", result);
			
			bufReader.close();  */
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufReader != null) {
				try {
					bufReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		return null;
	}

}
