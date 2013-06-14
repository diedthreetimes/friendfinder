/*
 * Ron: The CA's public key (N, e), the random number Rc as well as the authorization of the set of contacts by CA are put into this object in order to store it on the user's device and read it later on (during offline phase) when the PSI shall be done
 */

package com.sprout.friendfinder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public class AuthorizationObject implements Serializable {
	private BigInteger R;
	private BigInteger auth;
	private BigInteger N, e;
	
	public AuthorizationObject() {
		
	}
	
	public AuthorizationObject(BigInteger R, BigInteger auth, BigInteger N, BigInteger e) {
		this.R = R;
		this.auth = auth;
		this.N = N;
		this.e = e;
	}
	
	public void writeObject(java.io.ObjectOutputStream out)
		       throws IOException {
		out.writeObject(this.R);
		out.writeObject(this.auth);
		out.writeObject(this.N);
		out.writeObject(this.e);
	}
	
	public AuthorizationObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.R = (BigInteger) in.readObject();
		this.auth = (BigInteger) in.readObject();
		this.N = (BigInteger) in.readObject();
		this.e = (BigInteger) in.readObject();
		return this;
	}
	
	public BigInteger getR() {
		return this.R;
	}
	
	public BigInteger getAuth() {
		return this.auth;
	}
	
	public BigInteger getN() {
		return this.N;
	}
	
	public BigInteger gete() {
		return this.e;
	}
}
