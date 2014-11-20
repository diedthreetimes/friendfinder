package com.sprout.friendfinder.models;

import java.math.BigInteger;

import android.util.Base64;

import com.activeandroid.serializer.TypeSerializer;


public class BigIntegerSerializer extends TypeSerializer {

  @Override
  public Class<?> getDeserializedType() {
    return BigInteger.class;
  }

  @Override
  public Class<?> getSerializedType() {
    return String.class;
  }

  // We encode using base 16. Base 64
  @Override
  public String serialize(Object data) {
    if (data == null) {
      return null;
    }

    return new String(Base64.encode(((BigInteger) data).toByteArray(), Base64.DEFAULT));
  }

  @Override
  public BigInteger deserialize(Object data) {
    if (data == null) {
      return null;
    }
    
    return new BigInteger(Base64.decode(((String) data).getBytes(), Base64.DEFAULT));
  }
}
