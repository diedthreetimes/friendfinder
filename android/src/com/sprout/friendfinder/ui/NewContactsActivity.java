package com.sprout.friendfinder.ui;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.settings.SettingsActivity;

/**
 * Display a list of pending requests
 * @author Oak
 *
 */
public class NewContactsActivity extends ListActivity {
	private static final String TAG = NewContactsActivity.class.getSimpleName();
	  
	  private InteractionAdapter adapter;

	  @Override
	  public void onCreate(Bundle savedInstanceState) {        
	    super.onCreate(savedInstanceState);

	    // TODO: to get pending request, need to add a boolean in ProfileObject, right now a simple hack by using connectionRequested
	    List<Interaction> interactions =  new Select().from(Interaction.class).where("failed=0 and connectionRequested=1").orderBy("timestamp DESC").execute();

	    if(interactions.size() == 0) {
	      // TODO: Show something interesting in this case
	      Log.i(TAG, "No interactions found");
	    } 

	    adapter = new InteractionAdapter(this, interactions);

	    // Bind to our new adapter.
	    setListAdapter(adapter); 
	   
	  }
	  @Override
	  public void onStart() {
	    super.onStart();
	  }

	  @Override
	  public synchronized void onResume() {
	    super.onResume();

	    adapter.clear();
	    List<Interaction> interactions =  new Select().from(Interaction.class).where("failed=0 and connectionRequested=1").orderBy("timestamp DESC").execute();
	    adapter.addAll(interactions);
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
