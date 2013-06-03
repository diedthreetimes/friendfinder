/* Ron
 * here we handle the retrieving of the user's own profile information form Linkedin (i.e. the callback happened)
 * store the info in an object in a file
 */

package com.sprout.friendfinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;

import android.app.Activity;
import android.content.Context;
import android.provider.ContactsContract.Profile;
import android.util.Log;
import android.widget.Toast;



public final class SocialProfileListener implements SocialAuthListener<org.brickred.socialauth.Profile> {

	private Activity mActivity;
	
	public SocialProfileListener(Activity act) {
		mActivity = act;
	}
	
	@Override
	public void onError(SocialAuthError arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onExecute(org.brickred.socialauth.Profile profile) {
		Log.d("Social Profile Listener", "callback happened");
		
		Log.d("Social Profile Listener", "Your profile: \n");
		Log.d("Social Profile Listener", "Your name: " + profile.getFirstName() + " " + profile.getLastName());
		Toast.makeText(mActivity, "Your profile information is: " + profile.getFirstName() + " " + profile.getLastName(), Toast.LENGTH_SHORT).show();
		
		ProfileObject myProfile = new ProfileObject(profile.getFirstName(), profile.getLastName(), profile.getValidatedId());
		

		//write to file (serialized object)
		String filename = "profile";
		
		FileOutputStream fileOutput;
		ObjectOutputStream objectOutput; 
		
		try {
			fileOutput = mActivity.openFileOutput(filename, Context.MODE_PRIVATE);
			objectOutput = new ObjectOutputStream(fileOutput);
			myProfile.writeObject(objectOutput);
			objectOutput.close();
			Log.d("Social Profile Listener", "Profile saved");
		} catch (Exception e) {
			Log.d("Social Profile Listener", e.toString());
		}
	}

}


