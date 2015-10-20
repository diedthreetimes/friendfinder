package com.sprout.friendfinder.crypto.protocols;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PrivateProtocol;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager.ProtocolType;

/**
 * run this protocol before starting real protocols
 * this will negotiate what protocol we are going to run
 * @author norrathep
 *
 */
public class NegotiationProtocol extends PrivateProtocol<Void, Void, ProtocolType> {
  
  static final String TAG = NegotiationProtocol.class.getSimpleName();
  ProtocolType yourProtocol;
  ProtocolCallback callback;
  
  public NegotiationProtocol(CommunicationService s, ProtocolType yourProtocol, ProtocolCallback callback) {
    this(TAG, s, false);
    this.yourProtocol = yourProtocol;
    this.callback = callback;
    
  }

  protected NegotiationProtocol(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
  }

  @Override
  protected ProtocolType conductClientTest(CommunicationService s, Void... input) {
    return conductServerTest(s, input);
  }

  /**
   * TODO: implement idx protocol
   */
  @Override
  protected ProtocolType conductServerTest(CommunicationService s, Void... input) {
    s.write(yourProtocol.name());
    return ProtocolType.valueOf(s.readString());
  }

  @Override
  protected void loadSharedKeys() {}

  @Override
  public ProtocolType doInBackground(Void... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "Negotiation failed, ", e);
      return ProtocolType.NONE;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the negotiation protocol");
  }

  @Override
  public void onPostExecute(ProtocolType result) {
    // TODO: decide what protocol to run, right now assume both protocols have to match
    Log.i(TAG, "Result protocol is "+result.name());
    if(result == ProtocolType.NONE) {
      Log.i(TAG, "Error");
      callback.onError(result);
    }
    else {
      // return higher-priority protocol
      ProtocolType resultProtocol = ProtocolType.max(yourProtocol, result);
      callback.onComplete(resultProtocol);
    }
  }
}
