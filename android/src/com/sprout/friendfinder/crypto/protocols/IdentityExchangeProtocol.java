package com.sprout.friendfinder.crypto.protocols;

import org.brickred.socialauth.Profile;
import org.json.JSONObject;

import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PrivateProtocol;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;

/**
 * identity exchange protocol
 * TODO: now its a dummy protocol, doesnt do anything yet
 * @author norrathep
 *
 */
public class IdentityExchangeProtocol extends PrivateProtocol<Void, Void, ProfileObject> {
  
  private static final String TAG = IdentityExchangeProtocol.class.getSimpleName();
  private ProfileDownloadCallback callback;
  private AuthorizationObject authObj;
  private Interaction interaction;
  private CommunicationService com;
  
  public IdentityExchangeProtocol(CommunicationService s, ProfileDownloadCallback callback, 
      AuthorizationObject authObj, Interaction interaction) {
    this(IdentityExchangeProtocol.class.getSimpleName(), s, true);
    this.callback = callback;
    this.authObj = authObj;
    this.interaction = interaction;
    this.com = s;
  }

  protected IdentityExchangeProtocol(String testName, CommunicationService s,
      boolean client) {
    super(testName, s, client);
  }

  @Override
  protected ProfileObject conductClientTest(CommunicationService s, Void... input) {
    return conductServerTest(s, input);
  }

  @Override
  protected ProfileObject conductServerTest(CommunicationService s, Void... input) {
    s.write(authObj.getAuth());
    s.write(authObj.getIdentity());
    
    String peerAuthString = s.readString();
    String peerAuthIdentity = s.readString();
    
    // TODO: what if they send trash data here... then he will know my identity but i dont know his..
    AuthorizationObject peerAuth = new AuthorizationObject(authObj, peerAuthString, peerAuthIdentity);
    
    if(peerAuth.verifyIdentity()) {
      Log.d(TAG, "Identity verification succeeded");
    } else {
      Log.d(TAG, "Identity verification failed");
      return null;
    }
    
    String peerIdentity = peerAuth.getDecodedIdentity();
    Profile p = new Profile();
    
    try {
      JSONObject jObject = new JSONObject(peerIdentity);
      JSONObject msg = jObject.getJSONObject("profile");
      
      Log.i(TAG, "msg: "+msg.toString());
      
      p.setFirstName((String) msg.get("first_name"));
      p.setLastName((String) msg.get("last_name"));
      p.setValidatedId((String) msg.get("id"));
      
      JSONObject avatar = msg.getJSONObject("avatar");
      p.setProfileImageURL((String) avatar.get("url"));
      
    } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
    
    ProfileObject po = new ProfileObject(p);
//    po.getProfileImage(context) TODO: check with hash
    
    Log.i(TAG, "Identity: "+p.toString());
    
    return po;
  }

  @Override
  protected void loadSharedKeys() {}
  
  @Override
  public ProfileObject doInBackground(Void... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "IdentityExchangeProtocol failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the identity exchange protocol");
    
    
  }

  @Override
  public void onPostExecute(ProfileObject result) {
    if(result != null) {
      Log.i(TAG, "identity exchange complete");
      
      result.save();
      Log.i(TAG, "result: "+result.toString());
      
      interaction.profile = result;
      
      callback.onComplete();
    } else {
      Log.i(TAG, "identity exchange fail");
      callback.onError();
    }
  }
  
}
