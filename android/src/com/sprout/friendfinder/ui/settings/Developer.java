package com.sprout.friendfinder.ui.settings;

import com.activeandroid.util.SQLiteUtils;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.backend.ContactsNotificationManager;
import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.models.SampleData;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Class that sets up the "Developer Options" portion of the settings screen
 */
public class Developer {

  public static void initialize(final PreferenceFragment fragment) {
    setupLaunchResultActivity(fragment);
    setupLaunchContactsActivity(fragment);
    setupClearCaches(fragment);
    setupClearInteractions(fragment);
    setupLaunchResultActivity(fragment);
    setupAddNotification(fragment);
    
    // TODO: put syncnow somewhere
  }
  
  private static void setupAddNotification(final PreferenceFragment fragment) {
	    Preference launch = (Preference) fragment.findPreference(fragment.getResources()
	            .getString(R.string.pref_add_notification_key));
	    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	        	ContactsNotificationManager.getInstance().showNotification(preference.getContext(), SampleData.simulateContacts());
	            return true;
	        }
	      });
  }
  
  private static void setupClearInteractions(final PreferenceFragment fragment) {
    Preference launch = (Preference) fragment.findPreference(fragment.getResources()
        .getString(R.string.pref_clear_interactions_key));
    
    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        SQLiteUtils.execSql("delete from interactions;");
        return true;
      }
    });
  }
  
  private static void setupClearCaches(final PreferenceFragment fragment) {
    Preference launch = (Preference) fragment.findPreference(fragment.getResources()
        .getString(R.string.pref_clear_cache_key));
    
    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(fragment.getActivity(), DiscoveryService.class);
        intent.setAction(DiscoveryService.ACTION_RESET_CACHE);
        fragment.getActivity().startService(intent);
        return true;
      }
    });
    
    launch = (Preference) fragment.findPreference(fragment.getResources()
        .getString(R.string.pref_clear_cache_peers_key));
    
    launch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(fragment.getActivity(), DiscoveryService.class);
        intent.setAction(DiscoveryService.ACTION_RESET_CACHE_PEERS);
        fragment.getActivity().startService(intent);
        return true;
      }
    });
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
