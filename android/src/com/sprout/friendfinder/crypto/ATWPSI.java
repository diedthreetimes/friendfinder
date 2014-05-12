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

  // TODO: We need a way to set the authorization parameters.
  //   This is probably easiest to do in a function

  // TODO: Make this protocol pipelined

  @Override
  protected List<String> conductClientTest(CommunicationService s, String... input) {
    // As of now, the protocol is mirrored for client and server
    return conductServerTest(s, input);
  }

  @Override
  protected List<String> conductServerTest(CommunicationService s, String... input) {

    Log.d(TAG, "STARTING TEST");

    //keep track of state the PSI protocol currently is in
    BigInteger Ra, Rb;
    List<BigInteger> zis, yis;
    AuthorizationObject peerAuth;
    List<BigInteger> T, T2;

    Ra = authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts

    zis = authObj.getData();
    // TODO: Remove this once we know authorization is working
    //		zis = new ArrayList<BigInteger>();
    //		for (String inp : input) {
    //			BigInteger zi = hash(inp).modPow(Ra, p); // TODO: This set needs to be part of the authorization.
    //			zis.add(zi);		
    //		}

    //send zis + auth to peer:
    //send number of zis first, so that peer knows how many BigInt-Reads it has to conduct:
    //		s.write(String.valueOf(zis.size()));
    //		
    //	  	//A sends zis to B
    //	  	for (int i = 0; i < zis.size(); i++) {          		  
    //	  		s.write(zis.get(i));
    //	  	}
    s.write(authObj.getAuth());

    //A received B's auth:
    peerAuth = (AuthorizationObject) s.readSerializable(); // TODO: We need to resolve what is sent and received

    //A checks B's auth:
    if (peerAuth.verify()) {
      Log.d(TAG, "A: Contact set verification succeeded");
    }
    else {
      Log.d(TAG, "A: Contact set verification failed");
      return null; // TODO: Raise here instead
    }

    //A decodes B's yis
    List<BigInteger> peerSet = peerAuth.getData();

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

    for (int l = 0; l < T2.size(); l++) {
      for (int l2 = 0; l2 < T.size(); l2++) {
        if (T.get(l).equals(T2.get(l2))) {
          result.add(input[l]);
        }
      }
    }

    Log.d(TAG, "Found " + result.size() + " common inputs");

    return result;
  }
}
