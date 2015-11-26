package com.sprout.friendfinder.backend;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.sprout.finderlib.communication.BluetoothServiceLogger;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.crypto.protocols.Protocol;
import com.sprout.friendfinder.crypto.protocols.ProtocolFactory;
import com.sprout.friendfinder.crypto.protocols.ProtocolResult;

// TODO: General
//    Monitor INTERNET access state.
//    Schedule sync events if Internet is not available (or just for periodic updates)


/**
 * Similar to DiscoveryService but use test data
 * and stop the service once it finishes a test
 * @author Oak
 *
 */
public class ProtocolTestService extends Service {
  /***************************/
  /* *** Debug Flags     *** */
  /***************************/
  private static boolean D = true; // Generally useful debug info
  private static boolean V = true; // Verbose debug information
  private static boolean L = true; // Lifecycle methods

  private static String TAG = ProtocolTestService.class.getSimpleName();
  
  /***************************/
  /* *** Intent Actions  *** */
  /***************************/
  public static final String ACTION_RESTART = "action_restart";
  public static final String ACTION_START = "action_start";
  public static final String ACTION_STOP = "action_stop";
  public static final String ACTION_SYNC = "action_sync";
  
  /***************************/
  /* ***   Preferences   *** */
  /***************************/
  public static final String PROFILE_ID_PREF = "profile_id";
  public static final String LAST_SCAN_DEVICES_PREF = "last_scan_devices";

  /***************************/
  /* ***    STATES       *** */
  /***************************/

  // These states ensure that we don't attempt to initiate a protocol before we're ready
  public static final int STATE_STOP = 0;
  public static final int STATE_SYNC = 1;
  public static final int STATE_INIT = 2;
  public static final int STATE_READY = 3;
  public static final int STATE_RUNNING = 4;   // iterating through discovered devices
  public static final int STATE_CONNECTED = 5; // run a single instance of the protocol
  public static final int STATE_DISABLED = 6; // Usually because the medium is turned off
  public static final int STATE_COMPLETED = 7; // when it's completed

  private int mState;
  
  public ProtocolResult result;
  
  /***************************/
  /* ** Tunable Constants ** */
  /***************************/

  private static final int DISCOVERY_INTERVAL = 60; // Seconds
  private static final int DEVICE_AVOID_TIMEOUT = 60*60*24; // Seconds 
  public static final int MESSAGE_STATE_CHANGE = 54321; // rand
  
  /***************************/
  /* * Spoungy Castle Init * */
  /***************************/
  
  static {
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
  }
  public Device mRunningDevice = null;
  private Handler mHandler = null;
  
  private AuthorizationObject auth = null;


  /***************************/
  /* ***   Connections   *** */
  /***************************/
  private CommunicationService mMessageService;

  /***************************/
  /* *** Service Binders *** */
  /***************************/

  public class LocalBinder extends Binder {
    public ProtocolTestService getService() {
      return ProtocolTestService.this;
    }
  }
  private final IBinder mBinder = new LocalBinder();
  
  private Protocol testProtocol = null;
  private int numFriends = 0;
  private int numTrials = 1;

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
    
    numFriends = intent.getIntExtra("numFriends", 0);
    Log.i(TAG, "num test friends = "+numFriends);
    
    numTrials = intent.getIntExtra("numTrials", 1);
    Log.i(TAG, "num trials = "+numTrials);
    
    String protocolName = intent.getStringExtra("protocol");
    Log.i(TAG, "Running protocol "+protocolName);
    
