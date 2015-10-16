package com.sprout.friendfinder.ui.baseui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public interface Item {
    public int getViewType();
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent);
}