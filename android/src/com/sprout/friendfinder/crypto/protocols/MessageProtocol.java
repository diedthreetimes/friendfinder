package com.sprout.friendfinder.crypto.protocols;

import java.util.List;

import org.json.JSONException;

import com.activeandroid.query.Select;
import com.activeandroid.util.Log;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.crypto.PrivateProtocol;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.models.Message;

public class MessageProtocol extends PrivateProtocol<Void, Void, Integer> {

  static String TAG = MessageProtocol.class.getSimpleName();
  
  String addr;
  List<Message> messages;
  ProtocolCallback callback;
  
  public MessageProtocol(CommunicationService s, String a, ProtocolCallback c) {
    super(TAG, s, true);
    
    callback = c;
    addr = a;
    messages = new Select().from(Message.class).where("address = ?", addr).
        where("sent = ?", 0).orderBy("timestamp ASC").execute();
  }

  public MessageProtocol(String testName, CommunicationService s, boolean client) {
    super(testName, s, client);
  }

  @Override
  protected Integer conductClientTest(CommunicationService s, Void... input) {
    // TODO Auto-generated method stub
    return conductServerTest(s, input);
  }

  @Override
  protected Integer conductServerTest(CommunicationService s, Void... input) {
    Log.i(TAG, "Start test");
    
    s.write(messages.size()+"");
    if(!messages.isEmpty()) {
      for(Message m : messages) {
        String encode = m.toJsonString();
        s.write(encode);
        m.sent = true;
        m.save();
        Log.d(TAG, "sent msg: "+m.content);
      }
    }
      

    int numMessages = Integer.valueOf(s.readString());
    Log.i(TAG, "num messages: "+numMessages);
    for(int i=0; i<numMessages; i++) {
      Message m2;
      try {
        m2 = new Message(new Message(s.readString()), addr);
        m2.save();
        Log.d(TAG, "recv msg: "+m2.content);
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    Log.i(TAG, "done");
    
    return numMessages;
  }

  @Override
  public Integer doInBackground(Void... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "MessageProtocol failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the message protocol");
    
    
  }
  @Override
  protected void onPostExecute(Integer result) {
    callback.onComplete(result);
  }

  @Override
  protected void loadSharedKeys() {
    // TODO Auto-generated method stub
    
  }

}
