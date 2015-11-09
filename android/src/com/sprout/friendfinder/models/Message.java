package com.sprout.friendfinder.models;

import java.io.Serializable;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "message")
public class Message extends Model implements Serializable {

  private static final long serialVersionUID = -3079371663153791650L;
  
  public Message() {
    super();
  }
  
  public Message(String otherMessage) throws JSONException {
    super();
    
    JSONObject jsonObject = new JSONObject(otherMessage);
    address = jsonObject.getString("address");
    content = jsonObject.getString("content");
    owner = jsonObject.getBoolean("owner");
    sent = jsonObject.getBoolean("sent");
    timestamp = jsonObject.getLong("timestamp");
  }

  public Message(String addr, String cont) {
    super();
    
    address = addr;
    content = cont;
    owner = true;
    sent = false;
    timestamp = Calendar.getInstance().getTimeInMillis();
  }
  
  public Message(Message other, String otherAddr) {
    super();
    
    address = otherAddr;
    content = other.content;
    owner = false;
    sent = true;
    timestamp = other.timestamp;
  }

  @Column
  public String address; // the other side's address - could be id or maybe interaction id?

  @Column
  public String content;

  @Column
  public boolean owner; // true if its yours, false if its not.

  @Column
  public boolean sent;
  
  @Column
  public long timestamp;
  
  public String toJsonString() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("address", address);
      jsonObject.put("content", content);
      jsonObject.put("owner", owner);
      jsonObject.put("sent", sent);
      jsonObject.put("timestamp", timestamp);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    return jsonObject.toString();
  }

}
