package com.sprout.friendfinder.backend;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sprout.finderlib.communication.BluetoothService;
import com.sprout.finderlib.communication.BluetoothServiceLogger;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.crypto.ATWPSI;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.social.ContactDownloader;
import com.sprout.friendfinder.social.ContactsListObject;
import com.sprout.friendfinder.social.ProfileObject;
import com.sprout.friendfinder.social.SocialContactListener;
import com.sprout.friendfinder.social.SocialProfileListener;
import com.sprout.friendfinder.ui.CommonFriendsDialogFragment;

public class DiscoveryService extends Service {
  /***************************/
  /* *** Debug Flags     *** */
  /***************************/
  private static boolean D = true; // Generally useful debug info
  private static boolean V = true; // Verbose debug information
  private static boolean L = true; // Lifecycle methods

  private static String TAG = DiscoveryService.class.getSimpleName();
  
  /***************************/
  /* *** Intent Actions  *** */
  /***************************/
  public static final String ACTION_START = "action_start";

  /***************************/
  /* ***    STATES       *** */
  /***************************/

  // These states ensure that we don't attempt to initiate a protocol before we're ready
  private static final int STATE_STOP = 0;
  private static final int STATE_SYNC = 1;
  private static final int STATE_INIT = 2;
  private static final int STATE_READY = 3;
  private static final int STATE_RUNNING = 4;
  private static final int STATE_DISABLED = 5; // Usually because the medium is turned off

  private int mState;


  /***************************/
  /* ***   Connections   *** */
  /***************************/
  private CommunicationService mMessageService;
  private static boolean benchmarkBandwidth = false;

  /***************************/
  /* *** Service Binders *** */
  /***************************/

  public class LocalBinder extends Binder {
    public DiscoveryService getService() {
      return DiscoveryService.this;
    }
  }
  private final IBinder mBinder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  /*****************************/
  /* *** Lifecycle Methods *** */
  /*****************************/

  @Override
  public void onCreate() {
    if(L) Log.i(TAG, "Service created");

    setState(STATE_STOP);

    initSocialAdapter();
  }

  @Override
  public void onDestroy() {
    if(L) Log.i(TAG, "Service being destroyed");

    stop();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(L) Log.i(TAG, "Received start id " + startId + ": " + intent);

    if (intent == null || intent.getAction() == null) {
      Log.e(TAG, "Unexpected null intent recieved");
      return START_REDELIVER_INTENT;
    }

    login();

    //TODO: If we are not already synchronized (or are out of date). Synchronize 
    // Otherwise move directly to the init state
    // initialize();

    // This is useful to ensure that our service stays alive. 
    return START_REDELIVER_INTENT;
  }

  /*****************************/
  /* ** Profile Functions  *** */
  /*****************************/

  private SocialAuthAdapter adapter;
  private void initSocialAdapter() {
    adapter = new SocialAuthAdapter(new DialogListener() {
      @Override
      public void onComplete(Bundle values) {
        if(D) Log.d(TAG, "Authorization successful");
        //Here we now have a provider
      }

      @Override
      public void onCancel() {
        if(D) Log.d(TAG, "Cancel called");


      }     

      @Override
      public void onError(SocialAuthError er) {
        if(D) Log.d(TAG, "Error", er);

      }

      @Override
      public void onBack(){
        if(D) Log.d(TAG, "BACK");
      }
    });

    adapter.addProvider(Provider.LINKEDIN, R.drawable.linkedin);
  }

