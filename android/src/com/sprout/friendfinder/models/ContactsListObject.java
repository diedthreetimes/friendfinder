package com.sprout.friendfinder.models;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.brickred.socialauth.Contact;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import android.util.Log;

/**
 * This object maintains lists of contacts, and saves them to the DB.
 * 
 * This includes the device owners contacts, but also known results of any interaction.
 *
 */
@Table(name="contacts_lists")
public class ContactsListObject extends Model implements Serializable {

  private static String TAG = ContactsListObject.class.getSimpleName();
  private static boolean D = false;
  
	private static final long serialVersionUID = 1L;
	
	public ContactsListObject() {
		super();
	}
	
	public ContactsListObject(List<Contact> contactList) {
	  super();
	  
	  for (Iterator<Contact> it = contactList.iterator(); it.hasNext(); ) {
	    Contact p = it.next();

	    if(p.getFirstName().equals("private")) {
	      it.remove();
	    } else {
	      if(D) Log.d(TAG, "First Name: " + p.getFirstName());
	      if(D) Log.d(TAG, "Last Name: " + p.getLastName());
	      if(D) Log.d(TAG, "ID: " + p.getId() + "\n");
	    }
	  }
	  
	  Log.d(TAG, "Number of Contacts: " + contactList.size());
	  
    save(); // We need an id to put into the other table
    
    ActiveAndroid.beginTransaction();
    try {
      for (Contact c: contactList) {
        ProfileObject profile = new ProfileObject(c);
        profile.save();
        ContactsListsProfiles clp = new ContactsListsProfiles();
        clp.list = this;
        clp.contact = profile;
        clp.save();
      }
      ActiveAndroid.setTransactionSuccessful();
    }
    finally {
      ActiveAndroid.endTransaction();
    } 
	  
	}
	
	public void put(String profileUid) {
	  ProfileObject profile = new Select().from(ProfileObject.class).where("uid=?", profileUid).executeSingle();
	  
	  if (profile == null) {
	    Log.e(TAG, "Profile with uid:"+profileUid+" not found. Not added.");
	    return;
	  }
	  
	  put(profile);
	}
	
	/**
	 * Add profile to this list. 
	 * 
	 * Note: profile must be saved already
	 * @param profile
	 */
	public void put(ProfileObject profile) {
	  ContactsListsProfiles clp = new ContactsListsProfiles();
	  clp.list = this;
	  clp.contact = profile;
	  clp.save();
	}
	
	public List<ProfileObject> getContactList() {
	  return new Select()
	   .from(ProfileObject.class)
	   .innerJoin(ContactsListsProfiles.class).on("profiles.id = contacts_lists_profiles.contact")
	   .where("contacts_lists_profiles.list = ?", getId())
	   .orderBy("profiles.firstName")
	   .execute();
	}

	// None of these TODOs are immediately needed, but add functionality that could be useful in the future
	// TODO: implement dependent save
	// TODO: Implement get owningProfile (whose contact list is this) (this should be a lookup through profile)
}
