package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;

/**
 * base class for ATWPSI/ATWPSICA protocols
 * those protocols can be generalized into 5 steps
 * 1) send auth 2) receive auth 3) send your msg 4) receive msg 5) compute intersections
 * @author norrathep
 *
 */
public abstract class AbstractATWPSIProtocol extends AbstractPSIProtocol<String, Void, List<String>> {
  private static String TAG = AbstractATWPSIProtocol.class.getSimpleName();
  AuthorizationObject authObj;
  AuthorizationObject peerAuth;

  public AbstractATWPSIProtocol(CommunicationService s, AuthorizationObject authObject) {
    super(TAG, s, true); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }

  public AbstractATWPSIProtocol(String testName, CommunicationService s, AuthorizationObject authObject) {
    super(testName, s, true); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }

  public AbstractATWPSIProtocol(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
    super(testName, s, client); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }
  
  /**
   * send auth
   * @param s
   */
  protected abstract void sendAuth(CommunicationService s);
  
  /**
   * receive peer's auth
   * @param s
   * @return peer's auth
   */
  protected abstract AuthorizationObject receiveAuth(CommunicationService s);

  /**
   * compute and send a message
   * @param s
   * @return sent message
   */
  protected abstract List<BigInteger> sendMessage(CommunicationService s);
  
  /**
   * receive peer's message
   * @param s
   * @return peer's message
   */
  protected abstract List<BigInteger> receiveMessage(CommunicationService s);
  
  /**
   * 
   * @param T - our msg
   * @param T2 - peer msg
   * @return intersection
   */
  protected abstract List<String> computeIntersectionResult(List<BigInteger> T, List<BigInteger> T2);
  
  /**
   * how it works -
   * 1) send your auth
   * 2) receive peer's auth
   * 3) send msg for later computing intersection on peer's side
   * 4) receive msg from peer
   * 5) compute intersection from messages in step 3,4
   */
  @Override
  protected List<String> conductServerTest(CommunicationService s, String... input) {
    Log.d(TAG, "STARTING TEST");
    
    onlineWatch.start();
    Log.i(TAG, "starting time at "+onlineWatch.getElapsedTime());

    sendAuth(s);
    Log.i(TAG, "finish sending auth a "+onlineWatch.getElapsedTime());
    peerAuth = receiveAuth(s);
    Log.i(TAG, "finish receiving auth at "+onlineWatch.getElapsedTime());
    if(peerAuth==null) return null; // TODO: check auth type
    List<BigInteger> T = sendMessage(s);
    Log.i(TAG, "finish sending messages at "+onlineWatch.getElapsedTime());
    if(T==null) return null;
    List<BigInteger> T2 = receiveMessage(s);
    Log.i(TAG, "finish receiving messages at "+onlineWatch.getElapsedTime());
    if(T2==null) return null;

    if (authObj.getOriginalOrder().size() != input.length) {
      // This seems to be due to an issue with server downloading an improper number of connections from linkedIn.
      // From what I can tell, it seems to be a bug on linkedIn's side. But it could be the linkedIn ruby client as well
      Log.e(TAG, "Server authorization doesn't match provided input. Server: " + authObj.getOriginalOrder().size() + ". Input: " + input.length);
    }
    
    // making sure both devices are finished before computing intersection
    s.write("done");
    s.readString();
    
    onlineWatch.pause();

    // TODO: computing intersection is considered online?
    Log.d(TAG, "computing intersection");
    return computeIntersectionResult(T, T2);
  }

  @Override
  protected List<String> conductClientTest(CommunicationService s, String... input) {
    // As of now, the protocol is mirrored for client and server
    return conductServerTest(s, input);
  }
    
}
