package com.sprout.friendfinder.ui.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.sprout.friendfinder.R;

public class SettingsFragment extends PreferenceFragment
{ 
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.layout.fragment_settings);
    
    //Account.initialize(this);
    //Communication.initialize(this);
    //Privacy.initialize(this);
    Developer.initialize(this);  
  }
}
