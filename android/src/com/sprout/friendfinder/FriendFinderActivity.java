package com.sprout.friendfinder;

import java.lang.ref.WeakReference;
import java.util.List;

import org.brickred.socialauth.Contact;
import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;
import org.brickred.socialauth.android.SocialAuthError;

import com.sprout.finderlib.DeviceList;
import com.sprout.finderlib.PrivateProtocol;
import com.sprout.finderlib.BluetoothServiceLogger;
import com.sprout.finderlib.BluetoothService;
import com.sprout.finderlib.CommunicationService;
import com.sprout.finderlib.WifiService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class FriendFinderActivity extends Activity {
	
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
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    
    // UI elements
    private Button mBtButton;
    private Button mWifiButton;
    
    private SocialAuthAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);
        if(D) Log.d(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // TODO: Find a way to see if p2pwifi is supported
        
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
          // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
          //if (mMessageService != null) {
              // Only if the state is STATE_NONE, do we know that we haven't started already
          //    if (mMessageService.getState() == CommunicationService.STATE_NONE) {
                // Start the Bluetooth chat services
          //      mMessageService.start();
          //    }
              //else {
            	//  mMessageService.resume();
              //}
          //}
      }
  	
  	private void setupApp() {
          Log.d(TAG, "setupApp()");  

          mBtButton = (Button) findViewById(R.id.bt_button);
          mBtButton.setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                btConnect();
                mMessageService.start();
              }
          });
          
          mWifiButton = (Button) findViewById(R.id.wifi_button);
          mWifiButton.setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                  wifiConnect();
                  mMessageService.start();
                }
            });
          
          findViewById(R.id.send_button).setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                  mMessageService.write("Hello");
              }
          });
          
          findViewById(R.id.contacts_button).setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                  getContacts();
              }
          });
          
          
          
      }

  	
  	 @Override
      public synchronized void onPause() {
          super.onPause();
          if(D) Log.e(TAG, "- ON PAUSE -");
          
          //if(mMessageService != null)
          //  mMessageService.pause();
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
          if (mMessageService != null) {
          	mMessageService.stop();
          	mMessageService = null;
          }
          
      }
      
      private void btConnect() {
          if(D) Log.d(TAG, "bluetooth connect");
          
          // Initialize the CommunicationService to perform bluetooth connections
          //TODO: update this to use an interace instead
          //      This should alow us to move this out of this class
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
      
      public List<Contact> getContacts(){
    	  
    	  List<Contact> contactsList = adapter.getContactList();          
          
    	  if (contactsList != null && contactsList.size() > 0) 
    	  {
    	     for (Contact p : contactsList)
    	     {
    	          if (TextUtils.isEmpty(p.getFirstName()) && TextUtils.isEmpty(p.getLastName())) 
    	          {                                                   
    	              p.setFirstName(p.getDisplayName());
    	          }
    	                                                  
    	          Log.d(TAG , "Display Name = " +  p.getDisplayName());
    	          Log.d(TAG , "First Name   = " +  p.getFirstName());
    	          Log.d(TAG , "Last Name    = " +  p.getLastName());
    	          Log.d(TAG , "Contact ID   = " +  p.getId());
    	          Log.d(TAG , "Profile URL  = " +  p.getProfileUrl());  
    	                                                  
    	     }    
    	  }
    	  
    	  return contactsList;
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
}