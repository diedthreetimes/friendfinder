package com.sprout.friendfinder.test.crypto.bearercap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.skjegstad.utils.BloomFilter;
import com.sprout.friendfinder.crypto.protocols.bearercap.BearerCapabilities;
import com.sprout.friendfinder.crypto.protocols.bearercap.BloomFilterBearerCapabilities;
import com.sprout.friendfinder.crypto.protocols.bearercap.FullBearerCapabilities;

import android.test.AndroidTestCase;

public class BearerCapTest extends AndroidTestCase {

  BearerCapabilities full1;
  BearerCapabilities full2;
  BearerCapabilities bfCap1;
  BearerCapabilities bfCap2;
  double FP = 0.001;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    List<byte[]> l1 = new ArrayList<byte[]>();
    l1.add("0".getBytes());
    l1.add("1".getBytes());
    l1.add("2".getBytes());
    l1.add("3".getBytes());
    l1.add("4".getBytes());
    full1 = new FullBearerCapabilities(l1);

    List<byte[]> l2 = new ArrayList<byte[]>();
    l2.add("5".getBytes());
    l2.add("1".getBytes());
    l2.add("2".getBytes());
    l2.add("6".getBytes());
    l2.add("7".getBytes());
    BloomFilter<String> bf = new BloomFilter<String>(FP, l2.size());
    for(byte[] e : l2) {
      bf.add(e);
    }
    bfCap1 = new BloomFilterBearerCapabilities(bf);

    List<byte[]> l3 = new ArrayList<byte[]>();
    l3.add("8".getBytes());
    l3.add("1".getBytes());
    l3.add("222".getBytes());
    l3.add("9".getBytes());
    l3.add("10".getBytes());
    l3.add("11".getBytes());
    full2 = new FullBearerCapabilities(l3);

    List<byte[]> l4 = new ArrayList<byte[]>();
    l4.add("12".getBytes());
    l4.add("1".getBytes());
    l4.add("2".getBytes());
    l4.add("13".getBytes());
    l4.add("14".getBytes());
    l4.add("15".getBytes());
    l4.add("16".getBytes());
    BloomFilter<String> bf4 = new BloomFilter<String>(FP, l4.size());
    for(byte[] e : l4) {
      bf.add(e);
    }
    bfCap2 = new BloomFilterBearerCapabilities(bf4);
    
  }
  
  public void testIntersect() {
    List<byte[]> out = full1.intersect(full2);
    assertEquals(1, out.size());
    assertTrue(Arrays.equals("1".getBytes(), out.get(0)));

    List<byte[]> out2 = full1.intersect(bfCap1);
    assertEquals(2, out2.size());

    List<byte[]> out3 = bfCap1.intersect(full1);
    assertEquals(2, out3.size());

    List<byte[]> out4 = bfCap1.intersect(bfCap2);
    assertEquals(null, out4);
  }
}
