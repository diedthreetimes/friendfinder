package com.sprout.friendfinder.ui;

import java.sql.Timestamp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;

/* 
 * Ron: this class now implements also the NoticeCommonFriendsDialog interface so that we can get feedback
 *  here which button the user pressed when shown the dialog whether he wants to become friends with the peer or not
 */


// Main TODO: Move the core communication functionality into a Service class
// This service class should bypass the DeviceList, in doing so it must implement callbacks for onDiscovery, and onDiscovery complete.
// It is important to note, that attempting to connect to a device should not happen until discovery is completed.
// We may want to integrate this into the bluetooth service, i'm not sure. 

public class MainActivity extends Activity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final boolean D = true;

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE = 1; // Intent code for the Device List
  private static final int REQUEST_ENABLE_BT = 3; 
  private static final int REQUEST_DISCOVERABLE = 4;

  private BluetoothAdapter mBluetoothAdapter = null;

  
  /*
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
  

  
  */
  
  //to store key-value pairs: 
  SharedPreferences sharedPreference;
  


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {        


    super.onCreate(savedInstanceState);
    if(D) Log.d(TAG, "+++ ON CREATE +++");

    // Set up the window layout
    setContentView(R.layout.main);


    /*
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
    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION); */


    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    
    if (mBluetoothAdapter.getScanMode() !=
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
      // TODO: Note that enabling discovery also enables bluetooth, we shouldn't have any need to do both. 
      Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // A value of 0 turns discoverability on forever.
      // In the future if we would like to turn off discovery we can repeat this request with a value of 1
      startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }
    else{
      doPostDiscoverable();
    }
  }

  //NOTE: This happens when an activity becomes visible
  @Override
  public void onStart() {
    super.onStart();
    if(D) Log.e(TAG, "++ ON START ++");

    // TODO: Find a way in 4.0 to turn on wifi

    // If BT is not on, request that it be enabled.
    
    // Requesting discoverable will should enable the bluetooth
    /*
    if (!mBluetoothAdapter.isEnabled()) {          
      // Enable the bluetooth adapter
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    */
 
    

  }

  @Override
  public synchronized void onResume() {
    super.onResume();
    if(D) Log.e(TAG, "+ ON RESUME +");

    //WiFi P2P:
    //registerReceiver(mReceiver, mIntentFilter);

  }


  @Override
  public synchronized void onPause() {
    super.onPause();
    if(D) Log.e(TAG, "- ON PAUSE -");

    //WiFi P2P:
    // unregisterReceiver(mReceiver);
  }

  @Override
  public void onStop() {
    super.onStop();
    if(D) Log.e(TAG, "-- ON STOP --");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    
    if(D) Log.e(TAG, "--- ON DESTROY ---");
  }

 
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menubar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.account:
      //setContentView(R.layout.account);
      // loadProfileFromFile();

      //TextView t = new TextView(this);
      //t = (TextView)findViewById(R.id.current_name);
      //t.setText("Temproarily Unavailable");
      // TODO: Possibly bind to the service? Or loadProfile should be a utility function
      // t.setText(myProfile.getFirstName() + " " + myProfile.getLastName());

      return true;
    case R.id.privacy:
      //setContentView(R.layout.privacy);
      return true;
    case R.id.synchronization:
      //setContentView(R.layout.synchronization);

      //sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
      //Long lastSync = sharedPreference.getLong("lastSync", 0);
      //Timestamp ts = new Timestamp(lastSync);

      //TextView t2 = new TextView(this);
      //t2 = (TextView)findViewById(R.id.lastSyncText);
      //t2.setText(ts.toLocaleString()); 

      return true;
    case R.id.connection:
      //setContentView(R.layout.connection);
      return true;
    default: return super.onOptionsItemSelected(item);
    }
  }

  /* private void wifiConnect() { 
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
      mMessageService =  new WifiService(MainActivity.this, new mHandler(MainActivity.this));
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

  } */

    //Called when INTENT is returned
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if(D) Log.d(TAG, "onActivityResult " + resultCode);
      switch (requestCode) {
      case REQUEST_CONNECT_DEVICE:
        // When DeviceListActivity returns with a device to connect
        /*if (resultCode == Activity.RESULT_OK) {
          connectDevice(data, false);
        }*/
        break;
      case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns

        // TODO: This needs to be refactored into the bluetooth service
        //   We do not need to close the app if bluetooth isn't enabled
        if (resultCode != Activity.RESULT_OK) {
          // User did not enable Bluetooth or an error occurred
          Log.d(TAG, "BT not enabled");
          Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
          finish();
        }
        break;
      case REQUEST_DISCOVERABLE:
        //TODO: think about what if the user presses no?
        if( resultCode == RESULT_CANCELED ){
          // for now we do nothing
          Log.d(TAG, "User did not want to make their device discoverable");
          
          // Should popup a more meaningful view not just a toast.
          // TODO: Refactor into a R.string
          Toast.makeText(this, "Without being discoverable, no peers will be able to find you", Toast.LENGTH_LONG).show();
        }

        doPostDiscoverable();
        break;
      }
    }

    /*
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
    } */

    /**
     * Called after discovery is enabled. 
     */
    private void doPostDiscoverable() {
      Log.i(TAG, "About to start the service");
      Intent intent = new Intent(this, DiscoveryService.class);
      intent.setAction(DiscoveryService.ACTION_RESTART);
      startService(intent);
    }

    /**
     * Start the device list activity. If no service is started has no affect.
     */
    /* private void launchDeviceList(){
      if(mMessageService == null)
        return;


      Time now = new Time();
      now.setToNow();

      CommunicationService.com_transfers.put(now.format2445(), new WeakReference<CommunicationService>(mMessageService));
      // Launch the DeviceListActivity to see devices and do scan
      Intent serverIntent = new Intent(this, DeviceList.class);
      serverIntent.putExtra(CommunicationService.EXTRA_SERVICE_TRANFER, now.format2445());
      startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    } */

    public void syncnow(View view) {
      //TODO: Initate a request to the DiscoveryService to sync
      
      Intent intent = new Intent(this, DiscoveryService.class);
      intent.setAction(DiscoveryService.ACTION_SYNC);
      startService(intent);
      
      // TODO: The bellow code won't have any affect. Instead we should update the UI in a callback
      sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
      Long lastSync = sharedPreference.getLong("lastSync", 0);
      Timestamp ts = new Timestamp(lastSync);

      //TextView t2 = new TextView(this);
      //t2 = (TextView)findViewById(R.id.lastSyncText);
      //t2.setText(ts.toLocaleString());
    }

    public void backtomain(View view) {
      //setContentView(R.layout.main);
    }


    public void login(View view) {
      login();
    }

    public void login() {
      // TODO: Request a login from the discovery service
    }
    
    public void logout(View view) {
      // TODO: Request a logout from the discovery service
    }

    
}