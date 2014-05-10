package com.sprout.friendfinder.backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.LoginError;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import com.sprout.friendfinder.social.ContactsListObject;
import com.sprout.friendfinder.social.ProfileObject;
import com.sprout.friendfinder.ui.LoginActivity;


// TODO: General
//    Monitor INTERNET access state.
//    Schedule sync events if Internet is not available (or just for periodic updates)
//    Add timer for discovery


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
  public static final String ACTION_RESTART = "action_restart";
  public static final String ACTION_START = "action_start";
  public static final String ACTION_STOP = "action_stop";
  public static final String ACTION_SYNC = "action_sync";
  public static final String ACTION_LOGOUT = "action_logout";

  /***************************/
  /* ***    STATES       *** */
  /***************************/

  // These states ensure that we don't attempt to initiate a protocol before we're ready
  private static final int STATE_STOP = 0;
  private static final int STATE_SYNC = 1;
  private static final int STATE_INIT = 2;
  private static final int STATE_READY = 3;
  private static final int STATE_RUNNING = 4;   // iterating through discovered devices
  private static final int STATE_CONNECTED = 5; // run a single instance of the protocol
  private static final int STATE_DISABLED = 6; // Usually because the medium is turned off

  private int mState;
  
  /***************************/
  /* ** Tunable Constants ** */
  /***************************/
  
  private static final int DISCOVERY_INTERVAL = 60; // Seconds
  private static final int DEVICE_AVOID_TIMEOUT = 60*10;// 60*60*24; // Seconds


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
    
    discoveryTimer = new Timer("Discovery Timer");
  }

  @Override
  public void onDestroy() {
    if(L) Log.i(TAG, "Service being destroyed");

    stop();
    discoveryTimer.cancel();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(L) Log.i(TAG, "Received start id " + startId + ": " + intent);

    // We assume that any system restarts, are equivalent to an ACTION_RESTART
    if (intent == null || intent.getAction() == null || intent.getAction().equals(ACTION_START)) {
      // If we are not in the stop state we should probably just do nothing, we've already been started
      //  For debug purposes, we simply restart ourselves
      initialize();
    } else if (intent.getAction().equals(ACTION_RESTART)) {
      stop();
      initialize();
    } else if (intent.getAction().equals(ACTION_STOP)) {
      stop();
    } else if (intent.getAction().equals(ACTION_SYNC)) {
      sync();
    } else if (intent.getAction().equals(ACTION_LOGOUT)) {
      logout();
    }

    // This is useful to ensure that our service stays alive. 
    return START_REDELIVER_INTENT;
  }

  /*****************************/
  /* ** Profile Functions  *** */
  /*****************************/

  private SocialAuthAdapter adapter; // Initialized in sync

  private void login() {
    /* As of now it seems that the linkedin authenticator will not work. We should revist this later.
     * 


      AccountManager accountManager = AccountManager.get(this);
      Account[] accounts = accountManager.getAccountsByType("com.linkedin.android");


      if (accounts.length != 0) {
        for (Account account: accounts) {
          Log.d(TAG, "Account: " + account.toString());
        }

        // Prompt user to select an account, and remember decision. (but make sure remembered account is still present)
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
    
    if (D) Log.i(TAG, "Login called, about to authorize");
    adapter.authorizeIfAvailable(this, Provider.LINKEDIN);
    
    // After we call authorize, callbacks are passed in the DialogListner passed when the adapter is intitialized
    
    if (D) Log.d(TAG, "Local token is " + adapter.getToken(Provider.LINKEDIN));
  }

  private void logout() {
    // TODO: Use reflection to see if we are signed in already.
    // otherwise we get an error


    if(D) Log.i(TAG, "Loggingout from socialauth");
    try {
      // TODO: Does this actually log you out. It appears like it does not
      adapter.signOut(Provider.LINKEDIN.name());    
    } catch (Exception e) {
      Log.e(TAG,"Could not logout. Are you logged in", e);
    }

    // TODO: We need to also delete saved information her
  }
  
  // TODO: Eventually this may need to be more information.
  // Perhaps it could all be encompassed into a single Profile
  ProfileObject mProfile;
  List<ProfileObject> mContactList;
  AuthorizationObject mAuthObj; 

  // TODO: Rename donwloadAll
  private void downloadAll(final ProfileDownloadCallback callback) {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        // Download the profile
        try {
          // Querying synchronously is a bit cleaner, but we could get some speedup, by querying asynchrnously.
          // (One thread per download)
          new ProfileObject(adapter.getUserProfile()).save(DiscoveryService.this);
        
        
          ContactsListObject clist = new ContactsListObject(adapter.getContactList());
          clist.save(DiscoveryService.this);
          
          Log.d(TAG, "Your " + clist.size() + " contacts have been downloaded");
          
          clist.saveAuthorization(DiscoveryService.this);
          
          // Download Authorization
          // TODO: for now this is computed
          
        } catch (NullPointerException e) {
          Log.e(TAG, "Download failed.", e);
          return false;
        }
        catch (IOException e) {
          return false;
        }
        
        
      
        return true;
      }
      
      @Override
      protected void onPostExecute(Boolean success) {
        if (!success)
          callback.onError();
        else {
          callback.onComplete();
        }
      }
      
 
      
    }.execute();
    
  }

  public ProfileObject loadProfileFromFile() {  
    //load it into object that can be used later on

    ProfileObject myProfile = new ProfileObject();

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
      
      return myProfile;
    } catch (Exception e) {
      Log.d(TAG, "File has not been downloaded yet");
      
      return null;
    }
  } 

  public List<ProfileObject> loadContactsFromFile() {
    //load it into object that can be used later on

    ContactsListObject contactListObject = new ContactsListObject();
    List<ProfileObject> contactList = null;

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
      
      return contactList;

      //Toast.makeText(this, "Your " + contactList.size() + " offline contacts have been loaded", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      Log.d(TAG, "File has not been downloaded so far");
      
      return null;
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
  private void checkCommonFriends(
      List<ProfileObject> contactList, 
      AuthorizationObject authobj,
      ProfileDownloadCallback callback) {
    //assume established communication channel with peer 

    if (authobj == null || contactList == null) {
      Log.i(TAG, "Authorization or contactList not loaded, aborting test");  
      return;
    }


    String[] input = new String[contactList.size()];
    for( int i=0; i < contactList.size(); i++) {
      input[i] = contactList.get(i).getId();
    }

    // This is really weird. Perhaps we should just inline the CommonFriendsTest, it might make more sense.
    // I also really don't like the use of callback here, but it works for now
    (new CommonFriendsTest(mMessageService, authobj, callback)).execute(input);
    

  }
  
  /*****************************/
  /* ** Utility Functions  *** */
  /*****************************/
  
  private Set<Device> mDiscoveredDevices = new HashSet<Device>();
  // This may not belong as it's own function. Instead, we should simply do this in a timer.
  private void searchForPeers() {
    if(D) Log.i(TAG, "Searching for peers");
    
    mMessageService.discoverPeers(new CommunicationService.Callback(){  
      @Override
      public void onPeerDiscovered(Device peer) { 
        if (V) Log.i(TAG, "Device: " + peer.getName() + " found, (But not verified)");
      } // We don't use this function, since these devices may not be part of our app   

      @Override
      public void onDiscoveryComplete(boolean success){
        if(!success) {
          if (D) Log.e(TAG, "Discovery failed!");
          stop();
        }
        else { 
          if (D) Log.i(TAG, "Discovery completed");
          
          if (mState != STATE_CONNECTED && mState != STATE_RUNNING)
            run();
        }
      }

      @Override
      public void onDiscoveryStarted(){
        if (D) Log.i(TAG, "Discovery started");
        
        mDiscoveredDevices.clear();
      }

      @Override
      public void onServiceDiscovered(Device peer) {
        Log.i(TAG, "Service discovered! Adding to waitlist " + peer.getName());
        
        mDiscoveredDevices.add(peer);
      }
    });
  }

  /*****************************/
  /* ** State Transitions  *** */
  /*****************************/

  private String stateString(int state) {
    // This is a bit strange
    switch (state) {
    case STATE_STOP:
      return "STOP";
    case STATE_SYNC:
      return "SYNC";
    case STATE_INIT:
      return "INIT";
    case STATE_READY:
      return "READY";
    case STATE_RUNNING:
      return "RUNNING";
    case STATE_CONNECTED:
      return "CONNECTED";
    case STATE_DISABLED:
      return "DISABLED";
    default:
      return String.valueOf(state);       
    }
  }
  
  private void setState(int state) {
    if (D) Log.d(TAG, "setState() " + stateString(mState) + " -> " + stateString(state));
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

    // TODO: We need stop all communications for sync to happen.
    
    // Initialize the AuthAdapter
    // This is a bit of callback hell, we may want to refactor this at some point.
    // Basically, The dialoglistner's onComplete is called when authorization succeeds.
    // We then call another async method to download the profiles.
    // Once this completes, we transition away from the sync state (back to the initialize state)
    adapter = new SocialAuthAdapter(new DialogListener() {
      @Override
      public void onComplete(Bundle values) {
        if(D) Log.d(TAG, "Authorization successful");
        if(D) Log.d(TAG, "Local token is " + adapter.getToken(Provider.LINKEDIN));
        
        downloadAll(new ProfileDownloadCallback () {
          @Override
          public void onComplete() {
            // This is called, once all asyncs complete without error
            
            // Record the current time of sync;
            SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
            SharedPreferences.Editor editor = sharedPreference.edit();
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            Long time = ts.getTime();
            editor.putLong("lastSync", time);
            editor.commit();
            
            initialize();
          }
          
          @Override
          public void onError() {
            Log.e(TAG, "Unable to download profile. Perhpas internet is not avaialable");
            
            onSyncError();
          }
        });
      }

      // TODO: We need to display something useful to users in these failure cases
      @Override
      public void onCancel() {
        if(D) Log.d(TAG, "Cancel called");

        onSyncError();
      }     

      @Override
      public void onError(SocialAuthError er) {
        // This loginerror is thrown if we aren't currently logged in
        if (er.getInnerException() instanceof LoginError){
          if(D) Log.i(TAG, "Starting Login Activity");
          //TODO: Display a notification instead of starting the activity directly
          
          Intent intent = new Intent(DiscoveryService.this, LoginActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
          stop();
        } else {
          if(D) Log.e(TAG, "Error", er);
          onSyncError();
        }
      }

      @Override
      public void onBack(){
        if(D) Log.d(TAG, "BACK");
        
        onSyncError();
      }
    });

    adapter.addProvider(Provider.LINKEDIN, R.drawable.linkedin);
    login(); // Will start the callback process 
  }
  
  private void onSyncError() {
    // TODO: We need to schedule a sync for a later date.
    
    if( loadContactsFromFile() == null ||  loadAuthorizationFromFile() == null || loadProfileFromFile() == null) {
      // We definetly need to reschedule the sync in this case. 
      Log.i(TAG, "No preloaded data available, stopping");
      stop();
    } else {
      initialize();
    }
  }

  // Inititalize the com service
  private void initialize() {
    setState(STATE_INIT);   

    mContactList = loadContactsFromFile();
    mProfile = loadProfileFromFile();
    
    if (mContactList == null || mProfile == null) {
      Log.i(TAG, "No profile or contact information.");
      
      sync(); 
      return;
    }
    
    if (mMessageService == null) {
      if(benchmarkBandwidth){
        mMessageService = new BluetoothServiceLogger(this, new mHandler(DiscoveryService.this));
      }else {
        mMessageService =  new BluetoothService(this, new mHandler(DiscoveryService.this));
      }
    }

    if (mMessageService.isEnabled()){ 
      ready();
    } else {
      onDisabled();
    }
  }

  long lastDiscovery = 0;
  Timer discoveryTimer;
  private void ready() {
    setState(STATE_READY);

    if (mMessageService == null) {
      Log.e(TAG, "Service ready, but no CommunicationService initialized");
      stop();
      return;
    }
    
    // Before we start attempting connections, we need to make sure we have the available profile info
    if (mProfile == null)
      mProfile = loadProfileFromFile();
    if (mContactList == null)
      mContactList = loadContactsFromFile(); 
    if (mAuthObj == null)
      mAuthObj = loadAuthorizationFromFile();

    if (mProfile == null || mContactList == null || mAuthObj == null) {
      Log.e(TAG, "Profile, Authorization, or Contact list not available in run");
      
      sync();
      return;
    }
    
    mMessageService.start(false);

    //TODO: Once we are in the ready state we need to start the timers periodically calling discovery looking for connections
    
    // Once we leave the ready state we need to make sure to stop these timers
    

    
    long now = System.currentTimeMillis();
    
    // Was discovery run in the last INTERVAL seconds
    if (lastDiscovery == 0 || lastDiscovery < (now - (DISCOVERY_INTERVAL * 1000)) ) {
      // We may want to log the discovery when it completes (or starts),
      //   however, this can be problematic if it is terminated early
      //   Logging the discovery here shouldn't be an issue
      lastDiscovery = System.currentTimeMillis();
      searchForPeers();
    } else { 
      // Schedule the discovery to execute in DISCOVERY_INTERVAL s
      discoveryTimer.schedule(new TimerTask() {

        @Override
        public void run() {
          if(V) Log.i(TAG, "Discovery timer executing");
          lastDiscovery = System.currentTimeMillis();
          searchForPeers();
        } 
        
      }, DISCOVERY_INTERVAL * 1000);
    }
    
    // Discovery success will trigger entering the run() state. Failure will re-enter ready()
  }

  private DeviceCache mDeviceCache = new DeviceCache(DEVICE_AVOID_TIMEOUT);
  private Device mRunningDevice;
  private void run() {
    setState(STATE_RUNNING);

    if (mMessageService == null ) {
      Log.e(TAG, "Service ready, but CommunicationService not initialize");
      stop();
      return;
    }

    // While in the run state we should do the following.
    // Connect to a single peer that was discovered, and run the protocol with that peer
    
    // This is the code for performing a single connection
    mMessageService.stopDiscovery();        
    if(D) Log.i(TAG, "Discovery stopped. attempting to connect");
    
    Iterator<Device> i = mDiscoveredDevices.iterator();
    
    // If we have no devices more devices to connect to
    if (!i.hasNext()) {
      if(V) Log.d(TAG, "No more devices found");
      ready();
      return;
    }
    
    mRunningDevice = i.next();
    i.remove();
    
    
    if( mDeviceCache.contains(mRunningDevice) ) {
      if(D) Log.d(TAG, "Device in cache, avoiding. " + mRunningDevice);
      run();// A bit strange way to loop through devcices, but it works
      return;
    } else {
      mMessageService.connect(mRunningDevice);
    }
    
    // Upon establishing a connection we enter the connected() state
  }
  
  private void connected() {
    if (mState != STATE_RUNNING && mState != STATE_READY) {
      Log.e(TAG, "Connected() called before calling ready()");
      stop();
      return;
    }
    
    if (mMessageService == null || mMessageService.getState() != CommunicationService.STATE_CONNECTED) {
      Log.e(TAG, "Running, but CommunicationService not initialized, or not connected");
      stop();
      return;
    }
    
    setState(STATE_CONNECTED);
    
    if(D) Log.i(TAG, "Connected, attempting to check for common friends");
    checkCommonFriends(mContactList, mAuthObj, new ProfileDownloadCallback() {

      // After this protocol completes, we enter the ready state
      @Override
      public void onComplete() {
        if(V) Log.i(TAG, "Common friend detection complete.");
        mDeviceCache.add(mRunningDevice);
             
        // TODO: We need to save the history of this event 
        // We may also want to do something with the result, but for that we would need to modify the callback
        // It may be a case for removing the callback completely.  
        run();
      }

      @Override
      public void onError() {
        if(D) Log.i(TAG, "Error occured connecting to [device]");
        
        // TODO: we should incremembt device's failed counter
        // If it is less than maximum retry attempts we should try again (possibly on next discovery)
        // We also need to clear any state that we have saved due to contact with device
        // For now, we simply do nothing.
        run();
      }
    });
  }
    

  private void onDisabled() {
    setState(STATE_DISABLED);
    
    // TODO: Stop all communication attempts. (but be sure not to disable notification about enabled)
  }

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

          // Is it possible that this will be fired while we are already connecting?
          
          target.connected();
      
          // TODO: We need a way to transition away from running() if connecting fails
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



      case CommunicationService.MESSAGE_DEVICE:
        target.mRunningDevice = (Device) msg.getData().getSerializable(CommunicationService.DEVICE);
        break;            
      case CommunicationService.MESSAGE_TOAST:
        Toast.makeText(target.getApplicationContext(), msg.getData().getString(CommunicationService.TOAST),
            Toast.LENGTH_SHORT).show();
        break;
      case CommunicationService.MESSAGE_FAILED:
        // When a message fails we assume we need to reset
        
        Log.e(TAG, "Message failed");

        // TODO: Perform a reset (remove the peer, etc)
        break;
      case CommunicationService.MESSAGE_DISABLED:
        target.onDisabled();
        break;
      case CommunicationService.MESSAGE_ENABLED:
        // The state should be disabled. But this may not be the case
        if (target.mState != STATE_STOP) {
          target.initialize();
        }
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
      
      ProfileDownloadCallback callback;

      public CommonFriendsTest(CommunicationService s, AuthorizationObject authObject, ProfileDownloadCallback callback) {
        super(s, authObject);
        
        this.callback = callback;
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

        // TODO: This is a strange place to do this, it would make more sense wrapped in a contact list object.
        HashMap<String, String> idToNameMap = new HashMap<String, String>();
        for (ProfileObject prof : mContactList) {
          idToNameMap.put( prof.getId(), prof.getDisplayName());
        }

        List<String> commonFriends = new ArrayList<String>();
        for (String id : result){
          commonFriends.add(idToNameMap.get(id));
        }
        
        // TODO: Set a notification containing a reference to the result of the test.
        //  This is most easily done by using a DB, and storing the test id.
        
        callback.onComplete();
      }

    }
    
    /*****************************/
    /* ***  Custom Callbacks *** */
    /*****************************/
    
    /**
     * This callback monitors asynchronous social interactions. i.e. downloading
     */
    interface ProfileDownloadCallback {
      public void onComplete();
      public void onError();
    }

}
