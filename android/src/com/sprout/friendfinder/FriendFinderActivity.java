package com.sprout.friendfinder;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;

import com.sprout.finderlib.DeviceList;
import com.sprout.finderlib.PrivateProtocol;
import com.sprout.finderlib.BluetoothServiceLogger;
import com.sprout.finderlib.BluetoothService;
import com.sprout.finderlib.CommunicationService;
import com.sprout.finderlib.WifiService;
import com.sprout.friendfinder.CommonFriendsDialogFragment.NoticeCommonFriendsDialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
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

//Ron: this class now implements also the NoticeCommonFriendsDialog interface so that we can get feedback here which button the user pressed when shown the dialog whether he wants to become friends with the peer or not

public class FriendFinderActivity extends Activity implements NoticeCommonFriendsDialog {
	
	private static final String TAG = "MainActivity";
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
    
    //WiFi P2P
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
    
    //these variables are needed for the PSI protocol -> they should rather be put to the message handler class as they are used there...
    CurrentPeer currentPeer;
    BigInteger Ra, Rb, N, e;
    List<BigInteger> zis;
    List<BigInteger> yis;
    
    //keep track of state the PSI protocol currently is in
    int state = 1;
    int peerSetSize = 0;
    List<BigInteger> peerSet;
    BigInteger peerAuth;
    List<BigInteger> T;
    List<BigInteger> T2;
    
    
    ProfileObject myProfile;
    
    List<ProfileObject> contactList;
    AuthorizationObject authObj;
    
