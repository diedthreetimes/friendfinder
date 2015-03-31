package com.sprout.friendfinder.backend;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.crypto.AuthorizationObject;

public class ATWPSICAProtocol extends ATWPSIProtocol {
  
  String TAG = ATWPSICAProtocol.class.getSimpleName();


  public ATWPSICAProtocol(CommunicationService s, AuthorizationObject authObject) {
    super(s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSICAProtocol(String testName, CommunicationService s, AuthorizationObject authObject) {
    super(testName, s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSICAProtocol(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
    super(testName, s, authObject, client); // This relies on the fact that the protocol is mirrored. 
  }

  @Override
  protected List<BigInteger> sendMessage(CommunicationService s) {
    List<BigInteger> peerMessage = peerAuth.getData();
    List<BigInteger> T = new ArrayList<BigInteger>();
    for (BigInteger t1i : peerMessage) {
      t1i = sha1hash(t1i.modPow(authObj.getR(), p).toString(16)); // the only change...

      T.add(t1i);

      s.write(t1i);
    }
    return T;
  }
  
  /**
   * map string to Z+
   * @param s
   * @return
   */
  private BigInteger sha1hash(String s){
    
    byte[] message = s.getBytes();
    
    // input = selector | message
    byte [] input = new byte[message.length];
    System.arraycopy(message, 0, input, 0, message.length);
    
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e1) {
      Log.e(TAG, "SHA-1 is not supported");
      return null; // TODO: Raise an error?
    }
    digest.reset();
          
    return new BigInteger(1, digest.digest(input)); // tell bigint that its positive
  }
}
