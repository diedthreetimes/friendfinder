package com.sprout.friendfinder.ui;

import java.io.IOException;
import java.util.List;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.ContactsListObject;
import com.sprout.friendfinder.models.ProfileObject;

import android.app.ListActivity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class IntersectionResultsActivity extends ListActivity {

  private static final String TAG = IntersectionResultsActivity.class.getSimpleName();
  
  //Intent Extras
  public static final String EXTRA_DISPLAY = "extra_display";

  @Override
  protected void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);

      if(getIntent() == null) {
        Log.e(TAG, "Null intent provided");
        finish();
      }
      
      getActionBar().setDisplayHomeAsUpEnabled(true);
      getActionBar().setHomeButtonEnabled(true);

      long id = getIntent().getLongExtra(EXTRA_DISPLAY, -1);
      ContactsListObject contacts = ContactsListObject.load(ContactsListObject.class, id);
      
      if (contacts == null) {
        Log.e(TAG, "Contacts object not found for " + id);
        finish();
      }
      List<ProfileObject> toDisp =  contacts.getContactList();
        

      ArrayAdapter<ProfileObject> adapter =  new ArrayAdapter<ProfileObject>(this, -1, toDisp) {
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          ProfileObject rowItem = getItem(position);

          // Since we just have one view this tagging like this is probably not necessary
          final TextView txt;
          if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.contact_layout, null);
            txt = (TextView) convertView.findViewById(R.id.displayName);
            convertView.setTag(txt);
          } else {
            txt = (TextView) convertView.getTag();
          }
              
          txt.setText(rowItem.getDisplayName());
          txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_profile_image, 0, 0, 0);
          // Set the image asynchronously
          new AsyncTask<ProfileObject, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(ProfileObject... params) {
              ProfileObject prof = params[0];
              try {
                return prof.getProfileImage(getContext());
              } catch(IOException e) {
                Log.d(TAG, "Unable to load profile image");
                return null;
              }
            }
            
            @Override
            protected void onPostExecute(Drawable image) {
              float density = getResources().getDisplayMetrics().density;
              if (image != null) {
                image.setBounds(new Rect(0,0,(int)(68*density),(int)(68*density)));
                txt.setCompoundDrawables(image, null, null, null);
                //txt.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
              }
            }
          }.execute(rowItem);
          

          return convertView;
        }
      };

      // Bind to our new adapter.
      setListAdapter(adapter);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      onBackPressed();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
}
