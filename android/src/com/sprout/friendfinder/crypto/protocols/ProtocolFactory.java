package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import com.activeandroid.util.Log;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager.ProtocolType;
import com.sprout.friendfinder.models.ProfileObject;

public class ProtocolFactory {
  
  static final String TAG = ProtocolFactory.class.getSimpleName();

  public static Protocol getProtocol(String protocolName, String[] friends) {
    return getProtocol(ProtocolType.getProtocolType(protocolName), friends);
    
  }
  
  public static Protocol getProtocol(ProtocolType type, List<ProfileObject> contactList, Device connectedDevice) {
    if(type.equals(ProtocolType.MSG)) {
      return new ProtocolMessaging(connectedDevice);
    } else return getProtocol(type, contactList);
  }
  
  public static Protocol getProtocol(ProtocolType type, List<ProfileObject> contactList) {

    String[] input = new String[contactList.size()];
    for( int i=0; i < contactList.size(); i++) {
      input[i] = contactList.get(i).getUid();
    }
    return getProtocol(type, input);
  }

  public static Protocol getProtocol(ProtocolType type, String[] friends) {
    
    Log.i(TAG, "Getting protocol "+type.toString());
    if(type.equals(ProtocolType.ATWPSI)) {
      return new ProtocolATWPSI();
    } else if(type.equals(ProtocolType.ATWPSICA)) {
      return new ProtocolATWPSICA();
    } else if(type.equals(ProtocolType.PSICA)) {
      return new ProtocolPSICA(friends);
    } else if(type.equals(ProtocolType.ATWPSICA_NEW)) {
      return new ProtocolATWPSICA_NEW();
    } else if(type.equals(ProtocolType.BBFPSICA)) {
      return new ProtocolBBFPSICA();
    } else if(type.equals(ProtocolType.BPSICA)) {
      return new ProtocolBPSICA();
    }  else if(type.equals(ProtocolType.BBFPSI)) {
      return new ProtocolBBFPSI();
    } else if(type.equals(ProtocolType.BPSI)) {
      return new ProtocolBPSI();
    } else if(type.equals(ProtocolType.BBFPSI_No_V)) {
      return new ProtocolBBFPSINoV();
    }
    
    Log.e(TAG, "Invalid protocol "+type.toString());
    
    return null;
  }
}
