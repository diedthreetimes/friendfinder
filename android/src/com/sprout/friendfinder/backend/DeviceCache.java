package com.sprout.friendfinder.backend;

import java.util.HashMap;

import android.util.Log;

import com.sprout.finderlib.communication.Device;

// TODO: Find a way to persist the cache across reboots

public class DeviceCache {
  
  private static final String TAG = DeviceCache.class.getSimpleName();
  private static boolean D = true;

  private HashMap<Device, Long> cache;
  private long timeout;
  /**
   * Create a device cache, that invalidates address after timeout
   * @param timeout seconds
   */
  DeviceCache(int timeout) { 
    if (D) Log.d(TAG, "DeviceCache initialized with " + timeout + " second timeout.");
    cache = new HashMap<Device, Long>();
    this.timeout = timeout * 1000;
  }
  
  private long now() {
    return System.currentTimeMillis();
  }
  
  /**
   * 
   * @param device
   * @return true if device is in the cache
   */
  public boolean contains(Device device) {
    if (!cache.containsKey(device))
      return false;
    else {
      if (cache.get(device) < (now() - timeout)) {
        if (D) Log.d(TAG, "Device: " + device + " has expired");
        cache.remove(device);
        return false;
      }
      
      return true;
    }
  }
  
  /**
   * Add a device to the cache
   * @param device
   */
  public void add(Device device) {
    if (D) Log.d(TAG, "Device: " + device + " added to the cache");
    cache.put(device, now());
  }
  
  /**
   * Clear the cache
   */
  public void clear() {
    if (D) Log.d(TAG, "Clear...");
    cache.clear();
  }
}
