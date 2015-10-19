package com.sprout.friendfinder.crypto;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;

public class ATWBearerPSICA extends AbstractPSIProtocol<String, Void, List<String>> {

  private final String DH_ALGORITHM = "EC";
  static String TAG = ATWBearerPSICA.class.getSimpleName();
  List<BigInteger> capabilities;
  BigInteger Pkl;
  BigInteger Pkr;
  KeyPair requestor_key_pair;
  boolean amIClient = false;

  public ATWBearerPSICA(CommunicationService s, List<BigInteger> caps) {
    super(TAG, s, true);
    capabilities = caps;
  }

  public ATWBearerPSICA(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
    
  }

  @Override
  protected List<String> conductClientTest(CommunicationService s,
      String... input) {
    // not exactly mirror protocol, need to switch the keys
    amIClient = true;
    return conductServerTest(s, input);
  }

  // TODO: send bloom filter instead of hash(ci | PkS | PkC)
  @Override
  protected List<String> conductServerTest(CommunicationService s,
      String... input) {
    
    // key exchange
    byte[] serverPK = requestor_key_pair.getPublic().getEncoded();
    s.write(serverPK);
    
    byte[] clientPK = s.read();
    
    if(amIClient) {
      // switch the keys
      byte[] tempkey = serverPK;
      serverPK = clientPK;
      clientPK = tempkey; 
    }
    
    // send and receive bearer capabilities
    List<String> serverBearerCapabilities = new ArrayList<String>();
    List<String> clientBearerCapabilities = new ArrayList<String>();
    
    s.write(capabilities.size()+"");
    int clientCapSize = Integer.parseInt(s.readString());
    
    Collections.shuffle(capabilities, new SecureRandom());
    for(BigInteger ci : capabilities) {
      // bearer capability = ci | PkServer | PkClient
      byte[] ciArray = ci.toByteArray();
      byte[] bearerCap = new byte[ciArray.length+serverPK.length+clientPK.length];
      System.arraycopy(ciArray, 0, bearerCap, 0, ciArray.length);
      System.arraycopy(serverPK, 0, bearerCap, ciArray.length, serverPK.length);
      System.arraycopy(clientPK, 0, bearerCap, ciArray.length+serverPK.length, clientPK.length);
      
      String cap = hash(bearerCap, (byte) 0).toString(16);
      serverBearerCapabilities.add(cap);
      Log.d(TAG, "my bearer cap: "+cap);
      s.write(cap);

    }
    
    
    for(int i=0; i<clientCapSize; i++) {
      clientBearerCapabilities.add(s.readString());
      Log.d(TAG, "their bearer cap: "+clientBearerCapabilities.get(i));
    }
    
    // compute intersection
    clientBearerCapabilities.retainAll(serverBearerCapabilities);
    
    return clientBearerCapabilities;
  }
  
  @Override
  protected void loadSharedKeys(){
    Log.d(TAG, "generating dh key");
    try {
      KeyPairGenerator key_generator = KeyPairGenerator.getInstance(DH_ALGORITHM);
      key_generator.initialize(new ECGenParameterSpec("prime192v1"));

      requestor_key_pair = key_generator.generateKeyPair();
    } catch(NoSuchAlgorithmException e) {
      Log.e(TAG,"NoSuchAlgorithm: " + e.getMessage());
    } catch(InvalidAlgorithmParameterException e) {
      Log.e(TAG,"InvalidAlgorithmParameter: " + e.getMessage());
    }
  }
  
  public String bytesToHex(byte[] bytes) {
    char[] hexArray = "0123456789ABCDEF".toCharArray();
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

}
