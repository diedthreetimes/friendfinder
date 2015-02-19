package com.sprout.friendfinder.ui;

import java.io.IOException;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.ui.ItemAdapter.RowType;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class InteractionItem implements Item {
	
	Interaction interaction;

	private static final String TAG = ItemAdapter.class.getSimpleName();
	
	public InteractionItem(Interaction interaction) {
		Log.i(TAG, "Starting interactionItem");
		this.interaction = interaction;
	}

	@Override
	public int getViewType() {
		return RowType.INTERACTION_ITEM.ordinal();
	}
	
	public static class ViewHolder {
	    ImageView contactImage;
	    TextView displayName;
	    TextView contactsSize;
	    ImageButton exchangeIdentity;
	    ImageButton startMessaging;
	}


	@Override
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		Log.i(TAG, "in getView for InteractionItem");
		final Context context = inflater.getContext();
		final ViewHolder viewHolder;
	    if (convertView == null) {
	      convertView = (View) inflater.inflate(R.layout.interaction_layout, parent, false);

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
	    
	    if (interaction != null && interaction.sharedContacts != null) {
	      viewHolder.contactsSize.setText(context.getString(R.string.number_of_contacts,
	    		  interaction.sharedContacts.getContactList().size()));
	    }
	    
	    // TODO: Set on click listener for the two buttons
	    viewHolder.exchangeIdentity.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            // TODO Auto-generated method stub
	        	Toast.makeText(v.getContext(), "exchanged identity", Toast.LENGTH_LONG).show();
	        }
	    });

	    if (interaction.profile == null) {
	      viewHolder.displayName.setText(context.getString(R.string.anonomous_name));
	      viewHolder.contactImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_profile_image));
	    } else {
	      viewHolder.displayName.setText(interaction.profile.getDisplayName());
	      
	   // Set the image asynchronously
	      new AsyncTask<ProfileObject, Void, Drawable>() {
	        @Override
	        protected Drawable doInBackground(ProfileObject... params) {
	          if (params == null || params.length == 0 || params[0] == null)
	            return null;
	          ProfileObject prof = params[0];
	          try {
	            return prof.getProfileImage(context);
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
	      }.execute(interaction.profile);
	    }

	    return convertView;
	}
	
	public Interaction getInteraction() {
		return interaction;
	}

}
