/* Ron
 * handle the user's own profile information (like e.g. name, education, etc.) here
 * store object after receiving information from linkedin to file
 * read object from file in offline mode before doing the PSI
 */

package com.sprout.friendfinder.social;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.brickred.socialauth.Profile;

import android.content.Context;
import android.util.Log;

public class ProfileObject implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String firstName;
	private String lastName; 
	private String id;
	
	private static final String filename = "profile";
	
	public ProfileObject() {
		
	}
	
	public ProfileObject(Profile profile) {
    this.firstName = profile.getFirstName();
    this.lastName = profile.getLastName();
    this.id = profile.getValidatedId();
	}
	
	public ProfileObject(String firstName, String lastName, String id) {
		this.firstName = firstName;
		this.lastName = lastName; 
		this.id = id;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void save(Context context) throws IOException {
	  String filename = "profile";
    
    FileOutputStream fileOutput;
    ObjectOutputStream objectOutput; 
    
    fileOutput = context.openFileOutput(filename, Context.MODE_PRIVATE);
    objectOutput = new ObjectOutputStream(fileOutput);
    writeObject(objectOutput);
    objectOutput.close();
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

	public String getDisplayName() {
		return firstName + " " + lastName;
	}

}
