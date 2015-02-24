package com.sprout.friendfinder.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.ContactsNotificationManager;
import com.sprout.friendfinder.models.Interaction;

/**
 * Display a list of all active interactions 
 * rename to ActiveInteractionActivity?
 * @author Oak
 *
 */
public class ValidContactsActivity extends InteractionBaseActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {        
    super.onCreate(savedInstanceState);
    getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  /**
   * reset adapter if been used before. Otherwise, initialize it.
   */
  @Override
  protected void reloadAdapter() {
	  List<Interaction> activeInteractions = getActiveInteractions();
	  
	  List<Item> items = new ArrayList<Item>();
	  
	  // add active interactions
	  for(Interaction interaction : activeInteractions) {
	  	items.add(new InteractionItem(interaction));
	  }
	  
	  if(adapter == null) {
		  adapter = new ItemAdapter(this, items);
	  } else {
		  adapter.clear();
		  adapter.addAll(items);
	  }
  }
  
  @Override
  public synchronized void onResume() {
    super.onResume();
    ContactsNotificationManager.getInstance().clear();
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
