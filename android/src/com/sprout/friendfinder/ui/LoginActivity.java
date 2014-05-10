package com.sprout.friendfinder.ui;

import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;

public class LoginActivity extends Activity {

  private static final String TAG = LoginActivity.class.getSimpleName();
  private static final boolean D = true;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if(D) Log.i(TAG, "OnCreate");

    SocialAuthAdapter adapter = new SocialAuthAdapter(new DialogListener() {
      @Override
      public void onComplete(Bundle values) {
        if(D) Log.d(TAG, "Authorization successful");

        Intent intent = new Intent(LoginActivity.this, DiscoveryService.class);
        intent.setAction(DiscoveryService.ACTION_RESTART);
        startService(intent);

        finish();
      }

      // TODO: What do we do if this fails, or is canceled?
      // We probably need to display something to the user? Possibly remain in this activity?
      @Override
      public void onCancel() {
        if(D) Log.d(TAG, "Cancel called");

        finish();
      }     

      @Override
      public void onError(SocialAuthError er) {
        if(D) Log.d(TAG, "Error", er);

        finish();
      }

      @Override
      public void onBack(){
        if(D) Log.d(TAG, "BACK");

        finish();
      }
    });

    adapter.addProvider(Provider.LINKEDIN, R.drawable.linkedin);
    adapter.authorize(this, Provider.LINKEDIN);
  }

}
