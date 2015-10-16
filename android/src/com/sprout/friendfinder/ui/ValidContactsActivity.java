package com.sprout.friendfinder.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.ContactsNotificationManager;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.ui.baseui.InteractionBaseActivity;
import com.sprout.friendfinder.ui.baseui.Item;

/**
 * Display a list of cache interactions 
 * rename to ActiveInteractionActivity?
 * @author Oak
 *
 */
public class ValidContactsActivity extends InteractionBaseActivity {
  
  private static final String TAG = ValidContactsActivity.class.getSimpleName();

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
	  List<Interaction> cacheInteractions = getCacheInteractions();
	  
	  List<Item> items = new ArrayList<Item>();
	  
	  // add active interactions
	  for(Interaction interaction : cacheInteractions) {
	  	items.add(new InteractionItem(interaction));
	  }
	  
	  if(adapter == null) {
		  adapter = new ItemAdapter(this, items);
	  } else {
		  adapter.clear();
		  adapter.addAll(items);
	  }
  }
  
  protected List<Interaction> getCacheInteractions() {
    ArrayList<String> cacheInteractionId = getIntent().getStringArrayListExtra(ContactsNotificationManager.CACHE_INTERACT_ID_EXTRA);
    List<Interaction> cacheInteractions = new ArrayList<Interaction>();
    if(cacheInteractionId != null) {
      Log.i(TAG, "getting cache interactions for id: " + Arrays.toString(cacheInteractionId.toArray()));

      cacheInteractions = new Select().distinct().from(Interaction.class).where("id IN (" + TextUtils.join(",", cacheInteractionId) + ")").execute();
      Log.i(TAG, "num of interactions retrieved: " + cacheInteractions.size());
    } else {
      Log.i(TAG, "cache interaction is null");
    }
    return cacheInteractions;
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
