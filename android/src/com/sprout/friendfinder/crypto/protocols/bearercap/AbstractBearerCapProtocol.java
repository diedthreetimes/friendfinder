package com.sprout.friendfinder.crypto.protocols.bearercap;

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

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.AbstractPSIProtocol;

import android.util.Log;

/**
 * base class for all two-way bearer capability protocols
 * @author root
 *
 */
public abstract class AbstractBearerCapProtocol extends AbstractPSIProtocol<String, Void, List<String>> {
  
  
  private final String DH_ALGORITHM = "EC";
  static String TAG = AbstractBearerCapProtocol.class.getSimpleName();
  KeyPair requestor_key_pair;
  boolean amIClient = false;
  
  
  List<BigInteger> capabilities;

  public AbstractBearerCapProtocol(CommunicationService s, List<BigInteger> caps, boolean shuffleCaps) {
    super(TAG, s, true);
    capabilities = caps;
    if(shuffleCaps) Collections.shuffle(capabilities, new SecureRandom());
  }

  @Override
  protected List<String> conductClientTest(CommunicationService s, String... input) {
    amIClient = true;
    return conductServerTest(s, input);
  }

  @Override
  protected List<String> conductServerTest(CommunicationService s, String... input) {
    
    if (benchmark) {
      onlineWatch.start();
    }
    
    byte[] serverPK = requestor_key_pair.getPublic().getEncoded();
    
    byte[] clientPK = publicKeyExchange(s, serverPK);
    
    Log.i(TAG, "Server pk: "+bytesToHex(serverPK));
    Log.i(TAG, "Client pk: "+bytesToHex(clientPK));

    // switch the keys if you are actually a client
    if(amIClient) {
      byte[] tempkey = serverPK;
      serverPK = clientPK;
      clientPK = tempkey; 
    }
    
    Log.i(TAG, "Server pk: "+bytesToHex(serverPK));
    Log.i(TAG, "Client pk: "+bytesToHex(clientPK));
    
    BearerCapabilities serverBearerCapabilities = computeServerBearerCapabilites(serverPK, clientPK);
    
    sendServerBearerCapabilities(s, serverBearerCapabilities);
    
    BearerCapabilities clientBearerCapabilities = receiveClientBearerCapabilities(s);
    
    List<byte[]> result = computeIntersection(serverBearerCapabilities, clientBearerCapabilities);
    Log.i(TAG, "Number of intersection before verification: "+result.size());
    
    List<String> intersection = verification(s, result);
    Log.i(TAG, "Number of intersection after verification: "+intersection.size());

    if (benchmark) {
      onlineWatch.pause();
    }
    
    return intersection;
  }

  /**
   * we always compute bearer capabilities on our side
   * @param serverPK
   * @param clientPK
   */
  protected BearerCapabilities computeServerBearerCapabilites(byte[] serverPK, byte[] clientPK) {

    List<byte[]> serverBearerCapabilities = new ArrayList<byte[]>();
    
    for(BigInteger ci : capabilities) {
      // bearer capability = ci | PkServer | PkClient
      byte[] ciArray = ci.toByteArray();
      byte[] bearerCap = new byte[ciArray.length+serverPK.length+clientPK.length];
      System.arraycopy(ciArray, 0, bearerCap, 0, ciArray.length);
      System.arraycopy(serverPK, 0, bearerCap, ciArray.length, serverPK.length);
      System.arraycopy(clientPK, 0, bearerCap, ciArray.length+serverPK.length, clientPK.length);
      
      serverBearerCapabilities.add(bearerCap);
    }
    return new FullBearerCapabilities(serverBearerCapabilities);
  }
  
  /**
   * sometimes we send bloomfilter version or full version
   * @param s
   * @param serverBearerCapabilities
   */
  protected abstract void sendServerBearerCapabilities(CommunicationService s, BearerCapabilities serverBearerCapabilities);
  
  protected abstract BearerCapabilities receiveClientBearerCapabilities(CommunicationService s);
  
  /**
   * as the name says... should be the same for all variants
   * @param serverBearerCapabilities
   * @param clientBearerCapabilities
   * @return
   */
  protected List<byte[]> computeIntersection(BearerCapabilities serverBearerCapabilities, BearerCapabilities clientBearerCapabilities) {
    return serverBearerCapabilities.intersect(clientBearerCapabilities);
  }
  
  /**
   * optional, verification and return the actual userid, not bearer cap
   * @param s
   * @param result
   * @return
   */
  protected abstract List<String> verification(CommunicationService s, List<byte[]> result);
  
  /**
   * this part should be the same for all variants
   * exchange and return serverPK, clientPK
   * switch serverPK and clientPK if we are client
   * @param s
   * @param serverPK
   * @param clientPK
   */
  protected byte[] publicKeyExchange(CommunicationService s, byte[] serverPK) {
    // key exchange
    s.write(serverPK);
    
    byte[] clientPK = s.read();
    return clientPK;
  }
  
  protected long getTotalOnlineTime() {
    return onlineWatch.getElapsedTime();
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
