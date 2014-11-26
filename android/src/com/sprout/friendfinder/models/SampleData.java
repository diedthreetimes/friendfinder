package com.sprout.friendfinder.models;

import java.util.Calendar;

import org.brickred.socialauth.Profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sprout.friendfinder.backend.DiscoveryService;
import com.sprout.friendfinder.ui.IntersectionResultsActivity;

// TODO: Rename developer tools
public class SampleData {

  public static ContactsListObject simulateContacts() {
    ContactsListObject clo = new ContactsListObject();
    clo.save();
    
    for (int i=0; i < 10; i++) {
      Profile p = new Profile();
      p.setEmail("me"+i+"@example.com");
      p.setFirstName("First " + i);
      p.setLastName("Last " + i);
      p.setProfileImageURL("https://media.licdn.com/mpr/mpr/shrink_200_200/p/4/000/181/379/22152ef.jpg");
      p.setValidatedId("12340"+i);
    
      if (i==0) {
        p.setProfileImageURL(null);
      }
      if (i==1) {
        p.setProfileImageURL("");
      }
      ProfileObject prof = new ProfileObject(p);
      prof.save();
      clo.put( prof );
    }
    
    
    return clo;
  }
  
  public static Interaction simulateInteraction() {
    Interaction interaction = new Interaction();
    interaction.address = "ABCDEFG";
    interaction.failed = false;
    interaction.timestamp = Calendar.getInstance();
    interaction.sharedContacts = simulateContacts();
    // TODO:  interaction.profile = 
    interaction.save();
    
    return interaction;
  }
  
  public static void showContacts(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    
    ProfileObject profile = ProfileObject.load(ProfileObject.class, prefs.getLong(DiscoveryService.PROFILE_ID_PREF, -1));

    if (profile == null) {
      Toast.makeText(context, "No profile available", Toast.LENGTH_SHORT).show();
      return;
    }
    Interaction interaction = new Interaction();
    interaction.address = "ABCDEFG";
    interaction.failed = false;
    interaction.timestamp = Calendar.getInstance();
    interaction.sharedContacts = profile.contacts();
    interaction.save();
    
    simulateInteractionResult(context, interaction);
  }
  
  public static void simulateInteractionResult(Context context) {
    simulateInteractionResult(context, simulateInteraction());
  }

  public static void simulateInteractionResult(Context context, Interaction interaction) {
    Intent notifyIntent = new Intent(context, IntersectionResultsActivity.class);
    //notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    notifyIntent.putExtra(IntersectionResultsActivity.EXTRA_DISPLAY, interaction.sharedContacts.getId());
    
    context.startActivity(notifyIntent);
  }
  
  /* I'd like to do this, as it calls actual code (not copy pasted) but it requires binding to the discovery service
  public static void simulateNotification(Interaction interaction) {
    discoveryService.addNotification(interaction.sharedContacts);
  }
  */
}