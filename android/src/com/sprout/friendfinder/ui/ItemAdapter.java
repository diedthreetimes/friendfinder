package com.sprout.friendfinder.ui;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ItemAdapter extends ArrayAdapter<Item> {

	
	public enum RowType {
        INTERACTION_ITEM, HEADER_ITEM
    }
	private LayoutInflater mInflater;
	private static final String TAG = ItemAdapter.class.getSimpleName();
	
	public ItemAdapter(Context context, List<Item> objects) {
		super(context, -1, objects);
		Log.i(TAG, "creating item adapter with size " + objects.size());
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getViewTypeCount() {
	    return RowType.values().length;
	}
	
	@Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position).getView(mInflater, convertView, parent);
		
	}
}
