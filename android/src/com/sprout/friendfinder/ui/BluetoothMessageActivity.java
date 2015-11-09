package com.sprout.friendfinder.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.activeandroid.query.Select;
import com.sprout.friendfinder.R;
import com.sprout.friendfinder.models.Message;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class BluetoothMessageActivity extends Activity {
  
  static String TAG = BluetoothMessageActivity.class.getSimpleName();
  public static String ADDRESS_NEW_MESSAGES = "address_new_messages";

  // Layout Views
  private ListView mConversationView;
  private EditText mOutEditText;
  private Button mSendButton;
  
  protected OnSharedPreferenceChangeListener listener;
  
  ArrayAdapter<String> mConversationArrayAdapter;
  String address;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bluetooth_chat_layout);
      
      address = getIntent().getExtras().getString(InteractionItem.EXTRA_DEVICE_ADDR);
      
      // setup listener
      SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
      listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
              
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                String key) {
            if(key.equals(BluetoothMessageActivity.ADDRESS_NEW_MESSAGES)) {
              Set<String> addrSet = new HashSet<String>(sharedPreferences.getStringSet(key, new HashSet<String>()));
              Log.i(TAG, "addrs for new msgs: "+addrSet);
              if(addrSet != null && addrSet.contains(address)) {
                addrSet.remove(address);
                PreferenceManager.getDefaultSharedPreferences(BluetoothMessageActivity.this).edit()
                .putStringSet(BluetoothMessageActivity.ADDRESS_NEW_MESSAGES, addrSet)
                .apply();
                
                loadMessages();
                mConversationArrayAdapter.notifyDataSetChanged();
              }
            }
            
        }
      };
      mPrefs.registerOnSharedPreferenceChangeListener(listener);
  }


  @Override
  public void onStart() {
      super.onStart();
      
      loadMessages();
      
      mConversationView = (ListView) findViewById(R.id.in);
      mOutEditText = (EditText) findViewById(R.id.edit_text_out);
      mSendButton = (Button) findViewById(R.id.button_send);

      mConversationView.setAdapter(mConversationArrayAdapter);
      
      // Initialize the send button with a listener that for click events
      mSendButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            String text = mOutEditText.getText().toString();
            if (text.length() > 0) {
              Message message = new Message(address,text);
              message.save();
              
              mOutEditText.setText("");
              
              mConversationArrayAdapter.add("Me: " + text);
            }
          }
      });
  }
  
  private void loadMessages() {
    Log.i(TAG, "loading msg for device: "+address);
    // TODO: only load new messages, not all msgs?
    List<Message> msgs = new Select().from(Message.class).where("address = ?", address).orderBy("timestamp ASC").execute();


    if(mConversationArrayAdapter == null) {
      mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
    } else {
      mConversationArrayAdapter.clear();
    }
    
    for(Message m : msgs) {
      String line = (m.owner ? "Me: " : address+": ") + m.content;
      mConversationArrayAdapter.add(line);
    }
  }


  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }
}
