package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.sprout.finderlib.communication.CommunicationService;

import android.util.Log;

/**
 * Bearer PSICA (no bloomfilter)
 * @author root
 *
 */
public class BPSICA extends AbstractBearerCapProtocol {
  
  static final String TAG = BPSICA.class.getSimpleName();
  
  public BPSICA(CommunicationService s, List<BigInteger> caps) {
    super(s, caps, true);
  }

  @Override
  protected void sendServerBearerCapabilities(CommunicationService s, BearerCapabilities serverBearerCapabilities) {
    if(!(serverBearerCapabilities instanceof FullBearerCapabilities)) {
      // TODO: what do we do here?
      Log.e(TAG, "cant send non-full bearer cap in BPSICA");
      return;
    }
    FullBearerCapabilities fbc = (FullBearerCapabilities) serverBearerCapabilities;
    s.write(fbc.getNumCapabilities());
    for(byte[] sbc : fbc.getByteBearerCapabilities()) {
      s.write(sbc);
    }
  }
  
  @Override
  protected BearerCapabilities receiveClientBearerCapabilities(CommunicationService s) {
    List<byte[]> clientBearerCapabilities = new ArrayList<byte[]>();
    int numCaps = s.readInt();
    for(int i=0; i<numCaps; i++) {
      clientBearerCapabilities.add(s.read());
    }
    Log.i(TAG, "num client caps: "+clientBearerCapabilities.size());
    return new FullBearerCapabilities(clientBearerCapabilities);
  }

  @Override
  protected List<String> verification(CommunicationService s, List<byte[]> result) {
    // TODO: return sth better? now return dummy..
    List<String> out = new ArrayList<String>();
    for(int i=0; i<result.size(); i++) {
      out.add("X");
    }
    return out;
  }
  
}
