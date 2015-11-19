package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sprout.finderlib.communication.CommunicationService;

/**
 * Bearer BloomFilter PSI without verification step
 * extending Bearer BloomFilter PSI and override verification function
 * @author Oak
 *
 */
public class BBFPSI_NoV extends BBFPSI {

  public BBFPSI_NoV(CommunicationService s, Map<BigInteger, String> contactCaps) {
    super(s, contactCaps);
  }

  /**
   * cant reuse verification cuz we need to know the order
   * TODO: any more efficient / cleaner / no-duplicate way to do this?
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
