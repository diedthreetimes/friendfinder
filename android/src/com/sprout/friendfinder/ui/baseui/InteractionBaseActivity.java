package com.sprout.friendfinder.ui.baseui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.InteractionItem;
import com.sprout.friendfinder.ui.IntersectionResultsActivity;
import com.sprout.friendfinder.ui.ItemAdapter;
import com.sprout.friendfinder.ui.ItemAdapter.RowType;
import com.sprout.friendfinder.ui.settings.SettingsActivity;

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
public abstract class InteractionBaseActivity extends BaseListActivity {

	private static final String TAG = InteractionBaseActivity.class.getSimpleName();
	protected OnSharedPreferenceChangeListener listener;
	protected ItemAdapter adapter;
	
//	private BroadcastReceiver mReceiver;

  DiscoveryService mService;
  boolean mBound = false;
	
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
  
	@Override
	public void onStart() {
	  super.onStart();

//    Intent intent = new Intent(this, DiscoveryService.class);
//    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//    
//    mReceiver = new ReloadAdapterReceiver();
//    registerReceiver(mReceiver, new IntentFilter(DiscoveryService.LAST_SCAN_DEVICES_PREF));
//    Log.i(TAG, "succesfully register");
	}
	
//  @Override
//  protected void onStop() {
//    super.onStop();
//    // Unbind from the service
//    if (mBound) {
//        unbindService(mConnection);
//        mBound = false;
//    }
//    unregisterReceiver(mReceiver);
//  }
//	
//	public void onResume()
//  {
////    IntentFilter filter = new IntentFilter();
////    filter.addAction(INTENT_ACTON_RESET_ADAPTER);
////    registerReceiver(receiver, filter);  
//    super.onResume();
//  }
//
//  public void onPause()
//  {
//    super.onPause();
//  }
	
	/**
	 * where the activity loads adapter
	 */
	protected abstract void reloadAdapter();

  /**
   * 
   * @return all history of interactions
   */
  protected List<Interaction> getHistoryInteractions(List<Interaction> activeInteraction) {
    // TODO: need .where("failed=0 and infoExchanged=0") or other boolean checking?
    List<Interaction> pastInteractions = new Select().from(Interaction.class).orderBy("timestamp DESC").execute();
    pastInteractions.removeAll(activeInteraction);
    return pastInteractions;
  }
	/**
   * 
   * @return all history of interactions
   */
  protected List<Interaction> getHistoryInteractions() {
	  // TODO: need .where("failed=0 and infoExchanged=0") or other boolean checking?
	  return getHistoryInteractions(getActiveInteractions());
  }
  
  /**
   * return all active interactions, from last scan. 
   * Only show 1 interaction if multiple with the same addr exist 
   * @return
   */
  protected List<Interaction> getActiveInteractions() {
//    if(mBound) {
//      List<Interaction> activeInteractions = Interaction.getInteractionFromAddress(mService.getLastScanAddrs(), true);
//      return activeInteractions;
//    }
    SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    Set<String> lastScanAddr = mPrefs.getStringSet(DiscoveryService.LAST_SCAN_DEVICES_PREF, new HashSet<String>());
	  List<Interaction> activeInteractions = Interaction.getInteractionFromAddress(lastScanAddr, true) ;
	  return activeInteractions;
//    return new ArrayList<Interaction>();
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
  
//  /** Defines callbacks for service binding, passed to bindService() */
//  private ServiceConnection mConnection = new ServiceConnection() {
//
//      @Override
//      public void onServiceConnected(ComponentName className,
//              IBinder service) {
//          // We've bound to LocalService, cast the IBinder and get LocalService instance
//          LocalBinder binder = (LocalBinder) service;
//          mService = binder.getService();
//          mBound = true;
//      }
//
//      @Override
//      public void onServiceDisconnected(ComponentName arg0) {
//          mBound = false;
//      }
//  };
}
