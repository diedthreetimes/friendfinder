package com.sprout.friendfinder.common;

/**
 * A small class to keep all configuration in a consistent place.
 * 
 * This also facilitates changes to configurations necessary for local operation and benchmarking.
 */
public class Config {
  
  private static boolean benchmark = false;
  private static boolean local = false;
  
  public static boolean getBenchmark() {
    return benchmark;
  }
  
  public static String getHost() {
    if (local) { 
      return "localhost:3001";
    } else {
      return "344ee97c.ngrok.com:80";
    }
  }
  
  public static String getContactJsonUrl() {
    if (benchmark) {
      return "/authority/test_connections.json";
    } else { 
      return "/authority/download_connections.json";
    }
  }
}
