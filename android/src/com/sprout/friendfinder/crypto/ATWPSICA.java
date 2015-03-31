package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.ATWPSIProtocol;

//Authorized Two Way Psi cardinality
public class ATWPSICA extends ATWPSIProtocol {

  private static String TAG = ATWPSICA.class.getSimpleName();
  AuthorizationObject authObj;

  public ATWPSICA(CommunicationService s, AuthorizationObject authObject) {
    super(s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSICA(String testName, CommunicationService s, AuthorizationObject authObject) {
    super(testName, s, authObject); // This relies on the fact that the protocol is mirrored. 
  }

  public ATWPSICA(String testName, CommunicationService s, AuthorizationObject authObject, boolean client) {
    super(testName, s, authObject, client); // This relies on the fact that the protocol is mirrored. 
  }
  

  /**
   * compute and send a message
   * @param s
   * @return
   */
  @Override
  protected List<BigInteger> sendMessage(CommunicationService s) {
    //A decodes B's yis
    List<BigInteger> peerSet = peerAuth.getData();
    List<BigInteger> wi, yiPrime;
    int peerSetSize = peerSet.size();
    if(peerSetSize%2 != 0) {
      Log.e(TAG, "number of elements in peerSet is odd: "+peerSetSize);
      return null;
    }
    yiPrime = peerSet.subList(0, peerSetSize/2); // yi
    Collections.shuffle(yiPrime, new SecureRandom()); // TODO: maybe should use SHA1PRNG
    wi = peerSet.subList(peerSetSize/2, peerSetSize);
    
    Log.d(TAG, "Size of yis: " + peerSetSize/2);

    // TODO: Implement and measure threading
    //A performs computations on yis and sends them to B:
    for (BigInteger t1i : yiPrime) {
      t1i = t1i.modPow(authObj.getR2(), p);

      s.write(t1i);
    }
    return wi;
  }
  
  /**
   * receive peer's message
   * @param s
   * @return
   */
  @Override
  protected List<BigInteger> receiveMessage(CommunicationService s) {
    
    //prepare for receiving B's computed tags
    List<BigInteger> hbi = new ArrayList<BigInteger>();
    
    BigInteger RacInv = authObj.getR().modInverse(q);

    List<BigInteger> zivis = authObj.getData();
    List<BigInteger> zis = zivis.subList(0, zivis.size()/2);
    //A receives B's computed tags:
    for (int i = 0; i < zis.size(); i++) {
      BigInteger ziPrime = s.readBigInteger();
      hbi.add(hashPrime(ziPrime.modPow(RacInv, p).toString(16))); // hash'(ziPrime^inv(Rac))
    }
    
    return hbi;
  }

//  @Override
//  protected List<String> conductServerTest(CommunicationService s, String... input) {
//      Log.d(TAG, "STARTING TEST");
//      
//      if (benchmark) {
//        onlineWatch.start();
//      }
//
//      //keep track of state the PSI protocol currently is in
//      BigInteger Rac, Ras;
//      List<BigInteger> zivis, zis;
//      AuthorizationObject peerAuth;
//
//      Rac = authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
//      Ras = authObj.getR2();
//      zivis = authObj.getData();
//      int zivisSize = zivis.size();
//      if(zivisSize%2 != 0) {
//        Log.e(TAG, "number of elements in zivis is odd: "+zivisSize);
//        return null;
//      }
//      zis = zivis.subList(0, zivisSize/2);
//      
//      Log.d(TAG, "Zis length: " + zis.size());
//      
//      s.write(authObj.getAuth());
//
//      //A received B's auth:
//      peerAuth = new AuthorizationObject(authObj, s.readString()); // TODO: We need to resolve what is sent and received
//
//      //A checks B's auth:
//      if (peerAuth.verify()) {
//        Log.d(TAG, "Contact set verification succeeded");
//      }
//      else {
//        Log.d(TAG, "Contact set verification failed");
//        return null; // TODO: Raise here instead
//      }
//
//      //A decodes B's yis
//      List<BigInteger> peerSet = peerAuth.getData();
//      List<BigInteger> wi, yiPrime;
//      int peerSetSize = peerSet.size();
//      if(peerSetSize%2 != 0) {
//        Log.e(TAG, "number of elements in peerSet is odd: "+peerSetSize);
//        return null;
//      }
//      yiPrime = peerSet.subList(0, peerSetSize/2); // yi
//      Collections.shuffle(yiPrime, new SecureRandom()); // TODO: maybe should use SHA1PRNG
//      wi = peerSet.subList(peerSetSize/2, peerSetSize);
//      
//      Log.d(TAG, "Size of yis: " + peerSetSize/2);
//
//      // TODO: Implement and measure threading
//      //A performs computations on yis and sends them to B:
//      for (BigInteger t1i : yiPrime) {
//        t1i = t1i.modPow(Ras, p);
//
//        s.write(t1i);
//      }
//
//      //prepare for receiving B's computed tags
//      List<BigInteger> hbi = new ArrayList<BigInteger>(); //hbi
//      
//      BigInteger RacInv = Rac.modInverse(q);
//
//      //A receives B's computed tags:
//      for (int i = 0; i < zis.size(); i++) {
//        BigInteger ziPrime = s.readBigInteger();
//        hbi.add(hashPrime(ziPrime.modPow(RacInv, p).toString(16))); // hash'(ziPrime^inv(Rac))
//      }
//
//      //output result set:
//      List<String> result = new ArrayList<String>();
//
//      List<String> originalOrder = authObj.getOriginalOrder();
//      
//      if (originalOrder.size() != input.length) {
//        // This seems to be due to an issue with server downloading an improper number of connections from linkedIn.
//        // From what I can tell, it seems to be a bug on linkedIn's side. But it could be the linkedIn ruby client as well
//        Log.e(TAG, "Server authorization doesn't match provided input. Server: " + originalOrder.size() + ". Input: " + input.length);
//      }
//      
//      for (int l = 0; l < hbi.size(); l++) {
//        for (int l2 = 0; l2 < wi.size(); l2++) {
//          if (wi.get(l2).equals(hbi.get(l))) {
//            // This is a strange side affect of not recomputing the hashes ourselves.
//            // Instead, we trust the CA to give us the order
//            if (originalOrder != null) {
//              if (l < originalOrder.size()) {
//                // We may want to verify that this is indeed one of our inputs
//                // We trust the server, so for now we don't bother
//                result.add(originalOrder.get(l));
//              } else {
//                Log.e(TAG, "Attempted ot access input form server out of range");
//              }
//            } else {
//              // Here we can fallback on recomputing the masked hashes from the original input and the secret
//              // TODO: 
//              
//              Log.e(TAG, "Recovery of inputs not implemented. Order must be supplied form the server. "
//                  + "Perhaps the authorization could not be parsed?");
//            }
//          }
//        }
//      }
//
//      Log.d(TAG, "Found " + result.size() + " common inputs");
//      
//      if (benchmark) {
//        onlineWatch.pause();
//      }
//
//      return result;
//  }
  
  // H' - second hash fn in PSI-CA
  public BigInteger hashPrime(String input) {
    return hash(input.getBytes(), (byte)1).mod(p).modPow(t, p);
  }
  public BigInteger hash(String input) {
    return hash(input.getBytes(), (byte)0).mod(p).modPow(t, p);
  }

  // rewrite this function to always return a positive
  public BigInteger hash(byte [] message, byte selector){
    
    // input = selector | message
    byte [] input = new byte[message.length + 1];
    System.arraycopy(message, 0, input, 1, message.length);
    input[0] = selector;
    
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