  private void login() {
    /* As of now it seems that the linkedin authenticator will not work. We should revist this later.
     * 


      AccountManager accountManager = AccountManager.get(this);
      Account[] accounts = accountManager.getAccountsByType("com.linkedin.android");


      if (accounts.length != 0) {
        for (Account account: accounts) {
          Log.d(TAG, "Account: " + account.toString());
        }

        // TODO: Prompt user to select an account, and remember decision. (but make sure remembered account is still present)
        Account selected = accounts[0];

        //accountManager.getAuthToken(selected, selected.type, null, this, new AccountManagerCallback<Bundle>() {
        accountManager.getAuthToken(selected, selected.type, null, true, new AccountManagerCallback<Bundle>() {

      @Override
      public void run(AccountManagerFuture<Bundle> future) {
        try {
          Bundle res = future.getResult();
          Log.i(TAG, "Account Name: " + res.getString(AccountManager.KEY_ACCOUNT_NAME));
          Log.i(TAG, "Account Type: " + res.getString(AccountManager.KEY_ACCOUNT_TYPE));
          Log.i(TAG, "AuthToken: " + res.getString(AccountManager.KEY_AUTHTOKEN));
        } catch (OperationCanceledException e) {
          Log.e(TAG, "User canceled the request", e);
        } catch (AuthenticatorException e) {
          Log.e(TAG, "Authenticator failed to respond", e);
        } catch (IOException e) {
          Log.e(TAG, "Error retrieving token. Netowrk trouble? ", e);
        }
      }
        }, new Handler() {

        });

        // set token. If auth error occurs must call invalidateAuthToken
      } 
      */
    adapter.authorize(this, Provider.LINKEDIN);
  }

  private void logout() {
    // TODO: Use reflection to see if we are signed in already.
    // otherwise we get an error


    if(D) Log.d(TAG, "Loggingout from socialauth");
    try {
      // TODO: Does this actually log you out. It appears like it does not
      adapter.signOut(Provider.LINKEDIN.name());    
    } catch (Exception e) {
      Log.e(TAG,"Could not logout. Are you logged in", e);
    }

    // TODO: We need to also delete saved information her
  }

  // TODO: I'm not a huge fan of these variables do we need them here?
  ProfileObject myProfile;
  List<ProfileObject> contactList;

  // TODO: Eventually merge download contacts and download profile
  private void downloadProfile() {
    adapter.getUserProfileAsync(new SocialProfileListener(this));
  }

  // TODO: Rewrite
  private void downloadContacts() {
    try {
      adapter.getContactListAsync(new SocialContactListener(this)); 
    } catch (Exception e) {
      Log.d(TAG, e.getMessage());
    }

    // TODO: At the moment this does nothing
    //new ContactDownloader().execute();
  }

  private void downloadAuthorization() {
    // TODO: How is the authorization downloaded? 
  }

  public void loadProfileFromFile() {  
    //load it into object that can be used later on

    myProfile = new ProfileObject();

    String filename = "profile";

    FileInputStream fileInput;
    ObjectInputStream objectInput; 

    try {
      fileInput = this.openFileInput(filename);
      objectInput = new ObjectInputStream(fileInput);
      myProfile.readObject(objectInput);
      objectInput.close();
      Log.d(TAG, "Profile loaded");
      Log.d(TAG, "Name: " + myProfile.getFirstName() + " " + myProfile.getLastName());
      //Toast.makeText(this, "Your offline profile is: " + myProfile.getFirstName() + " " + myProfile.getLastName(), Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      Log.d(TAG, "File has not been downloaded so far");
      Toast.makeText(this, "You need to download your profile first", Toast.LENGTH_SHORT).show();
    }

  } 

  // TODO: It would be nice to encorporate load and donwload some how...
  public void loadContactsFromFile() {
    //load it into object that can be used later on

    ContactsListObject contactListObject = new ContactsListObject();

    String filename = "contacts";

    FileInputStream fileInput;
    ObjectInputStream objectInput; 

    try {
      fileInput = this.openFileInput(filename);
      objectInput = new ObjectInputStream(fileInput);
      contactListObject.readObject(objectInput);
      contactList = contactListObject.getContactList();
      objectInput.close();
      Log.d(TAG, "Contacts loaded");

      //Toast.makeText(this, "Your " + contactList.size() + " offline contacts have been loaded", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      Log.d(TAG, "File has not been downloaded so far");
      Toast.makeText(this, "You need to download your contacts first", Toast.LENGTH_SHORT).show();
    }
  }

