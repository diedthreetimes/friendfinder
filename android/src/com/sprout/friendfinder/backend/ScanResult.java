package com.sprout.friendfinder.backend;

import java.util.HashSet;
import java.util.Set;

import com.sprout.finderlib.communication.Device;

/**
 * This class holds all devices that where discovered in a single broadcast scan.
 * 
 * This is useful for determining active connections
 */
public class ScanResult extends HashSet<Device> {
  private static final long serialVersionUID = 1L;
  
  // Methods we use at the moment. 
  // add
  // iterator()
  
  // Perhaps better to hide the use of HashSet. atm this is nothing more than a typedef
  
  public Set<String> getAddresses() {
	  Set<String> addrs = new HashSet<String>();
	  for(Device d : this) {
		  if(d.getAddress() != null) addrs.add(d.getAddress());
	  }
	  return addrs;
  }
}
