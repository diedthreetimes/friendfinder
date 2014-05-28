package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;


// Authorized Two Way Psi 
public class ATWPSI extends AbstractPSIProtocol<String, Void, List<String>> {
  private static String TAG = "TwoWayPSI";
  AuthorizationObject authObj;

  public ATWPSI(CommunicationService s, AuthorizationObject authObject) {
    super(TAG, s, true); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }

  public ATWPSI(String testName, CommunicationService s, AuthorizationObject authObject) {
    super(testName, s, true); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }

  public ATWPSI(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
    super(testName, s, client); // This relies on the fact that the protocol is mirrored. 

    authObj = authObject;
  }

  // TODO: Make this protocol threaded. This is necessary for balanced security.

  @Override
  protected List<String> conductClientTest(CommunicationService s, String... input) {
    // As of now, the protocol is mirrored for client and server
    return conductServerTest(s, input);
  }

  @Override
  protected List<String> conductServerTest(CommunicationService s, String... input) {

    Log.d(TAG, "STARTING TEST");

    //keep track of state the PSI protocol currently is in
    BigInteger Ra;
    List<BigInteger> zis;
    AuthorizationObject peerAuth;
    List<BigInteger> T, T2;

    Ra = authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
    zis = authObj.getData();
    
    Log.d(TAG, "Zis length: " + zis.size());
    
    s.write(authObj.getAuth());

    //A received B's auth:
    peerAuth = new AuthorizationObject(authObj, s.readString()); // TODO: We need to resolve what is sent and received

    //A checks B's auth:
    if (peerAuth.verify()) {
      Log.d(TAG, "Contact set verification succeeded");
    }
    else {
      Log.d(TAG, "Contact set verification failed");
      return null; // TODO: Raise here instead
    }

    //A decodes B's yis
    List<BigInteger> peerSet = peerAuth.getData();
    
    Log.d(TAG, "Size of yis: " + peerSet.size());

    // TODO: Implement and measure threading
    //A performs computations on yis and sends them to B:
    T = new ArrayList<BigInteger>();
    for (BigInteger t1i : peerSet) {
      t1i = t1i.modPow(Ra, p);
      T.add(t1i);

      s.write(t1i);
    }

    //prepare for receiving B's computed tags
    T2 = new ArrayList<BigInteger>();

    //A receives B's computed tags:
    for (int i = 0; i < zis.size(); i++) {
      T2.add(s.readBigInteger());
    }

    //output result set:
    List<String> result = new ArrayList<String>();

    List<String> originalOrder = authObj.getOriginalOrder();
    
    if (originalOrder.size() != input.length) {
      // This seems to be due to an issue with server downloading an improper number of connections from linkedIn.
      // From what I can tell, it seems to be a bug on linkedIn's side. But it could be the linkedIn ruby client as well
      Log.e(TAG, "Server authorization doesn't match provided input. Server: " + originalOrder.size() + ". Input: " + input.length);
    }
    
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

    Log.d(TAG, "Found " + result.size() + " common inputs");

    return result;
  }
}
