/* Ron
 * here we handle the retrieving of the user's own profile information form Linkedin (i.e. the callback happened)
 * store the info in an object in a file
 */

package com.sprout.friendfinder.social;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;

import android.content.Context;
import android.util.Log;



public final class SocialProfileListener implements SocialAuthListener<org.brickred.socialauth.Profile> {

	private static final String TAG = SocialProfileListener.class.getSimpleName();
	
	private Context mActivity;
	
	public SocialProfileListener(Context act) {
		mActivity = act;
	}
	
	@Override
	public void onError(SocialAuthError arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onExecute(String provider, org.brickred.socialauth.Profile profile) {
	  // TODO: profile could be null here!
	  
		Log.d(TAG, "callback happened");
		
		Log.d(TAG, "Your profile: \n");
		Log.d(TAG, "Your name: " + profile.getFirstName() + " " + profile.getLastName());
		//Toast.makeText(mActivity, "Your profile information is: " + profile.getFirstName() + " " + profile.getLastName(), Toast.LENGTH_SHORT).show();
		
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
			Log.d(TAG, "Profile saved");
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
	}

}


