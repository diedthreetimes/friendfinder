package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;

/**
 * PSI with bearer capabilities
 * extending Bearer PSICA
 * @author root
 *
 */
public class BPSI extends BPSICA {

  Map<BigInteger, String> capMap;
  Map<ByteArrayWrapper, String> bearerCapMap = new HashMap<ByteArrayWrapper, String>();
  
  static final String TAG = BPSI.class.getSimpleName();
  
  public BPSI(CommunicationService s, Map<BigInteger, String> contactCaps) {
    super(s, new ArrayList<BigInteger>(contactCaps.keySet()));
    
    capMap = contactCaps;
  }

  /**
   * cant really use the one in abstract class
   * because we need to keep track of the owner of "shuffled" capabilities
   * and dont wanna make any assumption of bearer capabilities construction
   * TODO: any more efficient/ cleaner way to do this? / duplicate in BBFPSI
   */
  @Override
  protected BearerCapabilities computeServerBearerCapabilites(byte[] serverPK, byte[] clientPK) {
    Log.i(TAG, "computing server bearer cap separately and keeping track of the owner");
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
    
    List<String> res = new ArrayList<String>(result.size());
    for(byte[] b : result) {
      String intersect = bearerCapMap.get(new ByteArrayWrapper(b));
      res.add(intersect);
    }
    return res;
  }
}
