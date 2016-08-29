package com.sprout.friendfinder.crypto.protocols;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

public class ProtocolMessaging implements Protocol {
  
  public String deviceAddress = null;
  
  public ProtocolMessaging(Device connectedDevice) {
    deviceAddress = connectedDevice.getAddress();
  }

  @Override
  public AuthorizationObjectType getAuthType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth) {
    new MessageProtocol(cs, deviceAddress, callback).execute();
  }

  @Override
  public void config(int numTrials, boolean benchmark) {
    // dont matter for now
    
  }

}
