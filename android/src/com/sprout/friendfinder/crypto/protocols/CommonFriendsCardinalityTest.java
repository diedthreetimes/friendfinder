package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.ATWPSICAProtocol;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.models.Interaction;

public class CommonFriendsCardinalityTest extends ATWPSICAProtocol {
  
  String[] commonFriendsParam;
  AuthorizationObject commonFriendsAuth;
  ProfileDownloadCallback callback;
  Interaction interaction;
  CommunicationService comService;
  
  static final String TAG = CommonFriendsCardinalityTest.class.getSimpleName();
  
  public CommonFriendsCardinalityTest(CommunicationService s, AuthorizationObject psicaAuth, 
      AuthorizationObject psiAuth, ProfileDownloadCallback callback,
      Interaction interaction, String... commonFriendsParam) {
    // check authObject type
    super(s, psicaAuth);
    if(psicaAuth.getType() != AuthorizationObjectType.PSI_CA && psiAuth.getType() != AuthorizationObjectType.PSI) {
      String errorMsg = "auth mismatched";
      Log.e(TAG,  errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    setBenchmark(Config.getBenchmark());
    this.commonFriendsParam = commonFriendsParam;
    this.commonFriendsAuth = psiAuth;
    this.callback = callback;
    this.interaction = interaction;
    this.comService = s;
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
    Log.i(TAG, "Common friends cardinality protocol complete");
    Log.i(TAG, result.size()+" common friends");
    
    // check if peer wanna run PSI
    // TODO: now just hardcode the policy and policy is the same on both sides and assume PSI always done after PSI-CA
    boolean runPSIonMySide = (result.size() > 0);
    boolean runPSIonHis;
    byte[] write = new byte[1];
    write[0] = (byte) (runPSIonMySide ? 1 : 0);
    comService.write(write);
    byte[] read = comService.read();
    if(read.length != 1) {
      Log.i(TAG, "error reading");
      callback.onError();
    }
    runPSIonHis = (read[0] == 1) ? true : false;
    
    if(runPSIonHis && runPSIonMySide) {
      (new CommonFriendsTest(comService, commonFriendsAuth, callback, interaction)).execute(commonFriendsParam);
    } else {
    
      // fail to pass PSICA - exit
      Log.i(TAG, "I want to run PSI: "+runPSIonMySide+" , peer wants to run PSI: "+runPSIonHis);
      callback.onError();
    }
  }
}
