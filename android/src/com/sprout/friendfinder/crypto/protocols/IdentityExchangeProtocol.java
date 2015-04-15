package com.sprout.friendfinder.crypto.protocols;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.finderlib.crypto.PrivateProtocol;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.ui.InteractionItem;

/**
 * identity exchange protocol
 * TODO: now its a dummy protocol, doesnt do anything yet
 * @author norrathep
 *
 */
public class IdentityExchangeProtocol extends PrivateProtocol<Void, Void, String> {
  
  private Context context;
  private static final String TAG = IdentityExchangeProtocol.class.getSimpleName();
  private Device mRunningDevice;
  private ProfileDownloadCallback callback;
  
  public IdentityExchangeProtocol(CommunicationService s, Context c, Device connectedDevice, ProfileDownloadCallback callback) {
    this(IdentityExchangeProtocol.class.getSimpleName(), s, true);
    context = c;
    mRunningDevice = connectedDevice;
    this.callback = callback;
  }

  protected IdentityExchangeProtocol(String testName, CommunicationService s,
      boolean client) {
    super(testName, s, client);
  }

  @Override
  protected String conductClientTest(CommunicationService s, Void... input) {
    return conductServerTest(s, input);
  }

  @Override
  protected String conductServerTest(CommunicationService s, Void... input) {
    s.write("Your device is "+mRunningDevice.getAddress()+" "+mRunningDevice.getName());
    String message = s.readString();
    Log.i(TAG, "receive message: "+message);
    
    //remove addr in sharedpref
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> pendingExchangeAddr = pref.getStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, new HashSet<String>());
    pendingExchangeAddr.remove(mRunningDevice.getAddress());
    pref.edit().putStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, pendingExchangeAddr).commit();
    return message;
  }

  @Override
  protected void loadSharedKeys() {}
  
  @Override
  public String doInBackground(Void... params) {
    try {
      return super.doInBackground(params);
    } catch (Exception e) {
      Log.e(TAG, "IdentityExchangeProtocol failed, ", e);
      return null;
    }
  }

  @Override
  public void onPreExecute() {
    Log.i(TAG, "About to execute the identity exchange protocol");
  }

  @Override
  public void onPostExecute(String result) {
    if(result != null) {
      Log.i(TAG, "identity exchange complete");
      callback.onComplete();
    } else {
      Log.i(TAG, "identity exchange fail");
      callback.onError();
    }
  }
  
}
