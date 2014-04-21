package com.sprout.friendfinder.ui;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sprout.finderlib.communication.BluetoothService;
import com.sprout.finderlib.communication.BluetoothServiceLogger;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.WifiService;
import com.sprout.finderlib.ui.communication.DeviceList;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.communication.WiFiDirectBroadcastReceiver;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.social.ContactDownloader;
import com.sprout.friendfinder.social.ContactsListObject;
import com.sprout.friendfinder.social.CurrentPeer;
import com.sprout.friendfinder.social.ProfileObject;
import com.sprout.friendfinder.social.SocialContactListener;
import com.sprout.friendfinder.social.SocialProfileListener;
import com.sprout.friendfinder.ui.CommonFriendsDialogFragment.NoticeCommonFriendsDialog;

/* 
 * Ron: this class now implements also the NoticeCommonFriendsDialog interface so that we can get feedback
 *  here which button the user pressed when shown the dialog whether he wants to become friends with the peer or not
 */

public class FriendFinderActivity extends Activity implements NoticeCommonFriendsDialog {
	
	private static final String TAG = FriendFinderActivity.class.getSimpleName();
    private static final boolean D = true;
    private static final boolean benchmarkBandwidth = false;
    
    // Intent request codes
    //TODO: Refactor this out
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3; 
    private static final int REQUEST_DISCOVERABLE = 4;
	
	private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the communication services
    private CommunicationService mMessageService = null; 
    
    //WiFi P2PC
    private WifiP2pManager mManager;
    Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    
    // UI elements
    private Button mBtButton;
    private Button mWifiButton;
    
    private SocialAuthAdapter adapter;
    
    ProfileObject myProfile;
    
    List<ProfileObject> contactList;
    AuthorizationObject authObj;
    
