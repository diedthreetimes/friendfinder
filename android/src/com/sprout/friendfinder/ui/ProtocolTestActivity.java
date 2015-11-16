package com.sprout.friendfinder.ui;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.common.Constants;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager;
import com.sprout.friendfinder.crypto.protocols.ProtocolManager.ProtocolType;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
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
  RadioButton protocol;
  Button btnDisplay;
  CommunicationService mMessageService = null;
  private BluetoothAdapter mBluetoothAdapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
  
  public void addListenerOnButton() {

    protocolSelection = (RadioGroup) findViewById(R.id.protocol_selections);
    btnDisplay = (Button) findViewById(R.id.start_test);

    btnDisplay.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {

            // get selected radio button from radioGroup
            int selectedId = protocolSelection.getCheckedRadioButtonId();

            // find the radiobutton by returned id
            protocol = (RadioButton) findViewById(selectedId);

            Toast.makeText(ProtocolTestActivity.this, protocol.getText(), Toast.LENGTH_SHORT).show();

            startTest();
        }

    });

  }
  
  public void startTest() {

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      return;
    }

    if (mBluetoothAdapter.getScanMode() !=
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
      Toast.makeText(this, "Cant start the test because the bluetooth is off", Toast.LENGTH_SHORT).show();
    }
    else{
      doPostDiscoverable();
    }
  }

  /**
   * Called after discovery is enabled. 
   */
  private void doPostDiscoverable() {
    // TODO: use different service - just for testing. DiscoveryService is too slow.
    Toast.makeText(this, "Testing protocol "+ProtocolManager.overrideProtocol, Toast.LENGTH_SHORT).show();
    
//    Intent intent = new Intent(this, DiscoveryService.class);
//    intent.setAction(DiscoveryService.ACTION_RESTART);
//    startService(intent);
  }
}