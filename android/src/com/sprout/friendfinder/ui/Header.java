package com.sprout.friendfinder.ui;

import com.sprout.friendfinder.R;
import com.sprout.friendfinder.ui.ItemAdapter.RowType;
import com.sprout.friendfinder.ui.baseui.Item;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Header implements Item {
    private final String name;

    public Header(String name) {
        this.name = name;
    }

    @Override
    public int getViewType() {
        return RowType.HEADER_ITEM.ordinal();
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = (View) inflater.inflate(R.layout.header, parent, false);
        } else {
            view = convertView;
        }

        TextView text = (TextView) view.findViewById(R.id.separator);
        text.setText(name);

        return view;
    }

}