    //to store key-value pairs: 
    SharedPreferences sharedPreference;
    
   
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {        
    	
    	//Ron., Aug. 06: just for now: as the connection to the CA should be done in an async. task later on... 
    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	StrictMode.setThreadPolicy(policy);
    	
    	
    	super.onCreate(savedInstanceState);
        if(D) Log.d(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        
        // TODO: Find a way to see if p2pwifi is supported
        //Ron., Aug. 06: commented out:
        /*
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
        */
        
        
        //Ron., Aug. 06: commented out as Bluetooth is not supported in emulator:
        /*
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        */
        
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
        
        //TODO: This is probably not the place for this
        adapter.authorize(this, Provider.LINKEDIN);
        
    }
    
  //NOTE: This happens when an activity becomes visible
  	@Override
      public void onStart() {
          super.onStart();
          if(D) Log.e(TAG, "++ ON START ++");

          // TODO: Find a way in 4.0 to turn on wifi
          
          // If BT is not on, request that it be enabled.
          // setupChat() will then be called during onActivityResult
          
          
          //Ron, Aug. 06: commented out as it does not run in the emulator with Bluetooth on:
          /*
          if (!mBluetoothAdapter.isEnabled()) {          
              // Enable the bluetooth adapter
              Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
              startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
          // Otherwise, setup the chat session
          } else {
              if (mMessageService == null) setupApp();
          }
          */
          
          
          
  	}
  	
  	@Override
      public synchronized void onResume() {
          super.onResume();
          if(D) Log.e(TAG, "+ ON RESUME +");
          
          // Performing this check in onResume() covers the case in which BT was
          // not enabled during onStart(), so we were paused to enable it...
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
          //Ron, Aug. 08: commented out:
          //registerReceiver(mReceiver, mIntentFilter);
          
      }
  	
  	private void setupApp() {
          Log.d(TAG, "setupApp()");  
       
          
          findViewById(R.id.connect_button).setOnClickListener(new OnClickListener() {
        	  public void onClick(View v) {
        		  
        		  //!!!!Ron: Aug. 06: stopped Bluetooth for now to be able to test app in emulator
        		  //btConnect();
        		  //mMessageService.start();
        	  }
          });
          
          
          //once connected to a peer via Bluetooth or WiFi, check if there are any common friends
          findViewById(R.id.check_common_friends_button).setOnClickListener(new OnClickListener() {
        	  public void onClick(View v) {
        		  
        		  
        		  checkCommonFriends();
        	  }
          });
          
          
      }

  	
  	 @Override
      public synchronized void onPause() {
          super.onPause();
          if(D) Log.e(TAG, "- ON PAUSE -");
          
          //if(mMessageService != null)
            //mMessageService.pause();
          
          //WiFi P2P:
          //Ron., Aug. 06: commented out:
          //unregisterReceiver(mReceiver);
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
                 
          // Stop the message service
          //Ron, Aug. 06: commented out:
          /*
          if (mMessageService != null) {
          	mMessageService.stop();
          	mMessageService = null;
          }
          */
          
          
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
    	  

    	  /* Ron: deprecated:
    	  
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
    	  */
    	  
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

                  
                //Ron: perform the PSI's protocol steps here - this should be moved to class ATWPSI later on! 
                  
                    
                  if (mTarget.get().state == 1) { //readMessage.startsWith("1")
                	  //Toast.makeText(target.getApplicationContext(), "You are connected to " 
                		//	  + readMessage + " now. This does not mean that you are already Linkedin contacts.", Toast.LENGTH_SHORT).show(); //.substring(1)
                	  mTarget.get().loadProfileFromFile(); //need to get our own profile information first
                	  //set the current peer:
                	  
                	  mTarget.get().currentPeer = new CurrentPeer(); //currentPeer could be moved to this class... 
                	  //currentPeer.setId();
                	  //mTarget.get().currentPeer.setName(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName());
                	  mTarget.get().currentPeer.setName(readMessage);
                	  
                	  //"2" + 
                	  mTarget.get().mMessageService.write(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName()); //we should also provide the Linkedin ID here so that the connecting can be done later on
                	  
                	  //set my own state for the next phase to the next level
                	  mTarget.get().state = 3;
                  }
                 
                  else if (mTarget.get().state == 2) { //readMessage.startsWith("2")
                	  //Toast.makeText(target.getApplicationContext(), "You are connected to " 
                	//		  + readMessage + " now. This does not mean that you are already Linkedin contacts.", Toast.LENGTH_SHORT).show(); //.substring(1)
                	
                	  
                	  //set the current peer:
                	  mTarget.get().currentPeer = new CurrentPeer(); //currentPeer could be moved to this class... 
                	  //currentPeer.setId();
                	  //mTarget.get().currentPeer.setName(mTarget.get().myProfile.getFirstName() + " " + mTarget.get().myProfile.getLastName());
                	  mTarget.get().currentPeer.setName(readMessage);
                	  
                	  
                	//do the PSI with the current peer now:
              	    //get my own contactList (as we are offline now, we need to load the saved contact list from the file)
              	    mTarget.get().loadContactsFromFile();
              	    
              	    //load authorization data from file as well:
              	    mTarget.get().loadAuthorizationFromFile();
              	    
              	    mTarget.get().Ra = mTarget.get().authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts
              	    mTarget.get().N = mTarget.get().authObj.getN();
              	    mTarget.get().e = mTarget.get().authObj.gete();
              	    mTarget.get().zis = new ArrayList<BigInteger>();
              	    for (ProfileObject pro : mTarget.get().contactList) { //for all elements in my own contact list
              	    	mTarget.get().zis.add(mTarget.get().hash(pro.getId().getBytes(), (byte)0).modPow(mTarget.get().Ra, mTarget.get().N));
              	    }
              	    
              	    
              	    //send zis + auth to peer:
              	      //send number of zis first, so that peer knows how many BigInt-Reads it has to conduct:
              	    Integer s = mTarget.get().zis.size();
              	    mTarget.get().mMessageService.write(s.toString());
              	      
              	      
              	    mTarget.get().state = 4;
                  
                  }
                                  
          
                  else if (mTarget.get().state == 3) {
                	 //B receives A's number of zis
                	  mTarget.get().peerSetSize = Integer.parseInt(readMessage);
                	  mTarget.get().peerSet = new ArrayList<BigInteger>();
                	  
                	  
                	  //B initializes itself
                	  mTarget.get().loadContactsFromFile();
                	    
                	  //load authorization data from file as well:
                	  mTarget.get().loadAuthorizationFromFile();
                	    
                	  mTarget.get().Rb = mTarget.get().authObj.getR(); //Rc as chosen by the CA when certifying the set of contacts


                	  mTarget.get().N = mTarget.get().authObj.getN();
                	  mTarget.get().e = mTarget.get().authObj.gete();
                	  mTarget.get().yis = new ArrayList<BigInteger>();
                	  for (ProfileObject pro : mTarget.get().contactList) { //for all elements in my own contact list
                	    mTarget.get().yis.add(mTarget.get().hash(pro.getId().getBytes(), (byte)0).modPow(mTarget.get().Rb, mTarget.get().N));
                	  }
                	    
                	  //B sends number of yis to A:
                	  Integer s = mTarget.get().yis.size();
                	  mTarget.get().mMessageService.write(s.toString());
                	    
                	    
                	  mTarget.get().state = 5;
                
                  }
                  
                  else if (mTarget.get().state == 4) {
                	  //A receives B's number of yis
                	  mTarget.get().peerSetSize = Integer.parseInt(readMessage);
                	  mTarget.get().peerSet = new ArrayList<BigInteger>();
                	  
                	  //A sends zis to B
                	  for (int i = 0; i < mTarget.get().zis.size(); i++) {
              	    	  mTarget.get().mMessageService.write(mTarget.get().zis.get(i));
              	      }
                	  
                	  mTarget.get().state = 6;
                	  
                  }
                  
                  else if (mTarget.get().state == 5) {
                	  //B receives A's zis
                	  mTarget.get().peerSet.add(new BigInteger(readBuf));
            	  		  
            	  	  if (mTarget.get().peerSet.size() == mTarget.get().peerSetSize) { //i.e. received all elements
            	  		//B sends its yis to A:
                    	  for (int i = 0; i < mTarget.get().yis.size(); i++) {
                    		  mTarget.get().mMessageService.write(mTarget.get().yis.get(i));
                    	  }
            	  		  
            	  		  mTarget.get().state = 7;
            	  	  }
            	  	           	  	  
                  }
                  
                  else if (mTarget.get().state == 6) {
                	  //A receives B's yis
                	  mTarget.get().peerSet.add(new BigInteger(readBuf));
                	  
                	  if (mTarget.get().peerSet.size() == mTarget.get().peerSetSize) {
                		//A sends auth to B:
                    	  mTarget.get().mMessageService.write(mTarget.get().authObj.getAuth());
                		  
                		  mTarget.get().state = 8;
                	  }
                	  
                  }
                  
                  else if (mTarget.get().state == 7) {
                	  //B receives A's auth:
                	  mTarget.get().peerAuth = new BigInteger(readBuf);
                	  
                	  //B checks A's auth:
                	  BigInteger verification = mTarget.get().peerAuth.modPow(mTarget.get().e, mTarget.get().N);
                	  BigInteger toVerify = BigInteger.ONE;
                	  for (BigInteger toverifyi : mTarget.get().peerSet) {
                		  toVerify = toVerify.multiply(toverifyi).mod(mTarget.get().N);
                	  }
                	  
                	  toVerify = mTarget.get().hash(toVerify.toByteArray(), (byte)0).mod(mTarget.get().N);
                	  if (verification.equals(toVerify)) {
                		  Log.d(TAG, "B: Contact set verification succeeded");
                	  }
                	  else {
                		  Log.d(TAG, "B: Contact set verification failed");
            	    	  Log.d(TAG, "received verification tag: " + verification.toString());
            	    	  Log.d(TAG, "computed verification tag: " + toVerify.toString());
                	  }
                	  
                	  //B sends its aut to A:
                	  mTarget.get().mMessageService.write(mTarget.get().authObj.getAuth());
                	  
                	  //prepare for receiving computed tags later on:
                	  mTarget.get().T = new ArrayList<BigInteger>();
                	  mTarget.get().T2 = new ArrayList<BigInteger>();
                	  
                	  mTarget.get().state = 9;
                  }
                  
                  else if (mTarget.get().state == 8) {
                	  //A received B's auth:
                	  mTarget.get().peerAuth = new BigInteger(readBuf);
                	  
                	  //A checks B's auth:
                	  BigInteger verification = mTarget.get().peerAuth.modPow(mTarget.get().e, mTarget.get().N);
                	  BigInteger toVerify = BigInteger.ONE;
                	  for (BigInteger toverifyi : mTarget.get().peerSet) {
                		  toVerify = toVerify.multiply(toverifyi).mod(mTarget.get().N);
                	  }
                	  
                	  toVerify = mTarget.get().hash(toVerify.toByteArray(), (byte)0).mod(mTarget.get().N);
                	  if (verification.equals(toVerify)) {
                		  Log.d(TAG, "A: Contact set verification succeeded");
                	  }
                	  else {
                		  Log.d(TAG, "A: Contact set verification failed");
            	    	  Log.d(TAG, "received verification tag: " + verification.toString());
            	    	  Log.d(TAG, "computed verification tag: " + toVerify.toString());
                	  }
                	  
                	  
                	  //A performs computations on yis and sends them to B:
                	  mTarget.get().T = new ArrayList<BigInteger>();
                	  for (BigInteger t1i : mTarget.get().peerSet) {
                		  t1i = t1i.modPow(mTarget.get().Ra, mTarget.get().N);
                		  mTarget.get().T.add(t1i);
                		  mTarget.get().mMessageService.write(t1i);
                	  }
                	  
                	  //prepare for receiving B's computed tags
                	  mTarget.get().T2 = new ArrayList<BigInteger>();
                	  
                	  mTarget.get().state = 10;
                  }
                  
                  else if (mTarget.get().state == 9) {
                	  //B receives A's computed tags:
                	  mTarget.get().T.add(new BigInteger(readBuf));
                	  
                	  
                	  
                	  if (mTarget.get().yis.size() == mTarget.get().T.size()) {
                		  //B performs computations on zis and sends them to A:
                		  for (BigInteger t2i : mTarget.get().peerSet) {
                			  t2i = t2i.modPow(mTarget.get().Rb, mTarget.get().N);
                			  mTarget.get().T2.add(t2i);
                			  mTarget.get().mMessageService.write(t2i);
                		  }
                		  
                		  
                		  //output common contacts:
                		  int i0 = 0;
                		  List<String> inCommon2 = new ArrayList<String>();
                		  for (int l = 0; l < mTarget.get().T.size(); l++) {
                			  for (int l2 = 0; l2 < mTarget.get().T2.size(); l2++) {
                				  if (mTarget.get().T.get(l).equals(mTarget.get().T2.get(l2))) {
                				  inCommon2.add(mTarget.get().contactList.get(l2).getFirstName() + " " + mTarget.get().contactList.get(l2).getLastName());
                				  }
                				  }
                		  }
                		  Log.d(TAG, "Found " + inCommon2.size() + " common friends");
                		  
                		  String[] inCommon = new String[inCommon2.size()];
                		  for (int l = 0; l < inCommon2.size(); l++) {
                			  inCommon[l] = inCommon2.get(l);
                		  }
                		  mTarget.get().currentPeer.setCommonFriends(inCommon);
                		  
                		  //now show the result of the PSI to the user in a dialog:
                	      CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragment
                	      newFragment.setCurrentPeer(mTarget.get().currentPeer);
                	      newFragment.show(mTarget.get().getFragmentManager(), "check-common-friends");
            	  
                	      //the user's response to the "become friends" question is handled in the methods below
             		  
             		  
                		  mTarget.get().state = 1;
                	  }
                	  
                  }
                  
                  else if (mTarget.get().state == 10) {
                	  //A receives B's computed tags:
                	  mTarget.get().T2.add(new BigInteger(readBuf));
                	  
                	  
                	  
                	  if (mTarget.get().zis.size() == mTarget.get().T2.size()) {
                		  //output common contacts:
                		  int i0 = 0;
                		  List<String> inCommon2 = new ArrayList<String>();
                		  for (int l = 0; l < mTarget.get().T2.size(); l++) {
                			  for (int l2 = 0; l2 < mTarget.get().T.size(); l2++) {
                				  if (mTarget.get().T.get(l).equals(mTarget.get().T2.get(l2))) {
                				  inCommon2.add(mTarget.get().contactList.get(l2).getFirstName() + " " + mTarget.get().contactList.get(l2).getLastName());
                			  
                				  }
                			  }
                		  }
                		  Log.d(TAG, "Found " + inCommon2.size() + " common friends");
                		  
                		  String[] inCommon = new String[inCommon2.size()];
                		  for (int l = 0; l < inCommon2.size(); l++) {
                			  inCommon[l] = inCommon2.get(l);
                		  }
                		  mTarget.get().currentPeer.setCommonFriends(inCommon);
                		  
                		  //now show the result of the PSI to the user in a dialog:
                	      CommonFriendsDialogFragment newFragment = new CommonFriendsDialogFragment(); //DialogFragment
                	      newFragment.setCurrentPeer(mTarget.get().currentPeer);
                	      newFragment.show(mTarget.get().getFragmentManager(), "check-common-friends");
            	  
                	      //the user's response to the "become friends" question is handled in the methods below
                		  
                		  mTarget.get().state = 1; 
                	  }
                	  
                  }
  	  
            	  else {
                	   
                	  //user pressed send button (for test purposes only)
                	 // Toast.makeText(target.getApplicationContext(), "Message read " 
            		//	  + readMessage, Toast.LENGTH_SHORT).show();
                  }
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
  				Log.d("Friend Finder Activity", "Profile loaded");
  				Log.d("Friend Finder Activity", "Name: " + myProfile.getFirstName() + " " + myProfile.getLastName());
  				//Toast.makeText(this, "Your offline profile is: " + myProfile.getFirstName() + " " + myProfile.getLastName(), Toast.LENGTH_SHORT).show();
  			} catch (Exception e) {
  				Log.d("Friend Finder Activity", "File has not been downloaded so far");
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
  			Log.d("Friend Finder Activity", "Contacts loaded");
  				
  			//Toast.makeText(this, "Your " + contactList.size() + " offline contacts have been loaded", Toast.LENGTH_SHORT).show();
  		} catch (Exception e) {
  			Log.d("Friend Finder Activity", "File has not been downloaded so far");
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
			Log.d("Friend Finder Activity", "Authorization loaded");
				
		} catch (Exception e) {
			Log.d("Friend Finder Activity", "Authorization has not been granted so far");
			Toast.makeText(this, "You need to get a certification for your friends first", Toast.LENGTH_SHORT).show();
		}
      }
      
/* Ron
 * 
 */
      public void checkCommonFriends() {
    	  
    	  //assume established communication channel with peer 
    	  loadProfileFromFile(); //need to get our own profile information first
    	  
    	  //tell the peer who he is dealing with
    	  
    	  //"1" + 
    	  mMessageService.write(myProfile.getFirstName() + " " + myProfile.getLastName()); //we should also provide the id here so that the connecting can be done later on
    	  state = 2;
    	  
    	  //from now on, the protocol is performed in the message handler class above
    	  
      }
      
