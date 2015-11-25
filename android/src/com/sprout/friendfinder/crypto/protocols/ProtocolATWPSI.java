package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.ATWPSIProtocol;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

import android.util.Log;

public class ProtocolATWPSI implements Protocol {

  static final String TAG = ProtocolATWPSI.class.getSimpleName();
  
  @Override
  public AuthorizationObjectType getAuthType() {
    return AuthorizationObjectType.PSI;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {
    new ATWPSITest(cs, auth, callback).execute();
    
  }
  
  private int mNumTrials = 1;
  private boolean mBenchmark = Config.getBenchmark();

  @Override
  public void config(int numTrials, boolean benchmark) {
    this.mNumTrials = numTrials;
    this.mBenchmark = benchmark;
    
  }
  
  private class ATWPSITest extends ATWPSIProtocol {
    
    ProtocolCallback callback;

    public ATWPSITest(CommunicationService s, AuthorizationObject authObject, ProtocolCallback callback) {
      super(s, authObject);
      
      this.callback = callback;

      setBenchmark(mBenchmark);
      setNumTrials(mNumTrials);
    }
    
    @Override
    public List<String> doInBackground(String... params) {
      try {
        return super.doInBackground(params);
      } catch (Exception e) {
        Log.e(TAG, "CommonFriendsProtocol failed, ", e);
        return null;
      }
    }

    @Override
    public void onPreExecute() {
      Log.i(TAG, "About to execute the common friends protocol");
    }

    @Override
    public void onPostExecute(List<String> result) {
      Log.i(TAG, "Common friends protocol complete");
      
      if (result == null) {
        callback.onError(null);
        return;
      }
      
//      ContactsListObject contacts = new ContactsListObject();
//      contacts.save();
//
//      for (String id : result){          
//        contacts.put(id);
//      }
//      
//      interaction.sharedContacts = contacts;
    
      callback.onComplete(new ProtocolResult(onlineWatch.getElapsedTime(), result, mNumTrials));
    }
  }

}
