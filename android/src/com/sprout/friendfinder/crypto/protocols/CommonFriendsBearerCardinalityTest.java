package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.crypto.ATWBearerPSICA;
import com.sprout.friendfinder.crypto.AuthorizationObject;

public class CommonFriendsBearerCardinalityTest extends ATWBearerPSICA {

  static final String TAG = CommonFriendsBearerCardinalityTest.class.getSimpleName();
  ProfileDownloadCallback callback;

  public CommonFriendsBearerCardinalityTest(CommunicationService s,
      AuthorizationObject bpsiCaAuth, ProfileDownloadCallback cb) {
    super(s, bpsiCaAuth.getData());
    
    callback = cb;
  }
  
  @Override
  public List<String> doInBackground(String... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "CommonFriendsCardinalityProtocol failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the Common friends cardinality protocol");
  }

  @Override
  public void onPostExecute(List<String> result) {
    if(result == null) {
      callback.onError();
      return;
    } 
    Log.i(TAG, "Common friends: " + result.size());
    callback.onComplete();
  }

}
