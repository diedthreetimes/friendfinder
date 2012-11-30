package com.sprout.friendfinder;

import java.lang.ref.WeakReference;

import com.sprout.finderlib.DeviceList;
import com.sprout.finderlib.PrivateProtocol;
import com.sprout.finderlib.BluetoothServiceLogger;
import com.sprout.finderlib.BluetoothService;
import com.sprout.finderlib.CommunicationService;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class FriendFinderActivity extends Activity {
	
	private static final String TAG = "ConductTest";
    private static final boolean D = true;
    private static final boolean benchmarkBandwidth = true;
    
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);
        if(D) Log.d(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    
  //NOTE: This happens when an activity becomes visible
  	@Override
      public void onStart() {
          super.onStart();
          if(D) Log.e(TAG, "++ ON START ++");

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
          if (mMessageService != null) {
              // Only if the state is STATE_NONE, do we know that we haven't started already
              if (mMessageService.getState() == CommunicationService.STATE_NONE) {
                // Start the Bluetooth chat services
                mMessageService.start();
              }
              else {
            	  mMessageService.resume();
              }
          }
      }
  	
  	private void setupApp() {
          Log.d(TAG, "setupApp()");  

          mBtButton = (Button) findViewById(R.id.bt_button);
          mBtButton.setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                btConnect();
                
                // Initialize the BluetoothService to perform bluetooth connections
                //TODO: update this to use an interace instead
                //      This should alow us to move this out of this class
                if(benchmarkBandwidth){
                	mMessageService = new BluetoothServiceLogger(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
                }else {
                	mMessageService =  new BluetoothService(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
                }
              }
          });
          
          mWifiButton = (Button) findViewById(R.id.wifi_button);
          mWifiButton.setOnClickListener(new OnClickListener() {
              public void onClick(View v) {
                  wifiConnect();
                  

                  if(benchmarkBandwidth){
                	 // TODO: Fix the logger mechanisim to work for any service
                  	//mMessageService = new WifiServiceLogger(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
                  }else {
                  	//mMessageService = new WifiService(FriendFinderActivity.this, new mHandler(FriendFinderActivity.this));
                  }
                }
            });
          
          
          
          
          
      }

  	
  	 @Override
      public synchronized void onPause() {
          super.onPause();
          if(D) Log.e(TAG, "- ON PAUSE -");
          
          if(mMessageService != null)
        	  mMessageService.pause();
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
          
          // TODO: What if the system kills the mMessageService
          // Stop the Bluetooth chat services
          if (mMessageService != null) {
          	mMessageService.stop();
          	mMessageService = null;
          }
          
      }
      
      private void btConnect() {
          if(D) Log.d(TAG, "bluetooth connect");
          
          // Commented out for test
          if (mBluetoothAdapter.getScanMode() !=
              BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
              Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
              discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
              startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
          }
          else{
        	// Launch the DeviceListActivity to see devices and do scan
    	    Intent serverIntent = new Intent(this, DeviceList.class);
    	    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
          }
      }
      
      private void wifiConnect() {
    	  // TODO: We probably need to turn wifi on here.
      
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
                  case BluetoothService.STATE_CONNECTED:
                  	target.finishActivity( REQUEST_CONNECT_DEVICE_SECURE );
                      
                  	//setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
           	
                  	//if(connectionIndicator != null) connectionIndicator.dismiss();
                  	if(D) Log.d(TAG,"mStartTest + mOther: false");
                      break;
                  case BluetoothService.STATE_CONNECTING:
                      //setStatus(R.string.title_connecting);
                      break;
                  case BluetoothService.STATE_LISTEN:
                  case BluetoothService.STATE_NONE:
                      //setStatus(R.string.title_not_connected);
                      break;
                  }
                  break;
              case BluetoothService.MESSAGE_READ:
                  break;
              case BluetoothService.MESSAGE_DEVICE_NAME:
                  // save the connected device's name
                  target.mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                  Toast.makeText(target.getApplicationContext(), "Connected to "
                                 + target.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                  break;      	    
              case BluetoothService.MESSAGE_TOAST:
                  // For the usability test we mute toasts
              	//Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                  //               Toast.LENGTH_SHORT).show();
                  break;
              case BluetoothService.MESSAGE_FAILED:
              	// Reset the ui
              	//if(connectionIndicator != null) connectionIndicator.dismiss();
              	
              	Intent serverIntent = new Intent(target, DeviceList.class);
      	        target.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
      	        break;
          }
      }};
      
      
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
          	
  	        // Launch the DeviceListActivity to see devices and do scan
  	        Intent serverIntent = new Intent(this, DeviceList.class);
  	        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
          }
      }
      
      private void connectDevice(Intent data, boolean secure) {
      	// Check that we're actually connected before trying anything
          if (mMessageService.getState() == BluetoothService.STATE_CONNECTED) {
              Toast.makeText(this, R.string.already_connected, Toast.LENGTH_SHORT).show();
              return;
          }
      	//connectionIndicator = ProgressDialog.show(this, "", "Please wait while we connect you.");
      	
          // Get the device MAC address
          String address = data.getExtras()
              .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
          // Get the BluetoothDevice object
        
          // Attempt to connect to the device
          mMessageService.connect(address, secure);
      }
}