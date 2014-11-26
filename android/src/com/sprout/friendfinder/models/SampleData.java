package com.sprout.friendfinder.models;

import java.util.Calendar;

import org.brickred.socialauth.Profile;

import android.content.Context;
import android.content.Intent;

import com.sprout.friendfinder.ui.IntersectionResultsActivity;

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
  
  // TODO: 
  // launch a view of all profiles
  
  public static void simulateInteractionResult(Context context) {
    simulateInteractionResult(context, simulateInteraction());
  }

  public static void simulateInteractionResult(Context context, Interaction interaction) {
    Intent notifyIntent = new Intent(context, IntersectionResultsActivity.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    // Useful flags if we want to resume a current activity
    // .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    notifyIntent.putExtra(IntersectionResultsActivity.EXTRA_DISPLAY, interaction.sharedContacts.getId());
    
    context.startActivity(notifyIntent);
  }
  
  /* I'd like to do this, as it calls actual code (not copy pasted) but it requires binding to the discovery service
  public static void simulateNotification(Interaction interaction) {
    discoveryService.addNotification(interaction.sharedContacts);
  }
  */
}