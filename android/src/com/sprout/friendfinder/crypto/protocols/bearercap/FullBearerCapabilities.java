package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.skjegstad.utils.BloomFilter;

/**
 * full version contains the actual bearer capabilities
 * @author root
 *
 */
public class FullBearerCapabilities extends BearerCapabilities {
  List<byte[]> byteRepresentation = new ArrayList<byte[]>();
  
  public FullBearerCapabilities(List<byte[]> in) {
    byteRepresentation = in;
  }
  
  public BloomFilterBearerCapabilities compress(double bfPosRate) {

    BloomFilter<String> bf = new BloomFilter<String>(bfPosRate, byteRepresentation.size());
    for(byte[] b : byteRepresentation) {
      bf.add(b);
    }
    return new BloomFilterBearerCapabilities(bf);
  }
  
  /**
   * intersect 2 sets of capabilities
   * my set has to contain byteRepresentation
   * TODO: optimize this
   * @param others can contain either bf or byteRep, if both presents use bf
   * @return
   */
  @Override
  public List<byte[]> intersect(BearerCapabilities others) {
    // case 1: no bloom filter
    if(others instanceof BloomFilterBearerCapabilities) {
      return others.intersect(this);
    }
    List<byte[]> otherCap = ((FullBearerCapabilities) others).getByteBearerCapabilities();
    List<byte[]> result = new ArrayList<byte[]>();
    
    // Convert list of byte arrays to ByteArrayWrapper
    // since we cant do retainAll on List<byte[]>
    Set<ByteArrayWrapper> baw1 = new HashSet<ByteArrayWrapper>();
    for(byte[] b : otherCap) {
      baw1.add(new ByteArrayWrapper(b));
    }
    
    Set<ByteArrayWrapper> baw2 = new HashSet<ByteArrayWrapper>();
    for(byte[] b : byteRepresentation) {
      baw2.add(new ByteArrayWrapper(b));
    }
    
    baw1.retainAll(baw2);
    
    for(ByteArrayWrapper baw : baw1) {
      result.add(baw.getData());
    }
    
    return result;
  }
  
  public int getNumCapabilities() {
    return byteRepresentation.size();
  }
  
  public List<byte[]> getByteBearerCapabilities() {
    return byteRepresentation;
  }
  
  public List<String> getStringBearerCapabilities() {
    List<String> stringBC = new ArrayList<String>();
    for(byte[] br : byteRepresentation) {
      stringBC.add(bytesToHex(br));
    }
    return stringBC;
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
