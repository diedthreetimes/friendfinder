package com.sprout.friendfinder.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.ItemAdapter.RowType;
import com.sprout.friendfinder.ui.settings.SettingsActivity;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;

/**
 * abstract base class for displaying interaction items
 * given a set of interaction items, it can display them
 * @author Oak
 *
 */
public abstract class InteractionBaseActivity extends ListActivity {

	private static final String TAG = InteractionBaseActivity.class.getSimpleName();
	protected OnSharedPreferenceChangeListener listener;
	protected ItemAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {   
    super.onCreate(savedInstanceState);
    
    // set listener for changing adapter values
		SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				Log.i(TAG, "pref change for key: "+key);
				if(key.equals(DiscoveryService.LAST_SCAN_DEVICES_PREF)) {
					reloadAdapter();
					adapter.notifyDataSetChanged();
				}
				
			}
		};
		mPrefs.registerOnSharedPreferenceChangeListener(listener);

    // Bind to our new adapter.
    reloadAdapter();
    setListAdapter(adapter); 
	}
	
	/**
	 * where the activity loads adapter
	 */
	protected abstract void reloadAdapter();
	
	/**
   * 
   * @return all history of interactions
   */
  protected List<Interaction> getHistoryInteractions() {
	  // TODO: need .where("failed=0 and infoExchanged=0") or other boolean checking?
    List<Interaction> pastInteractions = new Select().from(Interaction.class).orderBy("timestamp DESC").execute();
    pastInteractions.removeAll(getActiveInteractions());
	  return pastInteractions;
  }
  
  /**
   * return all active interactions, from last scan. 
   * Only show 1 interaction if multiple with the same addr exist 
   * @return
   */
  protected List<Interaction> getActiveInteractions() {
    SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    Set<String> lastScanAddr = mPrefs.getStringSet(DiscoveryService.LAST_SCAN_DEVICES_PREF, new HashSet<String>());
	  List<Interaction> activeInteractions = Interaction.getInteractionFromAddress(lastScanAddr, true) ;
	  return activeInteractions;
  }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menubar, menu);
    return true;
	}
	  
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
    Item item = ((Item) getListView().getItemAtPosition(position));
    int viewType = item.getViewType();
    
    if(viewType == RowType.HEADER_ITEM.ordinal()) return;
    else if(viewType == RowType.INTERACTION_ITEM.ordinal()) {
	    Interaction interaction = ((InteractionItem) item).getInteraction();

	    Intent notifyIntent = new Intent(this, IntersectionResultsActivity.class);
	    //notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	    notifyIntent.putExtra(IntersectionResultsActivity.EXTRA_DISPLAY, interaction.sharedContacts.getId());

	    startActivity(notifyIntent);
    } else return;
	}

  protected void launchSettings() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    startActivity(intent);
  }
}
