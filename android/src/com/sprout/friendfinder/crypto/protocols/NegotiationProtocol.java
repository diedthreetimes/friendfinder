package com.sprout.friendfinder.crypto.protocols;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PrivateProtocol;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;

/**
 * run this protocol before starting real protocols
 * this will negotiate what protocol we are going to run
 * @author norrathep
 *
 */
public class NegotiationProtocol extends PrivateProtocol<Void, Void, String> {
  
  static final String TAG = NegotiationProtocol.class.getSimpleName();
  String yourProtocol;
  ProfileDownloadCallback callback;
  
  public NegotiationProtocol(CommunicationService s, String yourProtocol, ProfileDownloadCallback callback) {
    this(TAG, s, false);
    this.yourProtocol = yourProtocol;
    this.callback = callback;
    
  }

  protected NegotiationProtocol(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
  }

  @Override
  protected String conductClientTest(CommunicationService s, Void... input) {
    return conductServerTest(s, input);
  }

  /**
   * TODO: implement idx protocol
   */
  @Override
  protected String conductServerTest(CommunicationService s, Void... input) {
    s.write(yourProtocol);
    String peerProtocol = s.readString();
    return peerProtocol;
  }

  @Override
  protected void loadSharedKeys() {}

  @Override
  public String doInBackground(Void... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "Negotiation failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the negotiation protocol");
  }

  @Override
  public void onPostExecute(String result) {
    // TODO: decide what protocol to run, right now assume both protocols have to match
    if(result == null || !result.equals(yourProtocol)) {
      Log.i(TAG, "Conflict: I want to run "+yourProtocol+" he wants to run "+result+", aborting");
      callback.onError();
    }
    // maybe need to put protocol in oncomplete
    else callback.onComplete();
  }
}
