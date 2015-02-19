package com.sprout.friendfinder.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.ContactsNotificationManager;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.ItemAdapter.RowType;
import com.sprout.friendfinder.ui.settings.SettingsActivity;

/**
 * Display a list of all active interactions and history 
 * @author Oak
 *
 */
public class ValidContactsActivity extends ListActivity {
	
	private static final String TAG = ValidContactsActivity.class.getSimpleName();
	private OnSharedPreferenceChangeListener listener;
	  
	  private ItemAdapter adapter;

	  @Override
	  public void onCreate(Bundle savedInstanceState) {        
	    super.onCreate(savedInstanceState);
	    
	    getActionBar().setDisplayHomeAsUpEnabled(true);

	    resetAdapter();
	    
		SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	    listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				Log.i(TAG, "pref change for key: "+key);
				if(key.equals(DiscoveryService.LAST_SCAN_DEVICES_PREF)) {
					recreate();
				}
				
			}
		};
		mPrefs.registerOnSharedPreferenceChangeListener(listener);
		

	    // Bind to our new adapter.
	    setListAdapter(adapter); 
	  }

	  /**
	   * reset adapter if been used before. Otherwise, initialize it.
	   */
	  private void resetAdapter() {
		  // TODO: to get all (pending?) valid requests, might need to get it from mLastScan in DiscoveryService
		  List<Interaction> pastInteractions = getHistoryInteractions();
		  List<Interaction> activeInteractions = getActiveInteractions();
		  
		  List<Item> items = new ArrayList<Item>();
		  
		  // add active sections
		  items.add(new Header(getString(R.string.active_interactions)));
		  for(Interaction interaction : activeInteractions) {
		  	items.add(new InteractionItem(interaction));
		  }
		  
		  // add history sections
		  items.add(new Header(getString(R.string.past_interactions)));
		  for(Interaction interaction : pastInteractions) {
		  	items.add(new InteractionItem(interaction));
		  }
		  
		  if(adapter == null) {
			  adapter = new ItemAdapter(this, items);
		  } else {
			  adapter.clear();
			  adapter.addAll(items);
		  }
	  }
	  
	  /**
	   * 
	   * @return all history of interactions
	   */
	  private List<Interaction> getHistoryInteractions() {
		  return new Select().from(Interaction.class).where("failed=0 and infoExchanged=0").orderBy("timestamp DESC").execute();
	  }
	  
	  /**
	   * return all active interactions, from last scan. 
	   * Only show 1 interaction if multiple with the same addr exist 
	   * @return
	   */
	  private List<Interaction> getActiveInteractions() {

		  List<Interaction> activeInteractions = new ArrayList<Interaction>();
		  SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		  Set<String> activeAddr = new HashSet<String>();
		  Set<String> lastScanAddr = mPrefs.getStringSet(DiscoveryService.LAST_SCAN_DEVICES_PREF, new HashSet<String>());
		  Log.i(TAG, "all devices found in last scan: " + lastScanAddr.toString());
		  if(lastScanAddr.isEmpty()) {
			 Log.i(TAG, "last scan result is empty");
			 return activeInteractions;
		  }
		  
		  List<String> whereClauseList = new ArrayList<String>();
		  for(String addr : lastScanAddr) {
			  whereClauseList.add("address=\""+addr+"\"");
		  }
		  String whereClause = TextUtils.join(" or ", whereClauseList);
		  Log.i(TAG, "Getting active interactions - query: " + whereClause);

		  List<Interaction> allActiveInteractions = new Select().from(Interaction.class).where(whereClause).orderBy("timestamp DESC").execute();
		  Log.i(TAG, allActiveInteractions.size() + " interactions with address: " + allActiveInteractions);
		  
		  // only display 1 interaction per MAC address
		  for(Interaction interaction : allActiveInteractions) {
			  String curAddr = interaction.address;
			  if(!activeAddr.contains(curAddr)) {
				  activeAddr.add(curAddr);
				  activeInteractions.add(interaction);
			  }
		  }
		  return activeInteractions;
		 
		  
	  }
	  
	  @Override
	  public synchronized void onResume() {
	    super.onResume();

	    ContactsNotificationManager.getInstance().clear();
	    resetAdapter();
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
	    case android.R.id.home:
			returnToMainPage();
	    	return true;
	    default: return super.onOptionsItemSelected(item);
	    }
	  }

	  private void launchSettings() {
	    Intent intent = new Intent(this, SettingsActivity.class);
	    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    startActivity(intent);
	  }
	  
	  @Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
	    Item item = ((Item) getListView().getItemAtPosition(position));
	    int viewType = item.getViewType();
	    
	    if(viewType == RowType.HEADER_ITEM.ordinal()) return;
	    else if(viewType == RowType.INTERACTION_ITEM.ordinal()) {
		    Interaction interaction = ((InteractionItem) item).getInteraction();
		    
		    // Might need to reload the interaction here
	
		    Intent notifyIntent = new Intent(this, IntersectionResultsActivity.class);
		    //notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		    notifyIntent.putExtra(IntersectionResultsActivity.EXTRA_DISPLAY, interaction.sharedContacts.getId());
	
		    startActivity(notifyIntent);
	    } else return;
	  }
	  
	  @Override
	  public void onBackPressed() {
		  returnToMainPage();
	  }
	  
	  public void returnToMainPage() {
		  this.finish();
		  Intent intent = new Intent(this, MainActivity.class);
		  startActivity(intent);
	  }
}
