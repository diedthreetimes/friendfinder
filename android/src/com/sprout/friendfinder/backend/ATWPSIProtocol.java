package com.sprout.friendfinder.backend;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.crypto.AuthorizationObject;

public class ATWPSIProtocol extends AbstractATWPSIProtocol {
  private static String TAG = ATWPSIProtocol.class.getSimpleName();
  protected AuthorizationObject authObj;
  protected AuthorizationObject peerAuth;


  public ATWPSIProtocol(CommunicationService s, AuthorizationObject authObject) {
    super(s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSIProtocol(String testName, CommunicationService s, AuthorizationObject authObject) {
    super(testName, s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSIProtocol(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
    super(testName, s, authObject, client); // This relies on the fact that the protocol is mirrored. 
  }
  
  /**
   * send auth
   * @param s
   */
  @Override
  protected void sendAuth(CommunicationService s) {
    s.write(authObj.getAuth());
  }
  
  /**
   * receive peer's auth
   * @param s
   * @return
   */
  @Override
  protected AuthorizationObject receiveAuth(CommunicationService s) {
    //A received B's auth:
    AuthorizationObject peerAuth = new AuthorizationObject(authObj, s.readString()); // TODO: We need to resolve what is sent and received

    //A checks B's auth:
    if (peerAuth.verify()) {
      Log.d(TAG, "Contact set verification succeeded");
    }
    else {
      Log.d(TAG, "Contact set verification failed");
      return null; // TODO: Raise here instead
    }
    return peerAuth;
  }

  /**
   * compute and send a message
   * @param s
   * @return A's computed tags
   */
  @Override
  protected List<BigInteger> sendMessage(CommunicationService s) {
    List<BigInteger> peerMessage = peerAuth.getData();
    List<BigInteger> T = new ArrayList<BigInteger>();
    for (BigInteger t1i : peerMessage) {
      t1i = t1i.modPow(authObj.getR(), p);

      T.add(t1i);

      s.write(t1i);
    }
    return T;
  }
  
  /**
   * receive peer's message
   * @param s
   * @return B's computed tags
   */
  @Override
  protected List<BigInteger> receiveMessage(CommunicationService s) {
    //prepare for receiving B's computed tags
    List<BigInteger> T2 = new ArrayList<BigInteger>();

    //A receives B's computed tags:
    List<BigInteger> zis = authObj.getData();
    for (int i = 0; i < zis.size(); i++) {
      T2.add(s.readBigInteger());
    }
    return T2;
  }

  @Override
  protected List<String> computeIntersectionResult(List<BigInteger> T, List<BigInteger> T2) {
    List<String> result = new ArrayList<String>();

    List<String> originalOrder = authObj.getOriginalOrder();
    
    for (int l = 0; l < T2.size(); l++) {
      for (int l2 = 0; l2 < T.size(); l2++) {
        if (T.get(l2).equals(T2.get(l))) {
          // This is a strange side affect of not recomputing the hashes ourselves.
          // Instead, we trust the CA to give us the order
          if (originalOrder != null) {
            if (l < originalOrder.size()) {
              // We may want to verify that this is indeed one of our inputs
              // We trust the server, so for now we don't bother
              result.add(originalOrder.get(l));
            } else {
              Log.e(TAG, "Attempted ot access input form server out of range");
            }
          } else {
            // Here we can fallback on recomputing the masked hashes from the original input and the secret
            // TODO: 
            
            Log.e(TAG, "Recovery of inputs not implemented. Order must be supplied form the server. "
                + "Perhaps the authorization could not be parsed?");
          }
        }
      }
    }
    return result;
  }
    
}
