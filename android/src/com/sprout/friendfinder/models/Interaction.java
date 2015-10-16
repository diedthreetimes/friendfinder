package com.sprout.friendfinder.models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/*
 * This stores the results of a single interaction witha  peer
 * Speicifically it tracks
 * 1. Who the interaction was with - mac address, public key, and a link to the contact record if it was revealed (may also want to store chosen anonomous name)?
 * 2. Results of the interaction - friends / profile information in common etc.
 * 3. When the interaction was
 */
@Table(name = "interactions")
public class Interaction extends Model {
	
  private static final String TAG = Interaction.class.getSimpleName();
	
  @Column
  public String address;
  
  @Column
  public String publicKey;
  
  @Column(name = "profile")
  public ProfileObject profile;
   
  @Column
  public boolean infoExchanged;
  
  @Column
  public boolean connectionRequested;
  
  @Column
  public Calendar timestamp;
  
  @Column
  public ContactsListObject sharedContacts;
  
  @Column
  public boolean failed;
  
  public static List<Interaction> getInteractionFromAddress(Set<String> lastScanAddr, boolean uniqueLatest) {
	List<Interaction> interactions = new ArrayList<Interaction>();
	  Set<String> activeAddr = new HashSet<String>();
	  Log.i(TAG, "all devices found in last scan: " + lastScanAddr.toString());
	  if(lastScanAddr.isEmpty()) {
		 return interactions;
	  }

	  // TODO: need .where("failed=0 and infoExchanged=0") or other boolean checking?
	  List<String> whereClauseList = new ArrayList<String>();
	  for(String addr : lastScanAddr) {
		  whereClauseList.add("address=\""+addr+"\"");
	  }
	  String whereClause = TextUtils.join(" or ", whereClauseList);
	  Log.i(TAG, "Getting active interactions - query: " + whereClause);

	  List<Interaction> allActiveInteractions = new Select().from(Interaction.class).where(whereClause).orderBy("timestamp DESC").execute();
	  Log.i(TAG, allActiveInteractions.size() + " interactions with address: " + allActiveInteractions);
	  
	  if(uniqueLatest) {
		  // only display a latest interaction per MAC address
		  for(Interaction interaction : allActiveInteractions) {
			  String curAddr = interaction.address;
			  if(!activeAddr.contains(curAddr)) {
				  activeAddr.add(curAddr);
				  interactions.add(interaction);
			  }
		  }
		  return interactions;
	  } else return allActiveInteractions;
	}
  
  @Override
  public boolean equals(Object obj) {
      if (this == obj)
          return true;
      if (obj == null)
          return false;
      if (getClass() != obj.getClass())
          return false;
      Interaction int2 = (Interaction) obj;
      
      // should be enough
      return (this.address.equals(int2.address) && this.timestamp.equals(int2.timestamp));
  }
}
