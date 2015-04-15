package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.ATWPSIProtocol;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.models.ContactsListObject;
import com.sprout.friendfinder.models.Interaction;

public class CommonFriendsTest extends ATWPSIProtocol {
  
  ProfileDownloadCallback callback;
  Interaction interaction;
  static final String TAG = CommonFriendsTest.class.getSimpleName();

  public CommonFriendsTest(CommunicationService s, AuthorizationObject authObject, ProfileDownloadCallback callback, Interaction interaction) {
    super(s, authObject);
    
    this.callback = callback;
    this.interaction = interaction;

    setBenchmark(Config.getBenchmark());
  }
  
  @Override
  public List<String> doInBackground(String... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "CommonFriendsProtocol failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the common friends protocol");
  }

  @Override
  public void onPostExecute(List<String> result) {
    Log.i(TAG, "Common friends protocol complete");
    
    if (result == null) {
      callback.onError();
      return;
    }
    
    ContactsListObject contacts = new ContactsListObject();
    contacts.save();

    for (String id : result){          
      contacts.put(id);
    }
    
    interaction.sharedContacts = contacts;
  
    callback.onComplete();
  }
}