package com.sprout.friendfinder.crypto.protocols;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PSI_C;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

public class CommonFriendsCardinalityTest extends PSI_C {
  
  AuthorizationObject commonFriendsAuth;
  ProtocolCallback callback;
  
  static final String TAG = CommonFriendsCardinalityTest.class.getSimpleName();
  
  public CommonFriendsCardinalityTest(CommunicationService s, AuthorizationObject psicaAuth, 
      ProtocolCallback callback) {
    // check authObject type
    super(s, false);
    if(psicaAuth.getType() != AuthorizationObjectType.PSI_CA) {
      String errorMsg = "auth mismatched";
      Log.e(TAG,  errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    setBenchmark(Config.getBenchmark());
    this.callback = callback;
  }
  
  @Override
  public Integer doInBackground(String... params) {
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
  public void onPostExecute(Integer result) {
    if(result == null) {
      callback.onError(null);
      return;
    }
    Log.i(TAG, "Common friends cardinality protocol complete");
    Log.i(TAG, result +" common friends");
    callback.onComplete(result);
    
    // check if peer wanna run PSI
    // TODO: now just hardcode the policy and policy is the same on both sides and assume PSI always done after PSI-CA
//    boolean runPSIonMySide = (result.size() > 0);
//    boolean runPSIonHis;
//    byte[] write = new byte[1];
//    write[0] = (byte) (runPSIonMySide ? 1 : 0);
//    comService.write(write);
//    byte[] read = comService.read();
//    if(read.length != 1) {
//      Log.i(TAG, "error reading");
//      callback.onError();
//    }
//    runPSIonHis = (read[0] == 1) ? true : false;
//    
//    if(runPSIonHis && runPSIonMySide) {
//      (new CommonFriendsTest(comService, commonFriendsAuth, callback, interaction)).execute(commonFriendsParam);
//    } else {
//    
//      // fail to pass PSICA - exit
//      Log.i(TAG, "I want to run PSI: "+runPSIonMySide+" , peer wants to run PSI: "+runPSIonHis);
//      callback.onError();
//    }
  }
}