    //to store key-value pairs: 
    SharedPreferences sharedPreference;
    
   
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {        
    	
    	
    	super.onCreate(savedInstanceState);
        if(D) Log.d(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        
        // TODO: Find a way to see if p2pwifi is supported
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        if (mManager == null) {
        	Toast.makeText(this, "WiFi P2P not available", Toast.LENGTH_LONG).show();
        }
        
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Add the social library
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
                 
        // Add providers and enable button
        adapter.addProvider(Provider.LINKEDIN, R.drawable.linkedin);
        
        login(); // Ensure we are always logged in
        
    }
    
  //NOTE: This happens when an activity becomes visible
  	@Override
      public void onStart() {
          super.onStart();
          if(D) Log.e(TAG, "++ ON START ++");

          // TODO: Find a way in 4.0 to turn on wifi
          
          // If BT is not on, request that it be enabled.
          // setupChat() will then be called during onActivityResult
          
          if (!mBluetoothAdapter.isEnabled()) {          
              // Enable the bluetooth adapter
              Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
              startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
          // Otherwise, setup the chat session
          } else {
              if (mMessageService == null) setupApp();
          }
         
  	}
  	
  	@Override
      public synchronized void onResume() {
          super.onResume();
          if(D) Log.e(TAG, "+ ON RESUME +");
          
          // Performing this check in onResume() covers the case in which BT was
          // not enabled during onStart(), so we were paused to enable it...
       // Note: onResume gets called when onStart does not if the dialog box doesn't completely cover the current activity.
          // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        //  if (mMessageService != null) {
              // Only if the state is STATE_NONE, do we know that we haven't started already
          //    if (mMessageService.getState() == CommunicationService.STATE_NONE) {
                // Start the Bluetooth chat services
            //    mMessageService.start();
             // }
          //else {
            //	  mMessageService.resume();
           // }
        //  }
          
          //WiFi P2P:
          registerReceiver(mReceiver, mIntentFilter);
          
      }
  	
  	private void setupApp() {
          Log.d(TAG, "setupApp()");  
       
          // TODO: While we are still using setContentView we will not always have a button to initialize.
          //   To restore basic functionality we call setupApp whenever a view changes
          if (findViewById(R.id.connect_button) != null) { 
          
        	  findViewById(R.id.connect_button).setOnClickListener(new OnClickListener() {
        		  public void onClick(View v) {
        			  btConnect();
        			  mMessageService.start();
        		  }
        	  });
          } else {
        	  Log.d(TAG, "connect_button null");
          }
          
          
          if (findViewById(R.id.check_common_friends_button) != null) {
        	  //once connected to a peer via Bluetooth or WiFi, check if there are any common friends
        	  findViewById(R.id.check_common_friends_button).setOnClickListener(new OnClickListener() {
        		  public void onClick(View v) {


        			  checkCommonFriends();
        		  }
        	  });
          } else {
        	  Log.d(TAG, "check_common_friends_button null");
          }
          
          
      }

  	
  	 @Override
      public synchronized void onPause() {
          super.onPause();
          if(D) Log.e(TAG, "- ON PAUSE -");
          
          //if(mMessageService != null)
            //mMessageService.pause();
          
          //WiFi P2P:
          unregisterReceiver(mReceiver);
      }

      @Override
      public void onStop() {// Name of the connected device
          super.onStop();
          if(D) Log.e(TAG, "-- ON STOP --");
      }

      @Override
      public void onDestroy() {
          super.onDestroy();
          
          if(D) Log.e(TAG, "--- ON DESTROY ---");

          if (mMessageService != null) {
          	mMessageService.stop();
          	mMessageService = null;
          }
      }
      
      /* 
       * Ron, show the menu bar
       * (non-Javadoc)
       * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
       */
      public boolean onCreateOptionsMenu(Menu menu) {
    	  MenuInflater inflater = getMenuInflater();
    	  inflater.inflate(R.menu.menubar, menu);
    	  return true;
      }
      
      /*Ron, handle the user's selected menu items
       * (non-Javadoc)
       * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
       */
      
      public boolean onOptionsItemSelected(MenuItem item) {
    	  switch (item.getItemId()) {
    	  case R.id.account:
    	  	  setContentView(R.layout.account);
    	  	  loadProfileFromFile();
    	  	  
    	  	  TextView t = new TextView(this);
    	  	  t = (TextView)findViewById(R.id.current_name);
    	  	  t.setText(myProfile.getFirstName() + " " + myProfile.getLastName());
    	  		  
    		  return true;
    	  case R.id.privacy:
    		  setContentView(R.layout.privacy);
    		  return true;
    	  case R.id.synchronization:
    		  setContentView(R.layout.synchronization);
    		  
    		  sharedPreference = getPreferences(Context.MODE_PRIVATE);
    		  Long lastSync = sharedPreference.getLong("lastSync", 0);
    		  Timestamp ts = new Timestamp(lastSync);
    		  
    		  TextView t2 = new TextView(this);
    		  t2 = (TextView)findViewById(R.id.lastSyncText);
    		  t2.setText(ts.toLocaleString()); 
    		  
    		  return true;
    	  case R.id.connection:
    		  setContentView(R.layout.connection);
    		  return true;
    	  default: return super.onOptionsItemSelected(item);
    	  }
      }
      
      private void btConnect() {
          if(D) Log.d(TAG, "bluetooth connect");
          
          // Initialize the CommunicationService to perform bluetooth connections
          //TODO: update this to use an interface instead
          //      This should allows us to move this out of this class
          if(benchmarkBandwidth){
          	mMessageService = new BluetoothServiceLogger(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
          }else {
          	mMessageService =  new BluetoothService(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
          }
          
          if (mBluetoothAdapter.getScanMode() !=
              BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
              Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
              discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
              startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
          }
          else{
        	// Launch the DeviceListActivity to see devices and do scan
        	launchDeviceList();
          }
      }
      
      private void wifiConnect() { 
    	  // TODO: How do we check if we already have permissions.
              // Since this is the system wireless settings activity, it's                                                                               
              // We will be notified by WiFiDeviceBroadcastReceiver instead.                     

    	  //TODO: Can this be done differently if we are notified by the broadcast receiver
    	  // We need communicate state via the handler
    	  // Do we need this turned on ?
    	 
          //startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));

    	  
    	  if(benchmarkBandwidth){
    		  //mMessageService = new BluetoothServiceLogger(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
          }else {
        	  mMessageService =  new WifiService(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
          }
            
    	  launchDeviceList();
    	  

    	  // TODO: Ron mentioned this was deprecated. Why? Is it really deprecated?
    	  mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
    		  @Override
    		  public void onSuccess() {
    			  if(D) Log.d(TAG, "Peer discovery successfull");
    		  }
    		  
    		  @Override
    		  public void onFailure(int reasonCode) {
    			  if(D) Log.d(TAG, "Peer discovery not successfull");
    		  }
    	  });
    	  
      }
      
   // The Handler that gets information back from the BluetoothService
      static class mHandler extends Handler {
    	  private final WeakReference<FriendFinderActivity> mTarget;
          mHandler(FriendFinderActivity target) {
        	  mTarget = new WeakReference<FriendFinderActivity>(target);
          }
    	  @Override
          public void handleMessage(Message msg) {
    		  FriendFinderActivity target = mTarget.get();
    		  if(target == null)
    			  return;
    		  
              switch (msg.what) {
              case   BluetoothService.MESSAGE_STATE_CHANGE:
                  if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                  switch (msg.arg1) {
                  case CommunicationService.STATE_CONNECTED:
                  	target.finishActivity( REQUEST_CONNECT_DEVICE_SECURE );
                      
                  	//setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
           	
                  	//if(connectionIndicator != null) connectionIndicator.dismiss();
                  	//TODO: Start the protocol here
                  	
                  	
                  	
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
            	  byte[] readBuf = (byte[]) msg.obj;
                  // construct a string from the valid bytes in the buffer        
                  String readMessage = new String(readBuf, 0, msg.arg1);

                  

                  //user pressed send button (for test purposes only)
                   Toast.makeText(target.getApplicationContext(), "Message read " 
                  	  + readMessage, Toast.LENGTH_SHORT).show();
                
                  break;
                  
    
                  
              case CommunicationService.MESSAGE_DEVICE_NAME:
                  // save the connected device's name
                  target.mConnectedDeviceName = msg.getData().getString(CommunicationService.DEVICE_NAME);
                  Toast.makeText(target.getApplicationContext(), "Connected to "
                                 + target.mConnectedDeviceName, Toast.LENGTH_SHORT).show();             
                  break;      	    
              case CommunicationService.MESSAGE_TOAST:
                  // For the usability test we mute toasts
              	  Toast.makeText(target.getApplicationContext(), msg.getData().getString(CommunicationService.TOAST),
                                 Toast.LENGTH_SHORT).show();
                  break;
              case CommunicationService.MESSAGE_FAILED:
              	// Reset the ui
              	//if(connectionIndicator != null) connectionIndicator.dismiss();
              	
              	Intent serverIntent = new Intent(target, DeviceList.class);
      	        target.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
      	        break;
          }
      }};
      
      
      public void getContactsAsync() {
    	 
    	  try {
    		  adapter.getContactListAsync(new SocialContactListener(this)); 
    	  } catch (Exception e) {
    		  Log.d(TAG, e.getMessage());
    	  }
    	  
    	  new ContactDownloader().execute(); //perform the contact downloading as an async. task
    	  
      }
      
/* Ron
 * 
 */
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
      
/* Ron
 * 
 */
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
      
      /*
       * Ron, the CA's authorization for the set of contacts has been saved to file before and needs to be loaded to be able to do the PSI
       */
      public void loadAuthorizationFromFile() {
    	authObj = new AuthorizationObject();
    	String filename = "authorization";
    	FileInputStream fileInput;
		ObjectInputStream objectInput; 
		
		try {
			fileInput = this.openFileInput(filename);
			objectInput = new ObjectInputStream(fileInput);
			authObj.readObject(objectInput);
			objectInput.close();
			Log.d(TAG, "Authorization loaded");
				
		} catch (Exception e) {
			Log.d(TAG, "Authorization has not been granted so far");
			Toast.makeText(this, "You need to get a certification for your friends first", Toast.LENGTH_SHORT).show();
		}
      }
      
/* Ron
 * 
 */
      public void checkCommonFriends() {
    	  
    	  // TODO: Initiate a connection using the ATWPSI class
    	  
    	  //assume established communication channel with peer 
    	  loadProfileFromFile(); //need to get our own profile information first
    	  
    	  //tell the peer who he is dealing with  
    	  //"1" + 
    	  //mMessageService.write(myProfile.getFirstName() + " " + myProfile.getLastName()); //we should also provide the id here so that the connecting can be done later on
    	  //state = 2;
    	  
    	  // input should be something of the form
    	  // loadProfileFromFile()
    	  // loadAuthorizationFromFile()
    	  
    	  // TODO: We may want to accept arbitrary objects as inputs to conduct test
    	  //   The easiest way to do that is to provide some form of interface. Like (PSI_Input)
    	  // conductTest( contactList.collect{|x| x.id} )
    	  
      }
      
      //Ron: these are the implemented interface methods from CommonFriendsDialogFragment. Here we receive a callback when a button is pressed
      public void onDialogPositiveClick(DialogFragment dialog) {
    	  Toast.makeText(this, "You are now friends", Toast.LENGTH_SHORT).show();
    	  //TODO: initiate the "becoming friends" process here
    	  //Toast.makeText(this, "You are now friends with " + currentPeer.getName(), Toast.LENGTH_SHORT).show();
      }
      
      public void onDialogNegativeClick(DialogFragment dialog) {
    	  //user doesn't want to get friends with the peer. Store this event here so that the user isn't shown this peer again (for a while)
      }
      
      public void getProfileAsync() {

    	  adapter.getUserProfileAsync(new SocialProfileListener(this)); //the actual activity (which is the context) needs to be provided to the Listener
      }
      
      //Called when INTENT is returned
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
          if(D) Log.d(TAG, "onActivityResult " + resultCode);
          switch (requestCode) {
          case REQUEST_CONNECT_DEVICE_SECURE:
              // When DeviceListActivity returns with a device to connect
              if (resultCode == Activity.RESULT_OK) {
                  connectDevice(data, true);
              }
              break;
          case REQUEST_ENABLE_BT:
              // When the request to enable Bluetooth returns
        	  
        	  // TODO: This needs to be refactored into the bluetooth service
        	  //   We do not need to close the app if bluetooth isn't enabled
              if (resultCode == Activity.RESULT_OK) {
                  // Bluetooth is now enabled, so set up a chat session
                  setupApp();
                  
              } else {
                  // User did not enable Bluetooth or an error occurred
                  Log.d(TAG, "BT not enabled");
                  Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                  finish();
              }
          case REQUEST_DISCOVERABLE:
          	//TODO: think about what if the user presses no?
          	if( resultCode == RESULT_CANCELED ){
          		// for now we do nothing
          	}
          		
          	launchDeviceList();
          }
      }
      
      private void connectDevice(Intent data, boolean secure) {
      	// Check that we're actually connected before trying anything
          if (mMessageService.getState() == CommunicationService.STATE_CONNECTED) {
              Toast.makeText(this, R.string.already_connected, Toast.LENGTH_SHORT).show();
              return;
          }
      	//connectionIndicator = ProgressDialog.show(this, "", "Please wait while we connect you.");
      	
          // Get the device MAC address
          String address = data.getExtras()
              .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
        
          // Attempt to connect to the device
          mMessageService.connect(address, secure);
      }
      
      /**
       * Start the device list activity. If no service is started has no affect.
       */
      private void launchDeviceList(){
    	  if(mMessageService == null)
    		  return;
    	  
    	 
    	  Time now = new Time();
    	  now.setToNow();
  	
    	  CommunicationService.com_transfers.put(now.format2445(), new WeakReference<CommunicationService>(mMessageService));
    	  // Launch the DeviceListActivity to see devices and do scan
    	  Intent serverIntent = new Intent(this, DeviceList.class);
    	  serverIntent.putExtra(CommunicationService.EXTRA_SERVICE_TRANFER, now.format2445());
    	  startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
      }
      
      public void syncnow(View view) {
    	  getContactsAsync();
    	  getProfileAsync();
    	  
    	  sharedPreference = getPreferences(Context.MODE_PRIVATE);
    	  SharedPreferences.Editor editor = sharedPreference.edit();
    	  Timestamp ts = new Timestamp(System.currentTimeMillis());
    	  Long time = ts.getTime();
    	  editor.putLong("lastSync", time);
    	  editor.commit();
    	  
    	  TextView t2 = new TextView(this);
		  t2 = (TextView)findViewById(R.id.lastSyncText);
		  t2.setText(ts.toLocaleString()); 
      }
      
      public void backtomain(View view) {
    	  setContentView(R.layout.main);
    	  setupApp();
      }

      
      public void login(View view) {
    	  login();
      }
      
      public void login() {
    	  
    	  /** As of now it seems that the linkedin authenticator will not work. We should revist this later.
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
    	  // LinkedIn app is not installed. Fall back to socialauth. 
    	  else { 	 */ 
    		  if(D) Log.d(TAG, "Logging in via socialauth");
        	  adapter.authorize(this, Provider.LINKEDIN);  
    	  //}
      }
      
      public void logout(View view) {
    	  
    	  // TODO: Use reflection to see if we are signed in already.
    	  // otherwise we get an error
    	  
    	  
    	  if(D) Log.d(TAG, "Loggingout from socialauth");
    	  try {
    		  // TODO: Does this actually log you out. It appears like it does not
    		  adapter.signOut(Provider.LINKEDIN.name());    
    	  } catch (Exception e) {
    		  Log.e(TAG,"Could not logout. Are you logged in", e);
    	  }
    	  
    	  // TODO: We need to also delete saved information here.
      }
}