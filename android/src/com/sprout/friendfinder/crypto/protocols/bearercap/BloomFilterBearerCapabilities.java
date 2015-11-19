package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.util.ArrayList;
import java.util.List;

import com.skjegstad.utils.BloomFilter;

import android.util.Log;

/**
 * a compressed version of bearer capability
 * @author root
 *
 */
public class BloomFilterBearerCapabilities extends BearerCapabilities {
  static String TAG = BloomFilterBearerCapabilities.class.getSimpleName();
  BloomFilter<String> bf = null;
  
  public BloomFilterBearerCapabilities(BloomFilter<String> bfIn) {
    super();
    bf = bfIn;
  }
  
  public BloomFilter<String> getBloomFilter() {
    return bf;
  }

  @Override
  public List<byte[]> intersect(BearerCapabilities others) {
    if(others instanceof BloomFilterBearerCapabilities) {
      // you cant intersect 2 bloomfilter cap
      Log.e(TAG, "cant intersect 2 bloom filters");
      return null;
    }
    
    List<byte[]> result = new ArrayList<byte[]>();
    
    // compute intersection and hash result
    for(byte[] bearerCap : ((FullBearerCapabilities) others).getByteBearerCapabilities()) {
      if(bf.contains(bearerCap)) {
        result.add(bearerCap);
//        result.add(hash(bearerCap, (byte) 0).toString(16));
      }
    }
    return result;
  }
    
}
