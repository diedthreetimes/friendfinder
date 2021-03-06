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

import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

public class AuthorizationDownloader {

  private static final String TAG = AuthorizationDownloader.class.getSimpleName();
  private static final boolean D = true;
  
  public static AuthorizationObject download(Context context, AuthorizationObjectType type, HttpGet httpGet) 
      throws ClientProtocolException, IOException, CertificateException, JSONException {

    DefaultHttpClient httpClient = new DefaultHttpClient();
    
    if(D) Log.d(TAG, "Attempting a get of " + httpGet.getURI());
    
    HttpResponse httpResponse = httpClient.execute(httpGet);
    
    BufferedReader bufReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

    StringBuffer buffer = new StringBuffer();
    String line = bufReader.readLine();

    if(D) Log.d(TAG, "Processing Response");
    while (line != null) {
      buffer.append(line + "\n");
      line = bufReader.readLine();
    }

    String result = buffer.toString(); 
    bufReader.close(); 
    
    return new AuthorizationObject(context, result, type);
  }

  public static AuthorizationObject downloadTest(Context context, String token, String secret, AuthorizationObjectType type, int numFriends)
      throws ClientProtocolException, IOException, JSONException, CertificateException {

    HttpGet httpGet = new HttpGet("http://"+Config.getHost()+Config.getContactJsonUrl(true) +
            "?include_connections=true" +
            "&protocol=" + type.name().toLowerCase() +
            "&num_friends=" + numFriends
            );
    
    return download(context, type, httpGet);
  }
  
  public static AuthorizationObject download(Context context, String token, String secret, AuthorizationObjectType type)
      throws ClientProtocolException, IOException, JSONException, CertificateException {

    HttpGet httpGet = new HttpGet("http://"+Config.getHost()+Config.getContactJsonUrl() +
    		"?oauth_access_token=" + token + // 694d1dd7-6feb-4538-bb13-44497a3d778f
    		"&oauth_secret=" + secret + // fa4d85f2-0d0d-4c91-90eb-d8544e26f83b
    		"&include_connections=true" +
    		"&protocol=" + type.name().toLowerCase()
    		);
    
    return download(context, type, httpGet);
  }

}
