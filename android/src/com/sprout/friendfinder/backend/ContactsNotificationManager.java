package com.sprout.friendfinder.backend;

import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.ui.ValidContactsActivity;

/**
 * manage notification: keep 
 * 2 cases:
 * 1) only one new unread interaction -> display common contacts for bigView + 2)
 * 2) many interactions -> only show # of unread interactions
 * @author Oak
 *
 */
// TODO: extends Application?
public final class ContactsNotificationManager {

	private static final String TAG = ContactsNotificationManager.class.getSimpleName();
	
	/**
	 * Upon receiving notification getting dismissed, clear interaction cache
	 * @author Oak
	 *
	 */
	public static class NotificationBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "Received deleted notification");
			ContactsNotificationManager.getInstance().clear();
		}
	};
	
	private static ContactsNotificationManager cInstance;
	
	private List<Interaction> unseenInteractions;
	
	private final static int REQUEST_CODE = 10;
	private final static int DISPLAY_MAX = 7;
	
	private ContactsNotificationManager() {
		unseenInteractions = new ArrayList<Interaction>();
	}
	
	public static ContactsNotificationManager getInstance() {
		if(cInstance == null) {
			synchronized (ContactsNotificationManager.class) {
				if(cInstance == null) {
					cInstance = new ContactsNotificationManager();
				}
			}
		}
		return cInstance;
	}
	
	/**
	 * clear cache
	 */
	public void clear() {
		Log.i(TAG, "clearing interaction cache");
		unseenInteractions.clear();
	}
	
	/**
	 * display a notification without any incoming interaction
	 *
	 */
	public void showNotification(Context context) {
		Log.i(TAG, unseenInteractions.size() + " unseen interactions");
	    NotificationCompat.Builder b = new NotificationCompat.Builder(context);
	    Intent notifyIntent = new Intent(context, ValidContactsActivity.class);
	    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	    // Useful flags if we want to resume a current activity
	    // .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    
	    // FLAG_CANCEL_CURRENT ensures that we don't see security errors. However, it also invalidates any given intents.
	    // In this case, this is the desired behavior. When a new conneciton is found, we would like to remove the old one.
	    PendingIntent pendingIntent = PendingIntent.getActivity( context, REQUEST_CODE, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT );

	    // Regular view
	    b.setContentIntent(pendingIntent)
	     .setSmallIcon(R.drawable.ic_notification)
	     .setContentTitle("Peer found")
	     .setContentText(""+unseenInteractions.size() + " peers.")
	     .setDeleteIntent(PendingIntent.getBroadcast(context, REQUEST_CODE, new Intent(context, NotificationBroadcastReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT))
	     .setAutoCancel(true);
	    
	    // Display all common contacts if there is only one peer
	    if(unseenInteractions.size() == 1) {
		    
	    	// Big view
	    	List<ProfileObject> allContacts = unseenInteractions.get(0).sharedContacts.getContactList();
	    	int numCommonConnections = allContacts.size();
	    	Log.i(TAG, numCommonConnections + " common connections");
		    NotificationCompat.InboxStyle style = 
		        new NotificationCompat.InboxStyle();
		    style.setBigContentTitle("Common connections:");
		    
		    if(numCommonConnections == 0) {
		    	style.addLine("No common connections");
		    } else {
			    for (int i=0; i < DISPLAY_MAX && i < allContacts.size(); i++){
			      style.addLine(allContacts.get(i).getDisplayName());
			    }
			    if( DISPLAY_MAX < allContacts.size() ) {
			      style.setSummaryText("And " + (allContacts.size() - DISPLAY_MAX) + " more...");
			    }
		    }
		    b.setStyle(style);
	    }
	 
	    NotificationManager mNotificationManager =
	        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	    mNotificationManager.notify(REQUEST_CODE, b.build());
	}
	
	public void showNotification(Context context, Interaction interaction) {
		unseenInteractions.add(interaction);
		showNotification(context);
	}
	
	// TODO: validating if the object is "fresh" by comparing to lastScanDevices
}
