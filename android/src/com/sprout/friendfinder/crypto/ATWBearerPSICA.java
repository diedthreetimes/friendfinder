package com.sprout.friendfinder.crypto;

import java.io.Serializable;
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

import org.brickred.socialauth.util.Base64;

import android.util.Log;

import com.skjegstad.utils.BloomFilter;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;

public class ATWBearerPSICA extends AbstractPSIProtocol<String, Void, List<String>> {

  private final String DH_ALGORITHM = "EC";
  private final double BF_FALSE_POSITIVE = 0.1;
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

    Log.d(TAG, "STARTING TEST");
    
    if (benchmark) {
      onlineWatch.start();
    }
    
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
    List<byte[]> serverBearerCapabilities = new ArrayList<byte[]>();

    BloomFilter<String> bf = new BloomFilter<String>(BF_FALSE_POSITIVE, capabilities.size());
    
    Collections.shuffle(capabilities, new SecureRandom());
    for(BigInteger ci : capabilities) {
      // bearer capability = ci | PkServer | PkClient
      byte[] ciArray = ci.toByteArray();
      byte[] bearerCap = new byte[ciArray.length+serverPK.length+clientPK.length];
      System.arraycopy(ciArray, 0, bearerCap, 0, ciArray.length);
      System.arraycopy(serverPK, 0, bearerCap, ciArray.length, serverPK.length);
      System.arraycopy(clientPK, 0, bearerCap, ciArray.length+serverPK.length, clientPK.length);
      
      serverBearerCapabilities.add(bearerCap);

      bf.add(bearerCap);
    }
    
    // send bloom filter
    s.write(Base64.encodeObject(bf));

    // receive bloom filter
    @SuppressWarnings("unchecked")
    BloomFilter<String> clientBf = (BloomFilter<String>) Base64.decodeToObject(s.readString());
    
    // compute intersection and hash result
    List<String> results = new ArrayList<String>();
    for(byte[] bearerCap : serverBearerCapabilities) {
      if(clientBf.contains(bearerCap)) {
        results.add(hash(bearerCap, (byte) 0).toString(16));
      }
    }
    Log.i(TAG, "Number of intersection before verification: "+results.size());
    
    // verify intersection results
    // TODO: is it secure?
    Collections.shuffle(results, new SecureRandom());
    s.write(Base64.encodeObject((Serializable) results));
    @SuppressWarnings("unchecked")
    List<String> theirResult = (ArrayList<String>) Base64.decodeToObject(s.readString());
    results.retainAll(theirResult);
    Log.i(TAG, "Number of intersection after verification: "+results.size());
    // doesnt matter what to return, we only need size of results

    if (benchmark) {
      onlineWatch.pause();
    }
    
    return results;
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
