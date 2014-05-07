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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.brickred.socialauth.Contact;

import com.sprout.friendfinder.crypto.AuthorizationObject;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ContactsListObject extends ArrayList<ProfileObject> implements Serializable {

  private static String TAG = ContactsListObject.class.getSimpleName();
  private static boolean D = true;
  
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

	  Log.d(TAG, "Your " + contactList.size() + " contacts have been downloaded");
	  
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
	

	// TODO: This shoudln't be done here, but until we integrate the server, we need it.
  
  BigInteger p = new BigInteger("00dba2d30dfc225ffcd894015d8971" +
      "6c2693e7d35c051670eb850337a41f" + 
      "719855ebc0839747651487a4f178cd" +
      "3f5c17cccb66f7baa8f8f54c3c2021" + 
      "9a95f37f41", 16);
  BigInteger q = new BigInteger("00d10e691f38413dc6ca084a403059" +
      "de7934422b44436ffc8b4b35572e24" +  
      "e5df78615bfabc7251f1e050bb5a75" +
      "598e0d957c9ae96457442a43db9130" +
      "4c64d11e9b",16);
  BigInteger N = p.multiply(q);
  BigInteger e = BigInteger.valueOf(3);
  BigInteger d = e.modInverse(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)));
  
  public void saveAuthorization(Context context) throws IOException {
    List<BigInteger> ais = new ArrayList<BigInteger>();
    BigInteger Rc  = randomRange(N);
    
    
    for (ProfileObject c : this) {
      ais.add(hash(c.getId().getBytes(), (byte)0).modPow(Rc, N));
    }
    
    BigInteger auth = BigInteger.ONE;
    for (BigInteger ai : ais) {
      auth = auth.multiply(ai).mod(N); 
    }
    //hash&sign:
    auth = hash(auth.toByteArray(), (byte)0).mod(N);
    auth = auth.modPow(d, N);
    Log.d(TAG, "Auth(a): " + auth.toString());

  
  
    AuthorizationObject authObj = new AuthorizationObject(Rc, auth, N, e);
    String authfilename = "authorization";

    FileOutputStream fileOutput = context.openFileOutput(authfilename, Context.MODE_PRIVATE);
    ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
    authObj.writeObject(objectOutput);
    objectOutput.close();
    Log.d(TAG, "Authorization saved");
  }
  

	protected BigInteger hash(byte [] message, byte selector){

	  // input = selector | message
	  byte [] input = new byte[message.length + 1];
	  System.arraycopy(message, 0, input, 1, message.length);
	  input[0] = selector;

	  MessageDigest digest = null;
	  try {
	    digest = MessageDigest.getInstance("SHA-1");
	  } catch (NoSuchAlgorithmException e1) {
	    Log.e("Crypto", "SHA-1 is not supported");
	    return null; // TODO: Raise an error?
	  }
	  digest.reset();

	  return new BigInteger(digest.digest(input));
	}


	//this is not the right place for the functionality:
	protected BigInteger randomRange(BigInteger range){
	  //TODO: Is there anything else we should fall back on here perhaps openssl bn_range
	  //         another option is using an AES based key generator (the only algorithim supported by android)

	  // TODO: Should we be keeping this rand around? 
	  SecureRandom rand = new SecureRandom();
	  BigInteger temp = new BigInteger(range.bitLength(), rand);
	  while(temp.compareTo(range) >= 0 || temp.equals(BigInteger.ZERO)){
	    temp = new BigInteger(range.bitLength(), rand);
	  }
	  return temp;

	}
	
}
