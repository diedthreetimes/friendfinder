package com.sprout.friendfinder.ui;

import java.util.List;

import com.sprout.friendfinder.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

public class IntersectionResultsActivity extends ListActivity {

  private static final String TAG = IntersectionResultsActivity.class.getSimpleName();
  
  //Intent Extras
  public static final String EXTRA_DISPLAY = "extra_display";

  @Override
  protected void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);

      //setContentView(R.layout.intersection_result);
      
      if(getIntent() == null) {
        Log.e(TAG, "Null intent provided");
        finish();
      }
      
      @SuppressWarnings("unchecked")
      List<String> toDisp = (List<String>) getIntent().getSerializableExtra(EXTRA_DISPLAY);
        

      ArrayAdapter<String> adapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, toDisp);

      // Bind to our new adapter.
      setListAdapter(adapter);
  }
}
