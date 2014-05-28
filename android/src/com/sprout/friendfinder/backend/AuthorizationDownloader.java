package com.sprout.friendfinder.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import android.content.Context;
import android.util.Log;

import com.sprout.friendfinder.crypto.AuthorizationObject;

public class AuthorizationDownloader {

  private static final String host = "344ee97c.ngrok.com:80";
  
  private static final String TAG = AuthorizationDownloader.class.getSimpleName();
  private static final boolean D = true;
  
  public static AuthorizationObject download(Context context, String token, String secret) throws ClientProtocolException, IOException, JSONException, CertificateException {

    

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet("http://"+host+"/authority/download_connections.json?" +
    		"oauth_access_token=" + token + // 694d1dd7-6feb-4538-bb13-44497a3d778f
    		"&oauth_secret=" + secret + // fa4d85f2-0d0d-4c91-90eb-d8544e26f83b
    		"&include_connections=true"
    		);
    
    if(D) Log.d(TAG, "Attempting a get of " + httpGet.getURI());
    
    HttpResponse httpResponse = httpClient.execute(httpGet);
    
    BufferedReader bufReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

    StringBuffer buffer = new StringBuffer();
    String line = bufReader.readLine();

    if(D) Log.d(TAG, "Processing Response");
    while (line != null) {
      buffer.append(line + "\n");
      line = bufReader.readLine(); // TODO: Why is this not reading anymore
    }

    String result = buffer.toString(); 
    bufReader.close(); 
    
    return new AuthorizationObject(context, result);

  }

}
