package com.sprout.friendfinder.models;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

import com.activeandroid.serializer.TypeSerializer;


public class StringListSerializer extends TypeSerializer {

  public static String TAG = StringListSerializer.class.getSimpleName();
  
  @Override
  public Class<?> getDeserializedType() {
    return ArrayList.class;
  }

  @Override
  public Class<?> getSerializedType() {
    return String.class;
  }

  @SuppressWarnings("unchecked")
  @Override
  public String serialize(Object data) {
    if (data == null) {
      return null;
    }
    
    JSONArray jsonArray = new JSONArray();
    
    for (String s : ((ArrayList<String>) data)) {
      jsonArray.put(s);
    }
    
    return jsonArray.toString();
  }

  public ArrayList<String> deserialize(Object data) {
    if (data == null) {
      return null;
    }
    
    try {
      JSONArray json = new JSONArray((String) data);
      
      ArrayList<String> res = new ArrayList<String>();
      for (int i=0; i<json.length(); i++) {
        res.add(json.getString(i));
      }
      
      return res;
    } catch (JSONException e) {
      Log.e(TAG, "Unable to serialize stored json", e);
    }
    
    return null;
  }
}
