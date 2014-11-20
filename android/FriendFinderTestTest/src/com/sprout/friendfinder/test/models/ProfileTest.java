package com.sprout.friendfinder.test.models;

import java.util.List;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.sprout.friendfinder.models.ContactsListObject;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.test.helpers.DatabaseHelper;

public class ProfileTest extends AndroidTestCase {
  protected void setUp() throws Exception {
    super.setUp();
    RenamingDelegatingContext context = new RenamingDelegatingContext(
        getContext(), "test_");
    ActiveAndroid.initialize(context);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    ActiveAndroid.dispose();
  }
  
  public void testBasic() {
    ProfileObject profile = new ProfileObject(DatabaseHelper.mockSelf());
    
    long id = profile.save();
    
    ProfileObject loaded = ProfileObject.load(ProfileObject.class, id);
    
    assertEquals(profile.getFirstName(), loaded.getFirstName());
    assertEquals(profile.getLastName(), loaded.getLastName());
    assertEquals(profile.getDisplayName(), loaded.getDisplayName());
    assertEquals(profile.getProfileImageURL(), loaded.getProfileImageURL());
  }
  
  private static int NUM_CONTACTS = 10;
  public void testContacts() {
    ProfileObject profile = new ProfileObject(DatabaseHelper.mockSelf());
    
    ContactsListObject contacts = new ContactsListObject(DatabaseHelper.randContacts(NUM_CONTACTS));
    long cid = contacts.save();
    
    // NOTE: contacts have to be saved already for this to work
    profile.setContacts(contacts);
    long pid = profile.save();
    
    List<ProfileObject> list = contacts.getContactList();
    Log.d("ProfileTest", "Non Loaded " + list.toString());
    for (int i=0; i<NUM_CONTACTS; i++) {
      ProfileObject p = new ProfileObject(DatabaseHelper.randProfile(i));
      assertTrue("Profile " + p + " not found in : " + list, list.contains(p));
    }
    
    ContactsListObject cload = ContactsListObject.load(ContactsListObject.class, cid);
    ProfileObject pload = ProfileObject.load(ProfileObject.class, pid);
    
    list = pload.contacts().getContactList();
    Log.d("ProfileTest", list.toString());
    for (int i=0; i<NUM_CONTACTS; i++) {
      ProfileObject p = new ProfileObject(DatabaseHelper.randProfile(i));
      assertTrue("Profile " + p + " not found in : " + list, list.contains(p));
    }
    
    list = cload.getContactList();
    for (int i=0; i<NUM_CONTACTS; i++) {
      ProfileObject p = new ProfileObject(DatabaseHelper.randProfile(i));
      assertTrue("Profile " + p + " not found in : " + list, list.contains(p));
    }
  }
  
  // We should probably add more tests here, but this is enough for now
  // public void testImageDownload() {
  // }
}

