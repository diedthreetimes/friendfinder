/* Ron: 
 * 
 * this object is a list that includes all the contacts as ProfileObjects 
 * the object is used for storing received contacts on the file system and reading them from the file when in offline mode
 */

package com.sprout.friendfinder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContactsListObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<ProfileObject> contactList; 
	
	public ContactsListObject() {
		
	}
	
	public ContactsListObject(List<ProfileObject> list) {
		//this.contactList = new ArrayList<ProfileObject>();
		this.contactList = list;
	}
	
	public List<ProfileObject> getContactList() {
		return this.contactList;
	}
	
	public void writeObject(java.io.ObjectOutputStream out)
		       throws IOException {
		out.writeObject(this.contactList);
	}
	
	public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.contactList = (List<ProfileObject>) in.readObject();
	}

}
