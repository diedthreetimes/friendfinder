package com.sprout.friendfinder.ui.baseui;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;

import com.sprout.friendfinder.backend.FriendFinderApplication;

public class BaseListActivity extends ListActivity {
  protected FriendFinderApplication mMyApp;

  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      mMyApp = (FriendFinderApplication)this.getApplicationContext();
  }
  protected void onResume() {
      super.onResume();
      mMyApp.setCurrentActivity(this);
  }
  protected void onPause() {
      clearReferences();
      super.onPause();
  }
  protected void onDestroy() {        
      clearReferences();
      super.onDestroy();
  }

  private void clearReferences(){
      Activity currActivity = mMyApp.getCurrentActivity();
      if (currActivity != null && currActivity.equals(this))
          mMyApp.setCurrentActivity(null);
  }
}
