package com.sprout.friendfinder.crypto.protocols;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.crypto.protocols.bearercap.BPSI;

import android.util.Log;

public class ProtocolBPSI implements Protocol {

  static final String TAG = ProtocolBPSI.class.getSimpleName();

  @Override
  public AuthorizationObjectType getAuthType() {
    return AuthorizationObjectType.B_PSI_CA;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {

    List<BigInteger> cap = auth.getData();
    List<String> friends = auth.getOriginalOrder();
    Map<BigInteger, String> capMap = new HashMap<BigInteger, String>();
    for( int i=0; i < cap.size(); i++) {
      capMap.put(cap.get(i), friends.get(i));
    }
    
    new BPSITest(cs, capMap, callback).execute();
  }
  
  private int mNumTrials = 1;
  private boolean mBenchmark = Config.getBenchmark();

  @Override
  public void config(int numTrials, boolean benchmark) {
    this.mNumTrials = numTrials;
    this.mBenchmark = benchmark;
    
  }

  private class BPSITest extends BPSI {
    ProtocolCallback callback;
    
    public BPSITest(CommunicationService s, Map<BigInteger, String> capMap, ProtocolCallback cb) {
      super(s, capMap);

      setBenchmark(mBenchmark);
      setNumTrials(mNumTrials);
      callback = cb;
    }
    
    @Override
    public List<String> doInBackground(String... params) {
      try {
        return super.doInBackground(params);
      } catch (Exception e) {
        Log.e(TAG, "BPSITest failed, ", e);
        return null;
      }
    }

    @Override
    public void onPreExecute() {
      Log.i(TAG, "About to execute BPSITest");
    }

    @Override
    public void onPostExecute(List<String> result) {
      if(result == null) {
        callback.onError(null);
        return;
      } 
      Log.i(TAG, "Common friends: " + result.size());
      callback.onComplete(new ProtocolResult(onlineWatch.getElapsedTime(), result, mNumTrials));
    }

  }
}
