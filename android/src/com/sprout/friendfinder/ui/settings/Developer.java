package com.sprout.friendfinder.ui.settings;

import com.sprout.friendfinder.R;

import com.sprout.friendfinder.models.SampleData;

import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Class that sets up the "Developer Options" portion of the settings screen
 */
public class Developer {

  public static void initialize(final PreferenceFragment fragment) {
    setupLaunchResultActivity(fragment);
    setupLaunchContactsActivity(fragment);
    // TODO:
    //setupClearCache(fragment);
    //setupClearInteractions(fragment);
  }
  
  private static void setupLaunchContactsActivity(final PreferenceFragment fragment) {
    Preference launch = (Preference) fragment.findPreference(fragment.getResources()
        .getString(R.string.pref_launch_contacts_key));
    
    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        SampleData.showContacts(fragment.getActivity());
        return true;
      }
      
    });
  }
  
  private static void setupLaunchResultActivity(final PreferenceFragment fragment) {
    Preference launch = (Preference) fragment.findPreference(fragment.getResources()
        .getString(R.string.pref_launch_result_key));
    
    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        SampleData.simulateInteractionResult(fragment.getActivity());
        return true;
      }
      
    });
  }
}
