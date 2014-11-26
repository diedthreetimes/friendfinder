package com.sprout.friendfinder.ui;

import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.settings.SettingsActivity;


// TODO: Rename this to HistoryActivity
public class MainActivity extends ListActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final boolean D = true;

  // Intent request codes
  private static final int REQUEST_ENABLE_BT = 3; 
  private static final int REQUEST_DISCOVERABLE = 4;

  private BluetoothAdapter mBluetoothAdapter = null;
  
  private InteractionAdapter adapter;

  //to store key-value pairs: 
  SharedPreferences sharedPreference;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {        
    super.onCreate(savedInstanceState);

    List<Interaction> interactions =  new Select().from(Interaction.class).orderBy("timestamp DESC").execute();
    if(interactions.size() == 0) {
      // TODO: Show something interesting in this case
      Log.i("TAG", "No interactions found");
    } 

    adapter = new InteractionAdapter(this, interactions);

    // Bind to our new adapter.
    setListAdapter(adapter);
    
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      // TODO: Display a warning here?
      //finish();
      doPostDiscoverable();
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
  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    super.onResume();
    
    // TODO: update this as things change
    adapter.clear();
    List<Interaction>interactions = new Select().from(Interaction.class).orderBy("timestamp DESC").execute();
    adapter.addAll(interactions);
  }


  @Override
  public synchronized void onPause() {
    super.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
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
    case R.id.settings:
      launchSettings();
      return true;
    default: return super.onOptionsItemSelected(item);
    }
  }

  //Called when INTENT is returned
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(D) Log.d(TAG, "onActivityResult " + resultCode);
    switch (requestCode) {
    case REQUEST_ENABLE_BT:
      // When the request to enable Bluetooth returns

      // TODO: This needs to be refactored into the bluetooth service
      //   We shouldn't need to close the app if bluetooth isn't enabled
      if (resultCode != Activity.RESULT_OK) {
        // User did not enable Bluetooth or an error occurred
        Log.d(TAG, "BT not enabled");
        Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
        finish();
      }
      break;
    case REQUEST_DISCOVERABLE:
      // If user presses no we warn and move on
      if( resultCode == RESULT_CANCELED ){  
        Log.d(TAG, "User did not want to make their device discoverable");

        // Should popup a more meaningful view not just a toast.
        // TODO: Refactor into a R.string
        Toast.makeText(this, "Without being discoverable, no peers will be able to find you", Toast.LENGTH_LONG).show();
      }

      doPostDiscoverable();
      break;
    }
  }

  /**
   * Called after discovery is enabled. 
   */
  private void doPostDiscoverable() {
    Intent intent = new Intent(this, DiscoveryService.class);
    intent.setAction(DiscoveryService.ACTION_RESTART);
    startService(intent);
  }

  private void launchSettings() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    startActivity(intent);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Interaction interaction = ((Interaction) getListView().getItemAtPosition(position));
    // Might need to reload the interaction here

    Intent notifyIntent = new Intent(this, IntersectionResultsActivity.class);
    //notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    notifyIntent.putExtra(IntersectionResultsActivity.EXTRA_DISPLAY, interaction.sharedContacts.getId());

    startActivity(notifyIntent);
  }
}