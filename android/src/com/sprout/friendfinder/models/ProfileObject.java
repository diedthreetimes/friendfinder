package com.sprout.friendfinder.models;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.Profile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Handles storing information about a single profile.
 * Information is backed by a DB table via ActiveAndroid
 */
@Table(name = "profiles")
public class ProfileObject extends Model implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Column
	private String firstName;
	
	@Column
	private String lastName; 
	
	/* OSN id */
	@Column
	private String uid;
	
	@Column
  private String profileURL;
	
	@Column
	private String profileImageURL;
	
	@Column
	private boolean anonomous;
	
	@Column
	private ContactsListObject contacts;
	
	// TODO: Download and save img, and include local path here
	
	public ProfileObject() {
		super();
		
		anonomous = false;
	}
	
	public ProfileObject(Profile profile) {
	  super();
    this.firstName = profile.getFirstName();
    this.lastName = profile.getLastName();
    this.uid = profile.getValidatedId();
    this.profileImageURL = profile.getProfileImageURL();
    // this.profileURL = // TODO: To set this we need to extend social auth we can just retrieve it as needed later
    anonomous = false;
	}
	
	public ProfileObject(Contact c) {
	  super();

	  this.firstName = c.getFirstName();
	  this.lastName = c.getLastName();
	  this.uid = c.getId();
	  this.profileURL = c.getProfileUrl();
	  this.profileImageURL = c.getProfileImageURL();
	  
	  anonomous = false;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public String getUid() {
		return this.uid;
	}
	
	public String getProfileURL() {
	  return profileURL;
	}
	
	public String getProfileImageURL() {
	  return profileImageURL;
	}
	
	public List<Interaction> interactions() {
    return getMany(Interaction.class, "Profile");
  }
	
	public void setContacts(ContactsListObject contacts) {
	  this.contacts = contacts;
	}
	
	public ContactsListObject contacts() {
	  // Should we do anything special if no contacts have been found yet?
	  return contacts;
	}
	
	/* These are unused but left in order to eventually customize serialized output.
	 *      To do this we must change the signatures to be private
	public void writeObject(java.io.ObjectOutputStream out)
		       throws IOException {
		out.writeObject(this.firstName);
		out.writeObject(this.lastName);
		out.writeObject(this.uid);
		out.writeObject(this.profileImageURL);
		out.writeObject(this.profileURL);
		out.writeObject(this.anonomous);
	}
	
	public ProfileObject readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.firstName = (String) in.readObject();
		this.lastName = (String) in.readObject();
		this.uid = (String) in.readObject();
		this.profileImageURL = (String) in.readObject();
		this.profileURL = (String) in.readObject();
		this.anonomous = (Boolean) in.readObject();
		return this;
	} */

	public String getDisplayName() {
		return firstName + " " + lastName;
	}
	
	@Override
	public String toString() {
	  return "\""+getDisplayName() + "\":" + getUid();
	}
	
	@Override
	public boolean equals(Object other) {
	  if (other instanceof ProfileObject) {
	    return ((ProfileObject) other).getUid().equals(getUid());
	  }
	  return false;
	}
	
	@Override
	public int hashCode() {
	  return getUid().hashCode();
	}

	// Note this shouldn't be called on the main thread.
  public Drawable getProfileImage(Context context) throws IOException {
    // For now just convert the URL to a drawable
    // Eventually we !need! To cache these!
    // TODO: Load these images form a cache (if not in the cache and disconnected display the default icon)
    // http://developer.android.com/training/efficient-downloads/redundant_redundant.html
    
    Log.e("ProfileObject", "Picture url" + getProfileImageURL());
    
    HttpURLConnection connection = (HttpURLConnection) new URL(getProfileImageURL()).openConnection();
    connection.connect();
    InputStream input = connection.getInputStream();

    return new BitmapDrawable(context.getResources(), BitmapFactory.decodeStream(input));
  }

}
