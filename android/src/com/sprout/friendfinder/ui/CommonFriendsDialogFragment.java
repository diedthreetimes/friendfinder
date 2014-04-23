/*Ron
 *  this implements the dialog that is shown when the PSI is completed; the contacts in common are shown as a result
 *  a callback is done to FriendFinderActivity to notify it whether the user wants to become friend with the peer or not; the result is handled in FriendFinderActivity
 */

package com.sprout.friendfinder.ui;

import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import com.sprout.friendfinder.R;

public class CommonFriendsDialogFragment extends DialogFragment {
	
	private String[] commonFriends;
	
	public void setCommonFriends(String[] commonFriends) {
		this.commonFriends = commonFriends;
	}
	
	public void setCommonFriends(Collection<String> commonFriends) {
		setCommonFriends(commonFriends.toArray(new String[0]));
	}
	
	
	//the main activity (FriendFinderActivity) needs to implement this interface so that it receives event callbacks
	public interface NoticeCommonFriendsDialog {
		public void onDialogPositiveClick(DialogFragment dialog);
		public void onDialogNegativeClick(DialogFragment dialog);
	}
	
	NoticeCommonFriendsDialog  mListener;
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (NoticeCommonFriendsDialog) activity;
		} catch (Exception e) {
			
		}
	}
	
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		builder.setTitle("Friends in common shown below. Become friends?");
		
		
		builder.setView(inflater.inflate(R.layout.commonfriends, null))
		
		.setPositiveButton(R.string.yesString, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					//just notify host dialog (FriendFinderActivity) about it:
					mListener.onDialogPositiveClick(CommonFriendsDialogFragment.this);
				}
			})
			.setNegativeButton(R.string.noString, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					mListener.onDialogNegativeClick(CommonFriendsDialogFragment.this);
				}
			
	});
		
			
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, commonFriends);
		// ListView listView = (ListView) getActivity().findViewById(R.id.listView1);
		
		
		builder.setAdapter(adapter, null);
		
		
		return builder.create();
	}
}
