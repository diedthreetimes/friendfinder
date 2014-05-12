/*
 * Ron: The CA's public key (N, e), the random number Rc as well as the authorization of the set of contacts by CA are put into this object in order to store it on the user's device and read it later on (during offline phase) when the PSI shall be done
 */

package com.sprout.friendfinder.crypto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public class AuthorizationObject implements Serializable {
	private static final long serialVersionUID = -8723688620635455716L;
	

	/**
	 * Number of seconds a signature should be considered valid
	 */
	private static final int VALID_SECONDS = 7*24*60*60;
	
	private BigInteger R;
	private BigInteger auth;
	
	public AuthorizationObject() {
		
	}
	
	public AuthorizationObject(BigInteger R, BigInteger auth) {
		this.R = R;
		this.auth = auth;
	}
	
	
  public boolean verify() {
    // We may want a way to verify arbitrary data. In case the signature is detached. 
    
    
    return true;
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
