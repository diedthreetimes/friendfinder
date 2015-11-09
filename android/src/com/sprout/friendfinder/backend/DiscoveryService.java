package com.sprout.friendfinder.backend;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.Security;
import java.security.cert.CertificateException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.LoginError;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.util.AccessGrant;
import org.json.JSONException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.sprout.finderlib.communication.BluetoothService;
import com.sprout.finderlib.communication.BluetoothServiceLogger;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.common.Config;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.crypto.protocols.NegotiationProtocol;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager.ProtocolType;
import com.sprout.friendfinder.models.ContactsListObject;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.ui.BluetoothMessageActivity;
import com.sprout.friendfinder.ui.InteractionItem;
import com.sprout.friendfinder.ui.LoginActivity;

// TODO: General
//    Monitor INTERNET access state.
//    Schedule sync events if Internet is not available (or just for periodic updates)


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
  public static final String ACTION_RESET_CACHE = "action_cache";
  public static final String ACTION_RESET_CACHE_PEERS = "action_cache_peers";
  
  /***************************/
  /* ***   Preferences   *** */
  /***************************/
  public static final String PROFILE_ID_PREF = "profile_id";
  public static final String LAST_SCAN_DEVICES_PREF = "last_scan_devices";

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
  private static final int DEVICE_AVOID_TIMEOUT = 60*60*24; // Seconds 
  
  /***************************/
  /* * Spoungy Castle Init * */
  /***************************/
  
  static {
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
  }


  /***************************/
  /* ***   Connections   *** */
  /***************************/
  private CommunicationService mMessageService;
  private static boolean benchmarkBandwidth = Config.getBenchmark();

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
      reset();
      initialize();
    } else if (intent.getAction().equals(ACTION_RESTART)) {
      stop();
      initialize();
    } else if (intent.getAction().equals(ACTION_STOP)) {
      stop();
    } else if (intent.getAction().equals(ACTION_SYNC)) {
      // TODO: what if we are in the middle of a connection when this sync request starts.
      // Will the transition be handled smoothly? Probably not.
      sync();
    } else if (intent.getAction().equals(ACTION_LOGOUT)) {
      logout();
    } else if (intent.getAction().equals(ACTION_RESET_CACHE)) {
      mMessageService.clearAllCache();
      mDeviceCache.clear();

      PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this).edit().remove(LAST_SCAN_DEVICES_PREF).apply();
      PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this).edit().remove(InteractionItem.EXCHANGE_IDENTITY_ADDR).apply();
    } else if (intent.getAction().equals(ACTION_RESET_CACHE_PEERS)) {
      mDeviceCache.clear();
    }

    // This is useful to ensure that our service stays alive. 
    // TODO: We may only want to return this for start requests
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
    adapter.authorizeIfAvailable(this, Provider.TWITTER);
    
    // After we call authorize, callbacks are passed in the DialogListner passed when the adapter is intitialized
    
    if (D) Log.d(TAG, "Local token is " + adapter.getToken(Provider.TWITTER));
  }

  private void logout() {
    // TODO: Use reflection to see if we are signed in already.
    // otherwise we get an error


    if(D) Log.i(TAG, "Loggingout from socialauth");
    try {
      // TODO: Does this actually log you out. It appears like it does not
      adapter.signOut(Provider.TWITTER.name());    
    } catch (Exception e) {
      Log.e(TAG,"Could not logout. Are you logged in", e);
    }

    // TODO: We need to also delete saved information here
  }
  
  ProfileObject mProfile;
  List<ProfileObject> mContactList;
  Map<AuthorizationObjectType, AuthorizationObject> mAuthObj;

  // TODO: Rename donwloadAll
  private void downloadAll(final ProfileDownloadCallback callback) {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        // Download the profile
        try {
          // Querying synchronously is a bit cleaner, but we could get some speedup, by querying asynchrnously.
          // (One thread/task per download)
          ProfileObject prof = new ProfileObject(adapter.getUserProfile());
          ContactsListObject cont = new ContactsListObject(adapter.getContactList());
          prof.setContacts(cont);
          cont.save();
          prof.save();
          
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
          if (!prefs.edit().putLong(PROFILE_ID_PREF, prof.getId()).commit()) {
            Log.d(TAG, "Write profile id to preferences failed");
          }
          
          AccessGrant grant = adapter.getAccessGrant(Provider.TWITTER);
          AuthorizationDownloader.download(DiscoveryService.this, grant.getKey(), grant.getSecret(), AuthorizationObjectType.PSI).save();
          AuthorizationDownloader.download(DiscoveryService.this, grant.getKey(), grant.getSecret(), AuthorizationObjectType.PSI_CA_DEP).save();
          AuthorizationDownloader.download(DiscoveryService.this, grant.getKey(), grant.getSecret(), AuthorizationObjectType.PSI_CA).save();
          AuthorizationDownloader.download(DiscoveryService.this, grant.getKey(), grant.getSecret(), AuthorizationObjectType.B_PSI_CA).save();
          
          if(V) Log.d(TAG, "Downloads complete");
        } catch (NullPointerException e) {
          Log.e(TAG, "Download failed.", e);
          return false;
        }
        catch (IOException e) {
          Log.e(TAG, "Error downloading ", e);
          return false;
        } catch (JSONException e) {
          Log.e(TAG, "Authorization json can not be parsed", e);
          return false;
        } catch (CertificateException e) {
          Log.e(TAG, "Certificate loading error", e);
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

  // TODO: Rename, and consider using memoization
  public ProfileObject loadProfileFromFile() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    
    ProfileObject profile = ProfileObject.load(ProfileObject.class, prefs.getLong(PROFILE_ID_PREF, -1));
    if (profile == null) {
      if(prefs.getLong(PROFILE_ID_PREF, -1) == -1) {
        Log.e(TAG, "No profile downloaded");
      } else {
        Log.e(TAG, "Profile id cannot be found " + prefs.getLong(PROFILE_ID_PREF, -1));
      }
    }
    
    return profile;
  } 

  // TODO: Rename and reconsider structure of how these are stored.
  //  (i.e. rely on memoization so we don't need to save, and then load immediately), of course after testing
  public List<ProfileObject> loadContactsFromFile() {
    ProfileObject profile = loadProfileFromFile();
    if (profile == null) {
      Log.d(TAG, "Profile not found, can't retrieve contacts");
      return null;
    }
    
    return profile.contacts().getContactList();
  }

  // TODO: Rename
  public Map<AuthorizationObjectType, AuthorizationObject> loadAuthorizationFromFile() {
    
    // only need PSI and PSI_CA
    // TODO: put this into proper place
    final List<AuthorizationObjectType> AUTH_TYPES = new ArrayList<AuthorizationObjectType>() {{
      add(AuthorizationObjectType.PSI);
      add(AuthorizationObjectType.PSI_CA);
      add(AuthorizationObjectType.B_PSI_CA);
    }};
    
    try {
      Map<AuthorizationObjectType, AuthorizationObject> auths = new HashMap<AuthorizationObjectType, AuthorizationObject>();
      for(AuthorizationObjectType type : AUTH_TYPES) {
        auths.put(type, AuthorizationObject.getAvailableAuth(this, type));
      }
      return auths;
    } catch (Exception e) {
      Log.e(TAG, "Certificate could not be laoded. ", e);
      return null;
    }
  }
  
//  // Operations on the profile
//  private void checkCommonFriends(
//      List<ProfileObject> contactList, 
//      Map<AuthorizationObjectType, AuthorizationObject> authobj,
//      Interaction interaction,
//      ProfileDownloadCallback callback) {
//    //assume established communication channel with peer 
//
//    if (authobj == null || contactList == null) {
//      Log.i(TAG, "Authorization or contactList not loaded, aborting test");  
//      return;
//    }
//    
//    if(!authobj.containsKey(AuthorizationObjectType.PSI) && !authobj.containsKey(AuthorizationObjectType.PSI_CA)) {
//      Log.e(TAG, "auths dont exist, aborting test");
//      return;
//    }
//
//    String[] input = new String[contactList.size()];
//    for( int i=0; i < contactList.size(); i++) {
//      input[i] = contactList.get(i).getUid();
//    }
//    
//    Log.i(TAG, "Start common friends cardinality");
//    
//    // TODO: pretty ugly here passing all params for commonFriendsTest
//    (new CommonFriendsCardinalityTest(mMessageService, 
//        authobj.get(AuthorizationObjectType.PSI_CA), authobj.get(AuthorizationObjectType.PSI), 
//        callback, interaction, input)).execute(input);
//
//    // This is really weird. Perhaps we should just inline the CommonFriendsTest, it might make more sense.
//    // I also really don't like the use of callback here, but it works for now
////    (new CommonFriendsTest(mMessageService, authobj, callback, interaction)).execute(input);
//  }
  
  /*****************************/
  /* ** Utility Functions  *** */
  /*****************************/
  
  // This may not belong as it's own function. Instead, we should simply do this in a timer.
  private void searchForPeers() {
    if(D) Log.i(TAG, "Searching for peers");
    
    mMessageService.discoverPeers(new CommunicationService.Callback(){
      
      private ScanResult result = null;
      @Override
      public void onPeerDiscovered(Device peer) { 
        if (V) Log.i(TAG, "Device: " + peer.getName() + " found, (But not verified)");
      } // We don't use this function, since these devices may not be part of our app   

      // TODO: when no device is found, this code doesnt get executed...
      @Override
      public void onDiscoveryComplete(boolean success){
        if(!success) {
          if (D) Log.e(TAG, "Discovery failed!");
          stop();
        }
        else { 
          if (D) Log.i(TAG, "Discovery completed. Devices found: "+((result==null) ? null : result.getAddresses()));
          
          if (mState != STATE_CONNECTED && mState != STATE_RUNNING) {
            runAll(result);
          }
        }
      }

      @Override
      public void onDiscoveryStarted(){
        if (D) Log.i(TAG, "Discovery started");
        
        result = new ScanResult();
      }

      @Override
      public void onServiceDiscovered(Device peer) {
        Log.i(TAG, "Service discovered! Adding to waitlist " + peer.getName());
        
        if (result == null) {
          Log.e(TAG, "Why hasn't discovery been started already!!!");
          return;
        }
        
        result.add(peer);
      }
    });
  }
  
  /**
   * Reset any saved state. Useful for debugging.
   */
  private void reset() {
    mDeviceCache.clear();
    lastDiscovery = 0;
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

    adapter.addProvider(Provider.TWITTER, R.drawable.twitter);
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
  private ScanResult mLastScanResult; // TODO: Expose this for adding "available" peers ui
  
  private ScanResult mLastScanDevices; // would love to use mLastScanResult but it gets modified/removed at the end
  private Device mRunningDevice;
  
  // Called when discovery completes to run all discovered devices
  private void runAll(ScanResult discovered) {
    Log.i(TAG, "LAST_SCAN_DEVICES_PREF: "+PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this).getStringSet(LAST_SCAN_DEVICES_PREF, null));
    mLastScanResult = discovered;
    
    // sometimes it can be null
    if(discovered != null)  mLastScanDevices = (ScanResult) discovered.clone();
    
    run();
  }
  
  private void notifyChanges() {
    if(mLastScanDevices == null) {
      Log.i(TAG, "mLastScanDevices is null - most likely you are being discovered or no device found in last scan");
      return;
    }
    SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
    Editor prefsEditor = mPrefs.edit();
    prefsEditor.putStringSet(LAST_SCAN_DEVICES_PREF, mLastScanDevices.getAddresses()).apply();
    Log.i(TAG, "previous discovery, number of devices found: " + mLastScanDevices.size());
    
    // reset mLastScanDevices
    mLastScanDevices = null;
  }
  
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
    
    if (mLastScanResult == null) {
      if(V) Log.d(TAG, "No scan present.");
      notifyChanges();
      ready();
      return;
    }
    
    Iterator<Device> i = mLastScanResult.iterator();
    
    // If we have no devices more devices to connect to
    if (!i.hasNext()) {
      if(V) Log.d(TAG, "No more devices found");

      notifyChanges();
      ready();
      return;
    }
    
    mRunningDevice = i.next();
    i.remove();
    
    if(ProtocolManager.getProtocolType(this, mDeviceCache, mRunningDevice).equals(ProtocolType.NONE)) {
      run();
    } else {
      Log.i(TAG, "Trying to connect device: "+mRunningDevice);
      mMessageService.connect(mRunningDevice);
    }
    
    // Upon establishing a connection we enter the connected() state
  }
  
  private void connected() {    
    
    // TODO: What is the best thing to do in this case? For now just warn and return.
    if (mState == STATE_CONNECTED) {
      Log.w(TAG, "Connected() called while already connected");
      // The hope is that here we already have a protocol running asynchronously.
      // We may be able to remove this case from ever happening until then a (slightly horrendous) stop gap would be
      //   a timer that ensures the service doesn't get stuck
      return;
    }
    
    // This shouldn't happen.
    if (mState != STATE_RUNNING && mState != STATE_READY) {
      Log.w(TAG, "Connected() called before calling ready()");
      stop();
      return;
    }
    
    if (mMessageService == null || mMessageService.getState() != CommunicationService.STATE_CONNECTED) {
      Log.e(TAG, "Running, but CommunicationService not initialized, or not connected");
      if(mMessageService != null) {
        Log.e(TAG, "mMessageService state is "+mMessageService.getState());
      }
      stop();
      return;
    }
    
    setState(STATE_CONNECTED);
    
    // figure out what protocol "I" want to run with this device
    final ProtocolType protocolType = ProtocolManager.getProtocolType(this, mDeviceCache, mRunningDevice);
    Log.i(TAG, "before negotiation phase, I want to run "+protocolType+" with device: "+mRunningDevice);
    
    // start negotiation phase
    new NegotiationProtocol(mMessageService, protocolType, new ProtocolCallback() {

      @Override
      public void onComplete(Object o) {
        // now we know what protocol we should run..
        // TODO: for now, assume we will run protocol if my protocol matches his
        try {
          ProtocolType resultProtocol = (ProtocolType) o;
          Log.i(TAG, "negotiation is completed, now running protocol "+resultProtocol+" with device: "+mRunningDevice);
          runProtocol(resultProtocol);
        } catch (Exception e) {
          Log.i(TAG, "fail to run protocol");
          e.printStackTrace();
          run();
        }
      }

      @Override
      public void onError(Object o) {
        run();
      }
      
    }).execute();
    
  }
  
  private ProtocolCallback getProtocolCallback(ProtocolType protocolType, final Interaction interaction) {
    if(protocolType.equals(ProtocolType.IDX)) {
        return new ProtocolCallback() {
        
          @Override
          public void onError(Object o) {
            if(D) Log.i(TAG, "Error trying to exchange identity with device "+mRunningDevice);
            
            // TODO: do something with interaction and save it
            interaction.failed = true;
            interaction.save();
            run();
          }
          
          @Override
          public void onComplete(Object o) {
            if(V) Log.i(TAG, "Identity exchange complete. Remove "+mRunningDevice+" from EXCHANGE_IDENTITY_ADDR");
            //remove addr in sharedpref
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
            
            Set<String> pendingExchangeAddr = pref.getStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, new HashSet<String>());
            pendingExchangeAddr.remove(mRunningDevice.getAddress());

            // TODO: have to revisit this for active interaction
            // we need to add this when we are the one being discovered - scanResult is null
            // maybe use another pref for adding into active list (or otto)
            if(V) Log.i(TAG, "adding "+mRunningDevice.getAddress()+" into active interaction list");
            Set<String> lastScanAddrSet = new HashSet<String>(pref.getStringSet(LAST_SCAN_DEVICES_PREF, new HashSet<String>()));
            lastScanAddrSet.add(mRunningDevice.getAddress());

            pref.edit()
            .putStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, pendingExchangeAddr)
            .putStringSet(LAST_SCAN_DEVICES_PREF, lastScanAddrSet)
//            .putBoolean(InteractionBaseActivity.RELOAD_ADAPTER_PREF, true)
            .apply();
            
            // TODO: do something with interaction and save it
            interaction.failed = false;
            interaction.save();
            
            Log.i(TAG, "Saving interaction with profile "+interaction.profile.toString());
            
            run();
          }
          
        };
    } else if (protocolType.equals(ProtocolType.PSICA) || protocolType.equals(ProtocolType.BPSICA)) {
      
      return new ProtocolCallback() {
        @Override
        public void onComplete(Object o) {
          try {
            Log.i(TAG, "Successfully compute cardinality, now go to PSI");
            runProtocol(ProtocolType.PSI);
          } catch (Exception e) {
            e.printStackTrace();
            run();
          }
        }

        @Override
        public void onError(Object o) {
          if(D) Log.i(TAG, "Error occured connecting to [device]");
          
          interaction.failed = true;
          interaction.save();
          
          // TODO: we should increment device's failed counter
          // If it is less than maximum retry attempts we should try again (possibly on next discovery)
          // We also need to clear any state that we have saved due to contact with device
          // For now, we simply do nothing.
          run();
          
        }
      };
    
    } else if(protocolType.equals(ProtocolType.PSI)) {
      return new ProtocolCallback() {

        // After this protocol completes, we enter the ready state
        @Override
        public void onComplete(Object o) {
          if(V) Log.i(TAG, "Common friend detection complete.");
        
          if (!benchmarkBandwidth) {// overloaded for now
            mDeviceCache.add(mRunningDevice);
          } 
          
          interaction.failed = false;
          interaction.save();
          
          // show notification on success after getting address
          ContactsNotificationManager.getInstance().showNotification(DiscoveryService.this, interaction);
          Log.i(TAG, "saving interaction at addr: "+interaction.address);
          
          // TODO: have to revisit this for active interaction
          // we need to add this when we are the one being discovered - scanResult is null
          // maybe use another pref for adding into active list (or otto)
          Log.i(TAG, "adding "+mRunningDevice.getAddress()+" into active interaction list");
          SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
          Set<String> lastScanAddrSet = new HashSet<String>(mPrefs.getStringSet(LAST_SCAN_DEVICES_PREF, new HashSet<String>()));
          lastScanAddrSet.add(mRunningDevice.getAddress());
          PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this).edit().putStringSet(LAST_SCAN_DEVICES_PREF, lastScanAddrSet).apply();
          
          run();
        }

        @Override
        public void onError(Object o) {
          if(D) Log.i(TAG, "Error occured connecting to [device]");
          
          interaction.failed = true;
          interaction.save();
          
          // TODO: we should increment device's failed counter
          // If it is less than maximum retry attempts we should try again (possibly on next discovery)
          // We also need to clear any state that we have saved due to contact with device
          // For now, we simply do nothing.
          run();
        }
      };
    }   else if(protocolType.equals(ProtocolType.MSG)) {
      return new ProtocolCallback() {

        @Override
        public void onComplete(Object o) {
          if(o != null && (Integer) o > 0) {

            SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this);
            Set<String> anm = new HashSet<String>(mPrefs.getStringSet(BluetoothMessageActivity.ADDRESS_NEW_MESSAGES, new HashSet<String>()));
            anm.add(interaction.address);
            PreferenceManager.getDefaultSharedPreferences(DiscoveryService.this).edit()
            .putStringSet(BluetoothMessageActivity.ADDRESS_NEW_MESSAGES, anm)
            .apply();
          }
          // TODO: do anything about interaction??
          
          run();
          
        }
  
        @Override
        public void onError(Object o) {
          
          // TODO: do anything about interaction??
          
          run();
          
        }
      };
    } else return new ProtocolCallback() {

      @Override
      public void onComplete(Object o) {
        run();
        
      }

      @Override
      public void onError(Object o) {
        run();
        
      }
    };
  }
  
  private void runProtocol(ProtocolType protocolType) throws Exception  {
    final Interaction interaction;
    if(protocolType.equals(ProtocolType.IDX) || protocolType.equals(ProtocolType.MSG)) {
      //TODO: using address is wrong, maybe need to use id instead
      interaction = (Interaction) new Select().from(Interaction.class).where("address=?", mRunningDevice.getAddress()).orderBy("timestamp DESC").executeSingle();
    } else {
      interaction = new Interaction();
      interaction.address = mRunningDevice.getAddress();
      interaction.timestamp = Calendar.getInstance();
    }
    
    ProtocolCallback callback = getProtocolCallback(protocolType, interaction);
    
    Log.i(TAG, "about to run protocol "+protocolType);
    ProtocolManager.runProtocol(protocolType, mAuthObj, callback, this, mMessageService, mRunningDevice, mContactList, interaction);
  }

  private void onDisabled() {
    setState(STATE_DISABLED);
    
    // TODO: Stop all communication attempts. (but be sure not to disable messages about enabled events)
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
          // YES, not sure what the right thing to do is in this case.
          
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
    /* ***  Custom Callbacks *** */
    /*****************************/
    
    /**
     * This callback monitors asynchronous social interactions. i.e. downloading
     */
    public interface ProfileDownloadCallback {
      public void onComplete();
      public void onError();
    }
    
    public interface ProtocolCallback {
      public void onComplete(Object o);
      public void onError(Object o);
    }
}