      //Ron: these are the implemented interface methods from CommonFriendsDialogFragment. Here we receive a callback when a button is pressed
      public void onDialogPositiveClick(DialogFragment dialog) {
    	  //initiate the "becoming friends" process here
    	  Toast.makeText(this, "You are now friends with " + currentPeer.getName(), Toast.LENGTH_SHORT).show();
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
      
      //this functionality should be provided by some other class; this is not the right place for it
      protected BigInteger hash(byte [] message, byte selector){
  		
  		// input = selector | message
  		byte [] input = new byte[message.length + 1];
  		System.arraycopy(message, 0, input, 1, message.length);
  		input[0] = selector;
  		
  		MessageDigest digest = null;
  		try {
  			digest = MessageDigest.getInstance("SHA-1");
      	} catch (NoSuchAlgorithmException e1) {
      		Log.e(TAG, "SHA-1 is not supported");
      		return null; // TODO: Raise an error?
      	}
  		digest.reset();
      	    
  		return new BigInteger(digest.digest(input));
  	}
      
    //this functionality should be provided by some other class; this is not the right place for it
      protected BigInteger randomRange(BigInteger range){
    		//TODO: Is there anything else we should fall back on here perhaps openssl bn_range
    		//         another option is using an AES based key generator (the only algorithim supported by android)
    		
    		// TODO: Should we be keeping this rand around? 
    		SecureRandom rand = new SecureRandom();
    		BigInteger temp = new BigInteger(range.bitLength(), rand);
    		while(temp.compareTo(range) >= 0 || temp.equals(BigInteger.ZERO)){
    			temp = new BigInteger(range.bitLength(), rand);
    		}
    		return temp;
    		
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
      }
      
      public void login(View view) {
    	
      }
      
      public void logout(View view) {
    	  
    	  
      }
}