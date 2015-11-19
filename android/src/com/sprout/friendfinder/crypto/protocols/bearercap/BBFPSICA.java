package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.io.Serializable;


import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.brickred.socialauth.util.Base64;

import android.util.Log;

import com.skjegstad.utils.BloomFilter;
import com.sprout.finderlib.communication.CommunicationService;

/**
 * Bearer Bloomfilter PSICA with verification
 * @author root
 *
 */
public class BBFPSICA extends AbstractBearerCapProtocol {

  private final double BF_FALSE_POSITIVE = 0.0001;
  
  protected BBFPSICA(CommunicationService s, List<BigInteger> caps, boolean shuffle) {
    super(s, caps, shuffle);
  }
  
  public BBFPSICA(CommunicationService s, List<BigInteger> caps) {
    super(s, caps, true);
  }

  @Override
  protected void sendServerBearerCapabilities(CommunicationService s, BearerCapabilities serverBearerCapabilities) {
    if(serverBearerCapabilities instanceof BloomFilterBearerCapabilities) {
      //TODO: throw an exception
      Log.w(TAG, "Really weird bearer cap of server shouldnt be BF but i'll let you pass anway");
      BloomFilterBearerCapabilities bfbc = (BloomFilterBearerCapabilities) serverBearerCapabilities;
      s.write(Base64.encodeObject(bfbc.getBloomFilter()));
    } else {
      FullBearerCapabilities fbc = (FullBearerCapabilities) serverBearerCapabilities;
      s.write(Base64.encodeObject(fbc.compress(BF_FALSE_POSITIVE).getBloomFilter()));
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected BearerCapabilities receiveClientBearerCapabilities(CommunicationService s) {
    // receive bloom filter
    BloomFilter<String> clientBf = (BloomFilter<String>) Base64.decodeToObject(s.readString());
    return new BloomFilterBearerCapabilities(clientBf);
  }

  @Override
  protected List<String> verification(CommunicationService s, List<byte[]> result) {
    // verify intersection results
    // TODO: is it secure?
    List<String> hashResult = new ArrayList<String>();
    for(byte[] bearerCap : result) {
      hashResult.add(hash(bearerCap, (byte) 0).toString(16));
    }
    Collections.shuffle(hashResult, new SecureRandom());
    s.write(Base64.encodeObject((Serializable) hashResult));
    @SuppressWarnings("unchecked")
    List<String> theirResult = (ArrayList<String>) Base64.decodeToObject(s.readString());
    hashResult.retainAll(theirResult);
    Log.i(TAG, "Number of intersection after verification: "+hashResult.size());
    // psica doesnt know actual userid, so dont matter what to return, only care about the size
    return new ArrayList<String>(hashResult);
  }
  
}
