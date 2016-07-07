package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

public class CommonFriendsBearerCardinalityTest extends BPSICA {

  static final String TAG = CommonFriendsBearerCardinalityTest.class.getSimpleName();
  ProtocolCallback callback;
  boolean testAppFlag = false;

  public CommonFriendsBearerCardinalityTest(CommunicationService s,
      AuthorizationObject bpsiCaAuth, ProtocolCallback cb, boolean testApp) {
    this(s, bpsiCaAuth, cb);
    
    setBenchmark(testApp);
    testAppFlag = testApp;
  }

  public CommonFriendsBearerCardinalityTest(CommunicationService s,
      AuthorizationObject bpsiCaAuth, ProtocolCallback cb) {
    super(s, bpsiCaAuth.getData());
    if(bpsiCaAuth.getType() != AuthorizationObjectType.B_PSI_CA) {
      String errorMsg = "auth mismatched";
      Log.e(TAG,  errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    setBenchmark(Config.getBenchmark());
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
    if(testAppFlag) {
      callback.onComplete(getTotalOnlineTime());
      return;
    }
    if(result == null) {
      callback.onError(null);
      return;
    } 
    Log.i(TAG, "Common friends: " + result.size());
    callback.onComplete(result.size());
  }

}
