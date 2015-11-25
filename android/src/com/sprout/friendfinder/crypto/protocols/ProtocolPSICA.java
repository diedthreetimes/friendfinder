package com.sprout.friendfinder.crypto.protocols;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PSI_C;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

import android.util.Log;

public class ProtocolPSICA implements Protocol {
  
  String[] contactList;
  

  static final String TAG = CommonFriendsCardinalityTest.class.getSimpleName();
  
  public ProtocolPSICA(String[] input) {
    contactList = new String[input.length];
    System.arraycopy(input, 0, contactList, 0, input.length);
  }

  @Override
  public AuthorizationObjectType getAuthType() {
    // weird if we dont dl anything it wont let me past in ProtocolTestService...
    // put whatever here
    return AuthorizationObjectType.PSI_CA;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {
    // TODO Auto-generated method stub
    Log.i(TAG, "Running test");

    new PSICATest(cs, callback).execute(contactList);
  }
  
  private int mNumTrials = 1;
  private boolean mBenchmark = Config.getBenchmark();

  @Override
  public void config(int numTrials, boolean benchmark) {
    this.mNumTrials = numTrials;
    this.mBenchmark = benchmark;
    
  }
  
  private class PSICATest extends PSI_C {
    
    ProtocolCallback callback;
    
    
    public PSICATest(CommunicationService cs, ProtocolCallback cb) {
      super(cs, true);

      callback = cb;
      setBenchmark(mBenchmark);
      setNumTrials(mNumTrials);
    }
    
    @Override
    public Integer doInBackground(String... params) {
      try {
        return super.doInBackground(params);
      } catch (Exception e) {
        Log.e(TAG, "PSICATest failed, ", e);
        return null;
      }
    }

    @Override
    public void onPreExecute() {
      Log.i(TAG, "About to execute the PSICATest");
    }

    @Override
    public void onPostExecute(Integer result) {
      if(result == null) {
        callback.onError(null);
        return;
      }
      Log.i(TAG, "PSICATest complete");
      Log.i(TAG, result +" common friends");
      callback.onComplete(new ProtocolResult(onlineWatch.getElapsedTime(), result, mNumTrials));
    }
  }

}
