package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brickred.socialauth.util.Base64;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;

/**
 * Bloom Filter based PSI with Bearer Capabilities
 * extending BloomFilter PSICA, and modify a bit to return
 * the actual intersections instead of dummy
 * @author Oak
 *
 */
public class BBFPSI extends BPSICA {
  
  Map<BigInteger, String> capMap;
  Map<ByteArrayWrapper, String> bearerCapMap = new HashMap<ByteArrayWrapper, String>();
  
  static final String TAG = BBFPSI.class.getSimpleName();
  
  public BBFPSI(CommunicationService s, Map<BigInteger, String> contactCaps) {
    super(s, new ArrayList<BigInteger>(contactCaps.keySet()));
    
    capMap = contactCaps;
  }

  /**
   * cant really use the one in abstract class
   * because we need to keep track of the owner of "shuffled" capabilities
   * and dont wanna make any assumption of bearer capabilities construction
   * TODO: any more efficient/ cleaner way to do this?
   */
  @Override
  protected BearerCapabilities computeServerBearerCapabilites(byte[] serverPK, byte[] clientPK) {
    Log.i(TAG, "computing server bearer cap separately");
    List<byte[]> serverBearerCapabilities = new ArrayList<byte[]>();
    
    for(BigInteger ci : capabilities) {
      // bearer capability = ci | PkServer | PkClient
      byte[] ciArray = ci.toByteArray();
      byte[] bearerCap = new byte[ciArray.length+serverPK.length+clientPK.length];
      System.arraycopy(ciArray, 0, bearerCap, 0, ciArray.length);
      System.arraycopy(serverPK, 0, bearerCap, ciArray.length, serverPK.length);
      System.arraycopy(clientPK, 0, bearerCap, ciArray.length+serverPK.length, clientPK.length);
      
      serverBearerCapabilities.add(bearerCap);
      
      bearerCapMap.put(new ByteArrayWrapper(bearerCap), capMap.get(ci));
    }
    Log.i(TAG, "num server cap: "+serverBearerCapabilities.size());
    return new FullBearerCapabilities(serverBearerCapabilities);
  }

  /**
   * cant reuse verification cuz we need to know the order
   * TODO: any more efficient/ cleaner way to do this?
   */
  @Override
  protected List<String> verification(CommunicationService s, List<byte[]> result) {
    // verify intersection results
    List<String> hashResult = new ArrayList<String>();
    Map<String, String> intersectBearerCaps = new HashMap<String, String>();
    for(byte[] bearerCap : result) {
      String h = hash(bearerCap, (byte) 0).toString(16);
      hashResult.add(h);
      intersectBearerCaps.put(h, bearerCapMap.get(new ByteArrayWrapper(bearerCap)));
    }
    Collections.shuffle(hashResult, new SecureRandom());
    
    s.write(Base64.encodeObject((Serializable) hashResult));
    
    @SuppressWarnings("unchecked")
    List<String> theirResult = (ArrayList<String>) Base64.decodeToObject(s.readString());
    
    hashResult.retainAll(theirResult);
    Log.i(TAG, "Number of intersection after verification: "+hashResult.size());
    
    List<String> res = new ArrayList<String>(hashResult.size());
    for(String hr : hashResult) {
      res.add(intersectBearerCaps.get(hr));
      Log.i(TAG, "intersect res: "+intersectBearerCaps.get(hr));
    }
    return res;
  }
  
}
