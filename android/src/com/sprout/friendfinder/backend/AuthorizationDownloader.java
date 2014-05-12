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

  private static final String host = "creepyer.ics.uci.edu:3000";
  
  private static final String TAG = AuthorizationDownloader.class.getSimpleName();
  private static final boolean D = true;
  
  public static AuthorizationObject download(Context context, String token, String verifier) throws ClientProtocolException, IOException, JSONException, CertificateException {

    

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet("http://"+host+"/authority/download_connections.json?" +
    		"oauth_token=" + token +  // 75--86c42b15-e505-4c09-a35d-48ea4cf07934
    		"&oauth_verifier=" + verifier // 79135
    		);
    
    if(D) Log.d(TAG, "Attempting a get of " + httpGet.getURI());
    
    HttpResponse httpResponse = httpClient.execute(httpGet);

    BufferedReader bufReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));


    StringBuffer buffer = new StringBuffer();
    String line = "";

    while ((line = bufReader.readLine()) != null) {
      buffer.append(line + "\n");
    }

    String result = buffer.toString(); 
    bufReader.close(); 
    
    return new AuthorizationObject(context, result);

  }

}
