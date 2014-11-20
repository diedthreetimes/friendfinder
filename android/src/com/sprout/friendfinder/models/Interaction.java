package com.sprout.friendfinder.models;

import java.util.Calendar;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/*
 * This stores the results of a single interaction witha  peer
 * Speicifically it tracks
 * 1. Who the interaction was with - mac address, public key, and a link to the contact record if it was revealed (may also want to store chosen anonomous name)?
 * 2. Results of the interaction - friends / profile information in common etc.
 * 3. When the interaction was
 */
@Table(name = "interactions")
public class Interaction extends Model {
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
}
