/* Ron
 * here we handle the contact download from Linkedin (i.e. callback happened)
 * for test purposes, we also do the CA's certification of the contact set here (this should be done somewhere else in the future!)
 *  
 */

package com.sprout.friendfinder;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;


public final class SocialContactListener implements SocialAuthListener<List<Contact>> { 

	private final String TAG = "SocialContactListener";
	
private Activity mActivity;
	
	public SocialContactListener(Activity act) {
		mActivity = act;
	}
	
	@Override
	public void onError(SocialAuthError arg0) {
		// TODO Auto-generated method stub
		
	}
	


	@Override
	public void onExecute(List<Contact> contactList) {
		
		Log.d("Social Contact Listener", "callback happened");
		Log.d("Social Contact Listener", "Number of Contacts: " + contactList.size());
		
		for (Contact p : contactList) {
			Log.d("Social Contact Listener", "First Name: " + p.getFirstName());
			Log.d("Social Contact Listener", "Last Name: " + p.getLastName());
			Log.d("Social Contact Listener", "ID: " + p.getId() + "\n");
		}
		
		Toast.makeText(mActivity, "Your " + contactList.size() + " contacts have been downloaded", Toast.LENGTH_SHORT).show();
		
		
		
		//later on: get the certification of the contact list from the CA; for now: certify here for test purposes
				BigInteger N,e;
				BigInteger p,q,d;
				p = new BigInteger("00dba2d30dfc225ffcd894015d8971" +
						"6c2693e7d35c051670eb850337a41f" + 
				        "719855ebc0839747651487a4f178cd" +
				        "3f5c17cccb66f7baa8f8f54c3c2021" + 
				        "9a95f37f41", 16);
				q = new BigInteger("00d10e691f38413dc6ca084a403059" +
						"de7934422b44436ffc8b4b35572e24" + 	
						"e5df78615bfabc7251f1e050bb5a75" +
						"598e0d957c9ae96457442a43db9130" +
						"4c64d11e9b",16);
				N = p.multiply(q);
				e = BigInteger.valueOf(3);
				d = e.modInverse(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)));
				
				BigInteger Rc  = randomRange(N);
		List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,am}
		
		 // t = (p-1)/q to hash into the group Z*p
	    //BigInteger t;
	    //t = (p.subtract(BigInteger.ONE).divide(q));
		
		
		//save contacts to file
		List<ProfileObject> contacts = new ArrayList<ProfileObject>();
		ProfileObject profile; 
		for (Contact c: contactList) {
			profile = new ProfileObject(c.getFirstName(), c.getLastName(), c.getId());
			contacts.add(profile);
			
			//ais.add(hash(c.getId().getBytes(), (byte)0).mod(p).modPow(t, p).modPow(Rc, p)); //right?
			ais.add(hash(c.getId().getBytes(), (byte)0).modPow(Rc, N));
		}
		//Log.d(TAG, "Test**CA: " + ais.get(0).toString());
		
		//just for now: compute hash of product of ais instead of their concatenation
		BigInteger auth = BigInteger.ONE;
		for (BigInteger ai : ais) {
			auth = auth.multiply(ai).mod(N); //N instead of p
		}
		//hash&sign:
		auth = hash(auth.toByteArray(), (byte)0).mod(N);
		auth = auth.modPow(d, N);
		Log.d(TAG, "Auth(a): " + auth.toString());
		
		ContactsListObject clo = new ContactsListObject(contacts);
		
		String filename = "contacts";
		
		FileOutputStream fileOutput;
		ObjectOutputStream objectOutput; 
		
		try {
			fileOutput = mActivity.openFileOutput(filename, Context.MODE_PRIVATE);
			objectOutput = new ObjectOutputStream(fileOutput);
			clo.writeObject(objectOutput);
			objectOutput.close();
			Log.d("Social Contact Listener", "Contacts saved");
		} catch (Exception e1) {
			Log.d("Social Contact Listener", e1.toString());
		}
		
		//Toast.makeText(mActivity, "Contact list saved", Toast.LENGTH_SHORT).show();
		
		
		
		//just save the authorization data to a file as well (as it is needed later on during the PSI)
		AuthorizationObject authObj = new AuthorizationObject(Rc, auth, N, e);
		filename = "authorization";
		try {
			fileOutput = mActivity.openFileOutput(filename, Context.MODE_PRIVATE);
			objectOutput = new ObjectOutputStream(fileOutput);
			authObj.writeObject(objectOutput);
			objectOutput.close();
			Log.d("Social Contact Listener", "Authorization saved");
		} catch (Exception e1) {
			Log.d("Social Contact Listener", e1.toString());
		}
	}
	
	//this is not the right place for the functionality:
protected BigInteger hash(byte [] message, byte selector){
		
		// input = selector | message
		byte [] input = new byte[message.length + 1];
		System.arraycopy(message, 0, input, 1, message.length);
		input[0] = selector;
		
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-1");
    	} catch (NoSuchAlgorithmException e1) {
    		Log.e(TAG, "SHA-1 is not supported");
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
