/*
 * Ron: The CA's public key (N, e), the random number Rc as well as the authorization of the set of contacts by CA are put into this object in order to store it on the user's device and read it later on (during offline phase) when the PSI shall be done
 */

package com.sprout.friendfinder.crypto;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class AuthorizationObject implements Serializable {
  private static final long serialVersionUID = -8723688620635455716L;

  private static final String TAG = AuthorizationObject.class.getSimpleName();
  private static final boolean D = true;
  

  /**
   * Number of seconds a signature should be considered valid
   */
  private static final int VALID_SECONDS = 7*24*60*60;

  private BigInteger R;
  private BigInteger auth;

  private static String defaultFilename = "authorization";

  // TODO: There should be a static load function instead of an empty constructor
  public AuthorizationObject() {
    
  }
  
  public AuthorizationObject(String response) {

    
  }

  /*
  public AuthorizationObject(BigInteger R, BigInteger auth) {
    this.R = R;
    this.auth = auth;
  }*/


  public boolean verify() {
    // We may want a way to verify arbitrary data. In case the signature is detached. 
    
    // TODO: Validate the time


    return true;
  }

  public void save(Context context) throws IOException {
    save(context, defaultFilename);
  }

  public void save(Context context, String filename) throws IOException {
    FileOutputStream fileOutput = context.openFileOutput(filename, Context.MODE_PRIVATE);
    ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
    writeObject(objectOutput);
    objectOutput.close();
    Log.d(TAG, "Authorization saved");
  }

  public void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    out.writeObject(this.R);
    out.writeObject(this.auth);
  }

  public AuthorizationObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.R = (BigInteger) in.readObject();
    this.auth = (BigInteger) in.readObject();
    return this;
  }

  public BigInteger getR() {
    return this.R;
  }

  public BigInteger getAuth() {
    return this.auth;
  }

  public List<BigInteger> getData () {
    // decode the encoded data object if necessary
    return null;
  }
}
