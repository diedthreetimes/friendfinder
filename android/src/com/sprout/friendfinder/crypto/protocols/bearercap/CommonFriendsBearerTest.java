package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;

import android.util.Log;

/**
 * bearer PSI test shouldnt require any TTP
 * for now, pre-compute a user's capability
 * TODO: need to find a way to distribute the caps
 * @author Oak
 *
 */
public class CommonFriendsBearerTest extends BBFPSI {

  static final String TAG = CommonFriendsBearerTest.class.getSimpleName();
  ProtocolCallback callback;
  boolean testAppFlag = false;

  public CommonFriendsBearerTest(CommunicationService s, Map<BigInteger, String> capMap, ProtocolCallback cb, boolean testApp) {
    this(s, capMap, cb);
    
    this.testAppFlag = testApp;
    setBenchmark(testApp);
  }

  public CommonFriendsBearerTest(CommunicationService s, Map<BigInteger, String> capMap, ProtocolCallback cb) {
    super(s, capMap);

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
    callback.onComplete(result);
  }

}
