package com.sprout.friendfinder.models;


import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;

@Table(name="contacts_lists_profiles")
public class ContactsListsProfiles extends Model {

  @Column(name="list", onDelete=ForeignKeyAction.CASCADE)
  // TODO: Does this mean, when this is deleted delete the foreign object, or when the foreign object is deleted delete this. We only want the latter. 
  ContactsListObject list;
  
  @Column (name="contact", onDelete=ForeignKeyAction.CASCADE)
  ProfileObject contact;
}
