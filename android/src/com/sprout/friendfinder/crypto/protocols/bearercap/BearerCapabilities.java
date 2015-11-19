package com.sprout.friendfinder.crypto.protocols.bearercap;

import java.util.List;

/**
 * BearerCapabilities classes
 * @author root
 *
 */
public abstract class BearerCapabilities {
  public abstract List<byte[]> intersect(BearerCapabilities others);
}
