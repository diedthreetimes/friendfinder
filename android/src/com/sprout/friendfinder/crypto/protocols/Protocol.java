package com.sprout.friendfinder.crypto.protocols;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;

/**
 * interface protocol
 * @author root
 *
 */
public interface Protocol {

  /**
   * 
   * @return compatible AuthorizationObjectType
   */
  public AuthorizationObjectType getAuthType();
  
  /**
   * Run the test for this specific protocol
   * @param cs
   * @param callback
   * @param auth
   */
  public void runTest(CommunicationService cs, ProtocolCallback callback, AuthorizationObject auth);
  
  /**
   * set configuration of number of trials and benchmark
   * @param numTrials
   * @param benchmark
   */
  public void config(int numTrials, boolean benchmark);
  
// TODO: do we need this?
//  public ProtocolType getProtocolType();
  
}