  public AuthorizationObject loadAuthorizationFromFile() {
    AuthorizationObject authObj = new AuthorizationObject();
    String filename = "authorization";
    FileInputStream fileInput;
    ObjectInputStream objectInput; 

    try {
      fileInput = this.openFileInput(filename);
      objectInput = new ObjectInputStream(fileInput);
      authObj.readObject(objectInput);
      objectInput.close();
      Log.d(TAG, "Authorization loaded");

      return authObj;

    } catch (Exception e) {
      Log.d(TAG, "Authorization has not been granted so far");
      Toast.makeText(this, "You need to get a certification for your friends first", Toast.LENGTH_SHORT).show();
      return null;
    }
  }

  // Operations on the profile
  private void checkCommonFriends() {
    //assume established communication channel with peer 

    AuthorizationObject authobj = loadAuthorizationFromFile();

    if (authobj == null || contactList == null) {
      Log.i(TAG, "Authorization or contactList not loaded, aborting test");  
      return;
    }


    String[] input = new String[contactList.size()];
    for( int i=0; i < contactList.size(); i++) {
      input[i] = contactList.get(i).getId();
    }

    (new CommonFriendsTest(mMessageService, authobj)).execute(input);

  }
  
  /*****************************/
  /* ** Utility Functions  *** */
  /*****************************/
  
  // TODO: This may not belong as it's own function. Instead, we should simply do this in a timer.
  private void searchForPeers() {
    mMessageService.discoverPeers(mDiscoveryCallback);
  }

  /*****************************/
  /* ** State Transitions  *** */
  /*****************************/

  private void setState(int state) {
    if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
    mState = state;
  }

  private void stop() {
    setState(STATE_STOP);

    if (mMessageService != null) {
      mMessageService.stop();
      mMessageService = null;
    }
  }

  private void sync() {
    setState(STATE_SYNC);

    downloadProfile();
    downloadContacts();
    downloadAuthorization();

    // Record the current time of sync;
    SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = sharedPreference.edit();
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    Long time = ts.getTime();
    editor.putLong("lastSync", time);
    editor.commit();
  }

  // Inititalize the com service
  private void initialize() {
    setState(STATE_INIT);   

    if(benchmarkBandwidth){
      mMessageService = new BluetoothServiceLogger(this, new mHandler(DiscoveryService.this));
    }else {
      mMessageService =  new BluetoothService(this, new mHandler(DiscoveryService.this));
    }

    // TODO: Add ability to query this from the messageService
    // if bluetooth is enabled
    ready();
    // Else
    // onDisabled();
  }

  private void ready() {
    setState(STATE_READY);

    if (mMessageService == null) {
      Log.e(TAG, "Service ready, but no CommunicationService initialized");
      stop();
    }

    mMessageService.start(false);

    // Once we are in the ready state we need to start the timers
  }

  private void run() {
    setState(STATE_RUNNING);

    if (mMessageService == null || mMessageService.getState() != CommunicationService.STATE_CONNECTED) {
      Log.e(TAG, "Service ready, but CommunicationService not initialized, or not connected");
      stop();
    }

    // While in the run state we should do the following.
    // Periodically run discovery.
    // Process all peers that where discovered.

    // This is the code for performing a single connection
    //mMessageService.stopDiscovery();        
    //Log.i(TAG, "Device connected, attempting to check for common friends");
    //checkCommonFriends();

    loadProfileFromFile(); //need to get our own profile information first
    loadContactsFromFile(); 
  }

  private void onDisabled() {
    setState(STATE_DISABLED);
  }

  /***************************/
  /* *** Event Callbacks *** */
  /***************************/

  // Result of device discovery.
  private CommunicationService.Callback mDiscoveryCallback = new CommunicationService.Callback(){

    @Override
    public void onPeerDiscovered(Device peer) { 
      if (D) Log.i(TAG, "Device: " + peer.getName() + " found, (But not verified)");
    } // We don't use this function, since these devices may not be part of our app   

    @Override
    public void onDiscoveryComplete(boolean success){
      if(!success)
        if (D) Log.e(TAG, "Discovery failed!");
        else
          if (D) Log.i(TAG, "Discovery completed");
    }

    @Override
    public void onDiscoveryStarted(){
      if (D) Log.i(TAG, "Discovery started");
    }

    @Override
    public void onServiceDiscovered(Device peer) {
      // TODO: We want to do this one at a time, we should have a Thread that pulls devices from a queue.
      // Here we should simply put add this device to the queue
      Log.i(TAG, "Service discovered! Attepting to connect to " + peer.getName());
      mMessageService.connect(peer, false);
    }
  };

