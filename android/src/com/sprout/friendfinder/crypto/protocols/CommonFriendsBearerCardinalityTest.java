package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.ATWBearerPSICA;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

public class CommonFriendsBearerCardinalityTest extends ATWBearerPSICA {

  static final String TAG = CommonFriendsBearerCardinalityTest.class.getSimpleName();
  ProtocolCallback callback;

  public CommonFriendsBearerCardinalityTest(CommunicationService s,
      AuthorizationObject bpsiCaAuth, ProtocolCallback cb) {
    super(s, bpsiCaAuth.getData());
    if(bpsiCaAuth.getType() != AuthorizationObjectType.B_PSI_CA) {
      String errorMsg = "auth mismatched";
      Log.e(TAG,  errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
    
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
      callback.onError(null);
      return;
    } 
    Log.i(TAG, "Common friends: " + result.size());
    callback.onComplete(null);
  }

}
