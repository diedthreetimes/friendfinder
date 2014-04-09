/* Ron
 * here we handle the contact download from Linkedin (i.e. callback happened)
 * for test purposes, we also do the CA's certification of the contact set here (this should be done somewhere else in the future!)
 *  
 */

package com.sprout.friendfinder.social;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;
import org.json.JSONObject;

import com.sprout.friendfinder.crypto.AuthorizationObject;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.JsonReader;
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
		
		Log.d(TAG, "Your " + contactList.size() + " contacts have been downloaded");
		Toast.makeText(mActivity, "Your " + contactList.size() + " contacts have been downloaded", Toast.LENGTH_SHORT).show();
		
		
		 
		//later on: get the certification of the contact list from the CA; for now: certify here for test purposes
		//Ron., 15. Jul.: which key was used on the server-side? public key needs to be included here
				
		BigInteger N,e;
			
		/*
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
		*/
				
				//Ron, 6.8.: use the new CA's public key:
				String nString = "0282010100CE08801F12CD287F1ED1A6C66CA46A62F8C01E343320F64DB7DA99DFEBAEF49D3BDDCC723597B687C5C11343289A1FCCAD0B60C9DDE3D6364981DAF334617185C48914EEDD8E5FE0909E33BDB8255E2613936F014A73FE453E739B5E4A83A578D49D1B79C8FAFAB474E14C004FE6C13F594CC55EC5FB8F017C82CE1E4EF13D1E63C717D6BD6D93D0D61914C992621C86FB8A66E793934154F392CEA183D8D93CFEF6955C9919F47441BE25DA1E0EDE2BC83BDB4A8896B772A220F9E9DAD5C462C53E1CDD7CCC18F6530DE369F22CD99F5EEA75660A2035170B797D907AF9756509574A8473E726575F5A344C51797CB7FA05B51BB1BAF1A47FB60F1F17C33D01";
				N = new BigInteger(nString, 16); 
				e = BigInteger.valueOf(65537);
				
				//d = e.modInverse(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)));
				
				//get the secrets from server instead, Ron. 15. Jul: (done further down in the code now)
				//BigInteger Rc  = randomRange(N);
		List<BigInteger> ais = new ArrayList<BigInteger>(); // The set {a1,a2,...,am}
		
		
		
		//Ron, 15. Jul: fetch authorization from server (instead of above code):
		InputStream serverInput = null;
		String serverResult = null;
		JSONObject serverArray = null;
		JSONObject psiMessage = null;
		
		//retrieve data:
		try {
			Log.d(TAG, "Trying to connect to CA...");
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet("http://athena.ics.uci.edu/authority/download_profile.json");
			HttpResponse response = httpClient.execute(httpGet);
			
			int statusCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "Status Code CA: " + statusCode);
			HttpEntity entity = response.getEntity();
			serverInput = entity.getContent();
			Log.d(TAG, "Connected to CA");
			
		} catch (Exception e2) {
			Log.d("SocialContactListner", "Connection error to LinkedIn CA: " + e2.toString());
		}
		
		//convert response:
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(serverInput));
			StringBuilder stringBuilder = new StringBuilder();
			
			
			String line = null;
			Log.d(TAG, "*** CA response: ***");
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
				Log.d(TAG, line);
			}
			Log.d(TAG, "******");
			serverInput.close();
			serverResult = stringBuilder.toString();
		} catch (Exception e3) {
			Log.d("SocialContactListener", "Converting error: " + e3.toString());
		}
		
		//parse:
		try {
			serverArray = new JSONObject(serverResult);
		} catch (Exception e4) {
			Log.d("SocialContactListener", "Parsing error: " + e4.toString());
		}
		
		//get element:
		try {
			psiMessage = serverArray.getJSONObject("psi_message");
			
		} catch (Exception e5) {
			Log.d("SocialContactListener", "Parsing error:" + e5.toString());
		}
		
		//Ron, 15. Jul, end of fetching authorization


		
		
		//save contacts to file
		List<ProfileObject> contacts = new ArrayList<ProfileObject>();
		ProfileObject profile; 
		for (Contact c: contactList) {
			profile = new ProfileObject(c.getFirstName(), c.getLastName(), c.getId());
			contacts.add(profile);
						
			//Ron, 15. Jul.: instead of computing ais here, get them from the server (actually, we don't need to fetch them here as we can compute them later on during the PSI protocol):
			//ais.add(hash(c.getId().getBytes(), (byte)0).modPow(Rc, N));
			
			
		}
		
		
		/*
		 * Ron, 15. Jul.: get the authorization from the server instead
		 * warning: the hashing on the server-side might have been done different from here (and thus, the verification in the PSI protocol might be needed to be modified?)
		 
		//compute hash of product of ais instead of their concatenation
		BigInteger auth = BigInteger.ONE;
		for (BigInteger ai : ais) {
			auth = auth.multiply(ai).mod(N); 
		}
		//hash&sign:
		auth = hash(auth.toByteArray(), (byte)0).mod(N);
		auth = auth.modPow(d, N);
		Log.d(TAG, "Auth(a): " + auth.toString());
		
		*/
		BigInteger auth = null; 
		String authString;
		try {
			authString = psiMessage.getString("sig");
			//convert the base64-encoded string into BigInteger:
			auth = new BigInteger(Base64.decode(authString, Base64.DEFAULT));
		} catch (Exception e6) {
			Log.d(TAG, "Signature could not be retrieved: " + e6.toString());
		}
		
		//Ron., 15.Jul: retrieve the secret exponent from server now as well and save it as Rc:
		BigInteger Rc = null;
		String secretsString; 
		try {
			secretsString = psiMessage.getString("secrets");
			Rc = new BigInteger(Base64.decode(secretsString, Base64.DEFAULT));
		} catch (Exception e7) {
			Log.d(TAG, "Secret Rc could not be retrieved: " + e7.toString());
		}
		
		
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
	//Ron, 15. Jul.: not needed any longer as we get the authorization from the server:
	
	/*
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

*/
	
	
}
