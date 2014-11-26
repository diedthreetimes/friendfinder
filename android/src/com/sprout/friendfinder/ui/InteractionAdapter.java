package com.sprout.friendfinder.ui;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;

public class InteractionAdapter extends ArrayAdapter<Interaction> {

  private static String TAG = InteractionAdapter.class.getSimpleName();

  public InteractionAdapter(Context context, List<Interaction> interactions) {
    super(context,-1,interactions);
  }

  public static class ViewHolder {
    ImageView contactImage;
    TextView displayName;
    TextView contactsSize;
    ImageButton exchangeIdentity;
    ImageButton startMessaging;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    Interaction rowItem = getItem(position);

    // Since we just have one view this tagging like this is probably not necessary
    final ViewHolder viewHolder;
    if (convertView == null) {
      convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.interaction_layout, parent, false);

      viewHolder = new ViewHolder();
      viewHolder.contactImage = (ImageView) convertView.findViewById(R.id.contactImage);
      viewHolder.displayName = (TextView) convertView.findViewById(R.id.displayName);
      viewHolder.contactsSize = (TextView) convertView.findViewById(R.id.contactsSize);
      viewHolder.exchangeIdentity = (ImageButton) convertView.findViewById(R.id.exchangeIdentity);
      viewHolder.startMessaging = (ImageButton) convertView.findViewById(R.id.startMessaging);

      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }
    
    viewHolder.contactsSize.setText(getContext().getString(R.string.number_of_contacts,
        rowItem.sharedContacts.getContactList().size()));
    
    // TODO: Set on click listener for the two buttons

    if (rowItem.profile == null) {
      viewHolder.displayName.setText(getContext().getString(R.string.anonomous_name));
      viewHolder.contactImage.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_profile_image));
    } else {
      viewHolder.displayName.setText(rowItem.profile.getDisplayName());
      
   // Set the image asynchronously
      new AsyncTask<ProfileObject, Void, Drawable>() {
        @Override
        protected Drawable doInBackground(ProfileObject... params) {
          if (params == null || params.length == 0 || params[0] == null)
            return null;
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
          if (image != null) {
            viewHolder.contactImage.setImageDrawable(image);
          }
        }
      }.execute(rowItem.profile);
    }

    return convertView;
  }
}
