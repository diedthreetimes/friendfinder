package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.ATWPSICAProtocol;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

import android.util.Log;

public class ProtocolATWPSICA_NEW implements Protocol{
  
  static final String TAG = ProtocolATWPSICA_NEW.class.getSimpleName();

  @Override
  public AuthorizationObjectType getAuthType() {
    // TODO Auto-generated method stub
    return AuthorizationObjectType.PSI_CA;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {

    new ATWPSICA_NEWTest(cs, auth, callback).execute();
    
  }
  
  private int mNumTrials = 1;
  private boolean mBenchmark = Config.getBenchmark();

  @Override
  public void config(int numTrials, boolean benchmark) {
    this.mNumTrials = numTrials;
    this.mBenchmark = benchmark;
    
  }

  private class ATWPSICA_NEWTest extends ATWPSICAProtocol {
    
    ProtocolCallback callback;
    
    public ATWPSICA_NEWTest(CommunicationService s, AuthorizationObject psicaAuth, 
        ProtocolCallback callback) {
      // check authObject type
      super(s, psicaAuth);
      if(psicaAuth.getType() != AuthorizationObjectType.PSI_CA) {
        String errorMsg = "auth mismatched";
        Log.e(TAG,  errorMsg);
        throw new IllegalArgumentException(errorMsg);
      }

      setBenchmark(mBenchmark);
      setNumTrials(mNumTrials);
      this.callback = callback;
    }
    
    @Override
    public List<String> doInBackground(String... params) {
      try {
        return super.doInBackground(params);
      } catch (Exception e) {
        Log.e(TAG, "ATWPSICATest failed, ", e);
        return null;
      }
    }

    @Override
    public void onPreExecute() {
      Log.i(TAG, "About to execute ATWPSICATest");
    }

    @Override
    public void onPostExecute(List<String> result) {
      if(result == null) {
        callback.onError(null);
        return;
      }
      Log.i(TAG, "ATWPSICATest complete");
      Log.i(TAG, result.size() +" common friends");
      
      callback.onComplete(new ProtocolResult(onlineWatch.getElapsedTime(), result.size(), mNumTrials));
    }
  }
}