    // not consistent with server, TODO?
    String[] friends = new String[numFriends];
    for(int i=0; i<numFriends; i++) {
      friends[i] = ""+i;
    }
    testProtocol = ProtocolFactory.getProtocol(protocolName, friends);
    
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
    }

    // This is useful to ensure that our service stays alive. 
    // TODO: We may only want to return this for start requests
    return START_REDELIVER_INTENT;
  }

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
    case STATE_COMPLETED:
      return "COMPLETED";
    default:
      return String.valueOf(state);  
    }
  }
  
  private void setState(int state) {
    if (D) Log.d(TAG, "setState() " + stateString(mState) + " -> " + stateString(state));

    if(mHandler!=null) mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    mState = state;
  }
  
  public int getState() {
    return mState;
  }

  private void stop() {
    setState(STATE_STOP);

    if (mMessageService != null) {
      mMessageService.stop();
      mMessageService = null;
    }
  }

  // Inititalize the com service
  public void initialize() {
    setState(STATE_INIT);   
    
    if (testProtocol.getAuthType() != AuthorizationObjectType.NONE && auth == null) {
      Log.i(TAG, "Auth is null, loading it.");
      
      sync(); 
      return;
    }
    
    if(mMessageService == null) {
      // TODO: figure out why its not working when we dont download auth
      Log.i(TAG, "Handler is not set");
      stopSelf();
      return;
    }

    if (mMessageService.isEnabled()){ 
      ready();
    } else {
      onDisabled();
    }
  }
  
  public void setHandler(Handler mHandler2) {
    Log.i(TAG, "setting handler");
    mHandler = mHandler2;
    
    mMessageService = new BluetoothServiceLogger(this, mHandler);
    
  }
  
  private void sync() {
    setState(STATE_SYNC);
    
    if(testProtocol.getAuthType() != AuthorizationObjectType.NONE) {
    
      downloadAuth(new ProfileDownloadCallback () {
        @Override
        public void onComplete() {
          // This is called, once all asyncs complete without error
          initialize();
        }
        
        @Override
        public void onError() {
          Log.e(TAG, "Unable to download profile. Perhpas internet is not avaialable");
          
          onSyncError();
        }
      }, testProtocol.getAuthType());
    } else {
      // TODO: not working, handler isnt set right after this and infinite loop in initilize()
      Log.i(TAG, "not downloading auth obj");
      initialize();
    }
  }
  
  private void onSyncError() {
    // TODO: We need to schedule a sync for a later date.
    Log.i(TAG, "sync error do sth...");
    initialize();
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
    
    mMessageService.start(false);
    
    long now = System.currentTimeMillis();
    
    // TODO: prolly dont need all of these below codes for here but its ok for now
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
  
  // Called when discovery completes to run all discovered devices
  private void runAll(ScanResult discovered) {
    mLastScanResult = discovered;
    
    run();
  }
  private ScanResult mLastScanResult; // TODO: Expose this for adding "available" peers ui
  
  private void run() {
    setState(STATE_RUNNING);
    // While in the run state we should do the following.
    // Connect to a single peer that was discovered, and run the protocol with that peer
    
    // This is the code for performing a single connection
    mMessageService.stopDiscovery();        
    
    if(D) Log.i(TAG, "Discovery stopped. attempting to connect");
    
    Iterator<Device> i = mLastScanResult.iterator();
    
    // If we have no devices more devices to connect to
    if (!i.hasNext()) {
      if(V) Log.d(TAG, "No more devices found");
      ready();
      return;
    }
    
    mRunningDevice = i.next();
    i.remove();

    Log.i(TAG, "Trying to connect device: "+mRunningDevice);
    mMessageService.connect(mRunningDevice);
    
    // Upon establishing a connection we enter the connected() state
  }
  
  public void connected() {    
    
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
    
    Log.i(TAG, "YEAH CONNECTED");
    
    test();
  }

  public void test() {

    Log.i(TAG, "successfully downloaded auth");

    // callback: onComplete -> stop the service and get result
    // onError -> keep running till onComplete is called
    ProtocolCallback callback = new ProtocolCallback() {
      
      @Override
      public void onError(Object o) {
        // if error, keep running.
        run();
        
      }
      
      @Override
      public void onComplete(Object o) {
        if(o == null) {
          // TODO: do sth
        }
        result = (ProtocolResult) o;
        
        Log.i(TAG, "Result: "+result.toString());
        
        stop();
        
        setState(STATE_COMPLETED);
        
      }
    };
    
    try {
      testProtocol.config(numTrials, true);
      testProtocol.runTest(mMessageService, callback, auth);
    } catch(Exception e) {
      callback.onError(null);
    }
    
  }
  private void downloadAuth(final ProfileDownloadCallback callback, final AuthorizationObjectType type) {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... params) {
        // TODO: if else on protocolType
        // download auth if needed
        try {
          auth = AuthorizationDownloader.downloadTest(ProtocolTestService.this, "token", "secret", type, numFriends);
          return true;
        } catch (ClientProtocolException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (CertificateException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (JSONException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        return false;
      }

      @Override
      protected void onPostExecute(Boolean auth) {
        if (auth == null){
          callback.onError();
        } else {
          callback.onComplete();
        }
      }
      
    }.execute();
    
  }
  public void onDisabled() {
    setState(STATE_DISABLED);
    
    // TODO: Stop all communication attempts. (but be sure not to disable messages about enabled events)
  }

  
}
