/* Ron: 
 * 
 * this object is a list that includes all the contacts as ProfileObjects 
 * the object is used for storing received contacts on the file system and reading them from the file when in offline mode
 */

package com.sprout.friendfinder.social;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.brickred.socialauth.Contact;

import android.content.Context;
import android.util.Log;

public class ContactsListObject extends ArrayList<ProfileObject> implements Serializable {

  private static String TAG = ContactsListObject.class.getSimpleName();
  private static boolean D = false;
  
  private static String filename = "contacts";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ContactsListObject() {
		
	}
	
	public ContactsListObject(List<Contact> contactList) {
	  Log.d(TAG, "Number of Contacts: " + contactList.size());

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
	  
	  for (Contact c: contactList) {
      add(new ProfileObject(c.getFirstName(), c.getLastName(), c.getId()));
    }
	}
	
	public List<ProfileObject> getContactList() {
		return this;
	}
	
	public void writeObject(java.io.ObjectOutputStream out)
		       throws IOException {
		out.writeObject(this);
	}

	@SuppressWarnings("unchecked")
	public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

	  clear();

	  for( ProfileObject obj : (List<ProfileObject>) in.readObject() ) {
	    add(obj);
	  }
	}
	
	public void save(Context context) throws IOException {
    FileOutputStream fileOutput = context.openFileOutput(filename, Context.MODE_PRIVATE);
    ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
    writeObject(objectOutput);
    objectOutput.close();
    Log.d(TAG, "Contacts saved");
	}
}
