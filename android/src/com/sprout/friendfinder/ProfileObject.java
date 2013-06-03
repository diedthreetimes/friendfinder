/* Ron
 * handle the user's own profile information (like e.g. name, education, etc.) here
 * store object after receiving information from linkedin to file
 * read object from file in offline mode before doing the PSI
 */

package com.sprout.friendfinder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ProfileObject implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String firstName;
	private String lastName; 
	private String id;
	
	ProfileObject() {
		
	}
	
	ProfileObject(String firstName, String lastName, String id) {
		this.firstName = firstName;
		this.lastName = lastName; 
		this.id = id;
	}
	
	String getFirstName() {
		return this.firstName;
	}
	
	String getLastName() {
		return this.lastName;
	}
	
	String getId() {
		return this.id;
	}
	
	public void writeObject(java.io.ObjectOutputStream out)
		       throws IOException {
		out.writeObject(this.firstName);
		out.writeObject(this.lastName);
		out.writeObject(this.id);
	}
	
	public ProfileObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.firstName = (String) in.readObject();
		this.lastName = (String) in.readObject();
		this.id = (String) in.readObject();
		return this;
	}

}
