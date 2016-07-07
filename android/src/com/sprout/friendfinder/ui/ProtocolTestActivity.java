package com.sprout.friendfinder.ui;

import com.activeandroid.util.Log;
import com.sprout.finderlib.communication.BluetoothService;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.ProtocolTestService;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager.ProtocolType;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ProtocolTestActivity extends Activity {
  
  static final String TAG = ProtocolTestActivity.class.getSimpleName();
  
  RadioGroup protocolSelection;
  Button btnDisplay;
  CommunicationService mMessageService = null;
  Device mRunningDevice = null;
  ProtocolTestService mService;
  AlertDialog progressDialog;
  ProtocolType protocolTest;
  private BluetoothAdapter mBluetoothAdapter = null;

  private boolean mIsBound = false;
  
  private int numFriends = 0;
  private int numTrials = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    Log.i(TAG, "creating prtotocoltestactivity");

    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);
    setContentView(R.layout.protocol_selection);
    
    addListenerOnButton();
    
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      onBackPressed();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  /**
   * TODO: most vars inside dont have to be global..
   */
  public void addListenerOnButton() {

    protocolSelection = (RadioGroup) findViewById(R.id.protocol_selections);
    final RadioGroup numFriendsSelection = (RadioGroup) findViewById(R.id.num_friends);
    final RadioGroup numTrialsSelection = (RadioGroup) findViewById(R.id.num_trials);
    btnDisplay = (Button) findViewById(R.id.start_test);

    btnDisplay.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {

            // get selected radio button from radioGroup
            int selectedId = protocolSelection.getCheckedRadioButtonId();
            int selectedIdFriend =  numFriendsSelection.getCheckedRadioButtonId();
            int selectedIdTrial = numTrialsSelection.getCheckedRadioButtonId();

            // find the radiobutton by returned id
            RadioButton protocol = (RadioButton) findViewById(selectedId);
            numFriends = Integer.valueOf((String) ((RadioButton) findViewById(selectedIdFriend)).getText());
            numTrials = Integer.valueOf((String) ((RadioButton) findViewById(selectedIdTrial)).getText());
            
            

            Toast.makeText(ProtocolTestActivity.this, protocol.getText(), Toast.LENGTH_SHORT).show();
            Toast.makeText(ProtocolTestActivity.this, "using "+numFriends+" friends", Toast.LENGTH_SHORT).show();
            
            protocolTest = getProtocol((String) protocol.getText());

            startTest(protocolTest);
        }

    });

  }
  
  public ProtocolType getProtocol(String protocolName) {
    if(protocolName.equals(getString(R.string.rb_atwpsi))) {
      return ProtocolType.ATWPSI;
    } else if(protocolName.equals(getString(R.string.rb_atwpsica_new))) {
      return ProtocolType.ATWPSICA_NEW;
    } else if(protocolName.equals(getString(R.string.rb_b_bf_psica))) {
      return ProtocolType.BBFPSICA;
    } else if(protocolName.equals(getString(R.string.rb_psica))) {
      return ProtocolType.PSICA;
    } else if(protocolName.equals(getString(R.string.rb_atwpsica))) {
      return ProtocolType.ATWPSICA;
    } else if(protocolName.equals(getString(R.string.rb_b_bf_psi))) {
      return ProtocolType.BBFPSI;
    } else if(protocolName.equals(getString(R.string.rb_b_bf_psi_no_v))) {
      return ProtocolType.BBFPSI_No_V;
    } else if(protocolName.equals(getString(R.string.rb_b_psica))) {
      return ProtocolType.BPSICA;
    } else if(protocolName.equals(getString(R.string.rb_b_psi))) {
      return ProtocolType.BPSI;
    }
    return ProtocolType.NONE;
  }
  
  public void startTest(ProtocolType protocol) {

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      return;
    }

    doPostDiscoverable(protocol);
