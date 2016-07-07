package com.sprout.friendfinder.crypto.protocols;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class ProtocolResult {

  Integer numCommonFriends = null;
  List<String> commonFriends = null;
  Long onlineTime = null;
  Integer numTrials = null;
  // TODO: bandwidth
  
  public ProtocolResult(long oT, int cardinality, int nt) {
    onlineTime = oT/nt;
    numCommonFriends = cardinality;
    numTrials = nt;
  }
  
  public ProtocolResult(long oT, List<String> cf, int nt) {
    onlineTime = oT/nt;
    commonFriends = new ArrayList<String>(cf);
    numTrials = nt;
  }
  
  public ProtocolResult(long oT, int cardinality) {
    onlineTime = oT;
    numCommonFriends = cardinality;
  }
  
  public ProtocolResult(long oT, List<String> cf) {
    onlineTime = oT;
    commonFriends = new ArrayList<String>(cf);
  }
  
  public ProtocolResult(int cardinality) {
    numCommonFriends = cardinality;
  }
  
  public ProtocolResult(List<String> cf) {
    commonFriends = new ArrayList<String>(cf);
  }
  
  public List<String> getCommonFriends() {
    return commonFriends;
  }
  
  @Override
  public String toString() {
    String res = "";
    if(numTrials != null) {
      res += "Number of trials: "+numTrials+"\n";
    }
    if(onlineTime != null) {
      res += "Total time: "+onlineTime+" ms \n";
    }
    if(numCommonFriends != null) {
      res += "Num Common Friends: "+numCommonFriends+"\n";
    }
    if(commonFriends != null) {
      String cfString = TextUtils.join(",", commonFriends);
      res += "Common Friends: "+cfString+"\n";
    }
    return res;
  }
  
}