  /***************************/
  /* *** Message Handlers ** */
  /***************************/

  // The primary handler to receive messages form the com service
  static class mHandler extends Handler {
    private final WeakReference<DiscoveryService> mTarget;
    mHandler(DiscoveryService target) {
      mTarget = new WeakReference<DiscoveryService>(target);
    }
    @Override
    public void handleMessage(Message msg) {
      DiscoveryService target = mTarget.get();
      if(target == null)
        return;

      switch (msg.what) {
      case   BluetoothService.MESSAGE_STATE_CHANGE:
        if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
        switch (msg.arg1) {
        case CommunicationService.STATE_CONNECTED:
          // TODO: Eventually, we need a way to toggle what test gets run.
          // For now we just checkCommonFriends

          target.mMessageService.stopDiscovery();
          target.checkCommonFriends();

          break;
        case CommunicationService.STATE_CONNECTING:
          //setStatus(R.string.title_connecting);
          break;
        case CommunicationService.STATE_LISTEN:
        case CommunicationService.STATE_NONE:
          //setStatus(R.string.title_not_connected);
          break;
        }
        break;
      case CommunicationService.MESSAGE_READ:
        // As of now, this branch should never be executed. It remains for testing.

        byte[] readBuf = (byte[]) msg.obj;
        // construct a string from the valid bytes in the buffer        
        String readMessage = new String(readBuf, 0, msg.arg1);

        Log.i(TAG, "Read message " + readMessage);

        Toast.makeText(target.getApplicationContext(), "Message read " 
            + readMessage, Toast.LENGTH_SHORT).show();

        break;



      case CommunicationService.MESSAGE_DEVICE_NAME:
        // If desired we can get the device name as
        // = msg.getData().getString(CommunicationService.DEVICE_NAME);            
        break;            
      case CommunicationService.MESSAGE_TOAST:
        Toast.makeText(target.getApplicationContext(), msg.getData().getString(CommunicationService.TOAST),
            Toast.LENGTH_SHORT).show();
        break;
      case CommunicationService.MESSAGE_FAILED:
        // When a message fails we assume we need to reset

        break;
      case CommunicationService.MESSAGE_DISABLED:
        target.onDisabled();
        break;
      case CommunicationService.MESSAGE_ENABLED:
        // if state is disabled 

        break;

      }
    }};

    /*****************************/
    /* ** Getters / Setters  *** */
    /*****************************/



    /*****************************/
    /* ** Protocol Callbacks *** */
    /*****************************/


    // This extends AsyncTask, and thus we provide the UI specific methods here
    private class CommonFriendsTest extends ATWPSI {

      public CommonFriendsTest(CommunicationService s, AuthorizationObject authObject) {
        super(s, authObject);
      }

      @Override
      public void onPreExecute() {
        Log.i(TAG, "About to execute the common friends protocol");
      }

      @Override
      public void onPostExecute(List<String> result) {
        Log.i(TAG, "Common friends protocol complete");

        // TODO: This is a strange place to do this, it would make more sense wrapped in a contact list object.
        HashMap<String, String> idToNameMap = new HashMap<String, String>();
        for (ProfileObject prof : contactList) {
          idToNameMap.put( prof.getId(), prof.getDisplayName());
        }

        List<String> commonFriends = new ArrayList<String>();
        for (String id : result){
          commonFriends.add(idToNameMap.get(id));
        }

        /* 
       CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragme
       newFragment.setCommonFriends(commonFriends);
       newFragment.show(getFragmentManager(), "check-common-friends");
         */
      }

    }

}
