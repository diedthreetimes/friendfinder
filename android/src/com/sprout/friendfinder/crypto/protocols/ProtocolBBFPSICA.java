package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.crypto.protocols.bearercap.BBFPSICA;

import android.util.Log;

public class ProtocolBBFPSICA implements Protocol {

  static final String TAG = ProtocolBBFPSICA.class.getSimpleName();

  @Override
  public AuthorizationObjectType getAuthType() {
    return AuthorizationObjectType.B_PSI_CA;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {
    new BBFPSICATest(cs, auth, callback).execute();
  }
  
  private int mNumTrials = 1;
  private boolean mBenchmark = Config.getBenchmark();

  @Override
  public void config(int numTrials, boolean benchmark) {
    this.mNumTrials = numTrials;
    this.mBenchmark = benchmark;
    
  }

  private class BBFPSICATest extends BBFPSICA {
    ProtocolCallback callback;

    public BBFPSICATest(CommunicationService s,
        AuthorizationObject bpsiCaAuth, ProtocolCallback cb) {
      super(s, bpsiCaAuth.getData());
      if(bpsiCaAuth.getType() != AuthorizationObjectType.B_PSI_CA) {
        String errorMsg = "auth mismatched";
        Log.e(TAG,  errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      setBenchmark(mBenchmark);
      setNumTrials(mNumTrials);
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
      callback.onComplete(new ProtocolResult(onlineWatch.getElapsedTime(), result.size(), mNumTrials));
    }

  }

}