//    if (mBluetoothAdapter.getScanMode() !=
//        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//      Toast.makeText(this, "Cant start the test because the bluetooth is off", Toast.LENGTH_SHORT).show();
//    }
//    else{
//      doPostDiscoverable(protocol);
//    }
  }
  
  /**
   * show progress when trying to run a protocol with a peer
   * TODO: no hard-coded string
   */
  public void showProgressDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle("Protocol "+protocolTest.toString()+" with "+numTrials+" trials, "+numFriends+" friends.");
    builder.setMessage("Initiating the test");
    
    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        if(mService != null) {
          mService.stopSelf();
          doUnbindService();
        }
      }
      
    });
    
    // Create popup and show
    progressDialog = builder.create();
    
    progressDialog.show();
  }
  
  private class PTSServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder)
    {
        mService = ((ProtocolTestService.LocalBinder)iBinder).getService();
        mService.setHandler(new PTSHandler());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        mService = null;
    }
  };
  
  ServiceConnection mConnection;

  private void doBindService()
  {
      // Establish a connection with the service.  We use an explicit
      // class name because we want a specific service implementation that
      // we know will be running in our own process (and thus won't be
      // supporting component replacement by other applications).
      mConnection = new PTSServiceConnection();
      bindService(new Intent(this, ProtocolTestService.class), mConnection, Context.BIND_AUTO_CREATE);
      mIsBound = true;
  }
  
  private void doUnbindService()
  {
      if (mIsBound)
      {
          // Detach our existing connection.
          unbindService(mConnection);
          mIsBound = false;
      }
  }
  
  @Override
  protected void onDestroy()
  {
      super.onDestroy();
  }


  /**
   * Called after discovery is enabled. 
   */
  private void doPostDiscoverable(ProtocolType protocol) {
    // TODO: use different service - just for testing. DiscoveryService is too slow.
    Toast.makeText(this, "Testing protocol "+protocol.toString(), Toast.LENGTH_SHORT).show();
    
    showProgressDialog();
    Intent intent = new Intent(this, ProtocolTestService.class);
    intent.setAction(ProtocolTestService.ACTION_RESTART);
    intent.putExtra("numFriends", numFriends);
    intent.putExtra("numTrials", numTrials);
    intent.putExtra("protocol", protocol.toString());
    startService(intent);

    doBindService();
  }
  
  /***************************/
  /* *** Message Handlers ** */ 
  /***************************/

  // The primary handler to receive messages form the com service
  // TODO: right way to do?
  private class PTSHandler extends Handler {
    
    @Override
    public void handleMessage(Message msg) {
      if(mService == null) {
        Log.w(TAG, "service not bound");
        return;
      }

      switch (msg.what) {
      
      case ProtocolTestService.MESSAGE_STATE_CHANGE:
        switch (msg.arg1) {
        case ProtocolTestService.STATE_RUNNING:
          break;
          
        case ProtocolTestService.STATE_READY:
          progressDialog.setMessage("Service is ready");
          break;
          
        case ProtocolTestService.STATE_COMPLETED:
          
          progressDialog.setMessage(mService.result.toString());
          doUnbindService();
          break;
        case ProtocolTestService.STATE_SYNC:
          progressDialog.setMessage("Downloading authorization object");
          
        case ProtocolTestService.STATE_DISABLED:
          progressDialog.setMessage("Communication Service is disabled");
        }
        break;
        
      case   BluetoothService.MESSAGE_STATE_CHANGE:
        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
        switch (msg.arg1) {
        case CommunicationService.STATE_CONNECTED:
          // TODO: Eventually, we need a way to toggle what test gets run.
          // For now we just checkCommonFriends

          // Is it possible that this will be fired while we are already connecting?
          // YES, not sure what the right thing to do is in this case.
          
          mService.connected();
          progressDialog.setMessage("Connected to Device: "+mService.mRunningDevice.getName());
      
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

        Toast.makeText(mService.getApplicationContext(), "Message read " 
            + readMessage, Toast.LENGTH_SHORT).show();
        break;
      case CommunicationService.MESSAGE_DEVICE:
        mService.mRunningDevice = (Device) msg.getData().getSerializable(CommunicationService.DEVICE);
        break;            
      case CommunicationService.MESSAGE_TOAST:
        Toast.makeText(mService.getApplicationContext(), msg.getData().getString(CommunicationService.TOAST),
            Toast.LENGTH_SHORT).show();
        break;
      case CommunicationService.MESSAGE_FAILED:
        // When a message fails we assume we need to reset
        Log.e(TAG, "Message failed");
        
        // TODO: correct way to do this?
        // run() already removed the device, so skipping to the next device
        mService.run();

        // TODO: Perform a reset (remove the peer, etc)
        break;
      case CommunicationService.MESSAGE_DISABLED:
        mService.onDisabled();
        break;
      case CommunicationService.MESSAGE_ENABLED:
        // The state should be disabled. But this may not be the case
        if (mService.getState() != ProtocolTestService.STATE_STOP) {
          mService.initialize();
        }
        break;
      }
    }};
}