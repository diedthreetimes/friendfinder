package com.sprout.friendfinder.crypto.protocols;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.query.Select;
import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.backend.DeviceCache;
import com.sprout.friendfinder.backend.DiscoveryService.ProtocolCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.crypto.protocols.bearercap.CommonFriendsBearerCardinalityTest;
import com.sprout.friendfinder.crypto.protocols.bearercap.CommonFriendsBearerTest;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.Message;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.ui.BluetoothChatActivity;
import com.sprout.friendfinder.ui.BluetoothMessageActivity;
import com.sprout.friendfinder.ui.InteractionItem;

public class ProtocolManager {
  
  public static ProtocolType overrideProtocol = ProtocolType.NONE;
  
  // going to add protocol type/policy type in policy when implemented
  public enum ProtocolType {
    NONE(0),  // Least priority, run any other protocols 
    MSG(1), // messaging protocol
    IDX(2),  // identity protocol
    ATWPSI(3), // authorized two-way PSI
    BBFPSI(4), // bearer bloom filter PSI
    BPSI(5), // bearer PSI
    BBFPSI_No_V(6), // bearer bloom filter PSI w/o verification
    BBFPSICA(7), // bearer bloom filter PSICA
    BPSICA(8), // bearer PSICA
    ATWPSICA(9), // authorized two-way PSICA
    ATWPSICA_NEW(10), // faster authorized two-way PSICA
    PSICA(11); // traditional PSICA without TTP
    
    int priority;
    
    public static ProtocolType getProtocolType(String protName) {
      return ProtocolType.valueOf(protName);
    }
    private ProtocolType(int val) {
      priority = val;
    }
    
    public static ProtocolType max(ProtocolType p1, ProtocolType p2) {
      return p1.priority > p2.priority ? p1 : p2;
    }
  }
  
  static final String TAG = ProtocolManager.class.getSimpleName();
  
  

  
  /**
   * get type of protocol based on status of connected device and user's policy
   * @param context
   * @param connectedDevice
   * @return
   */
  public static ProtocolType getProtocolType(Context context, DeviceCache deviceCache, Device connectedDevice) {
    Log.i(TAG, "getting type of protocol");
    
    // override protocol if needed
    if(!overrideProtocol.equals(ProtocolType.NONE)) {
      return overrideProtocol;
    }
    
    // check for unsent msg
    // TODO: re-order these priority..

    Message m = new Select().from(Message.class).where("address = ?", connectedDevice.getAddress())
        .where("sent = ?", 0).executeSingle();
    if(m!=null) {
      Log.i(TAG, "return MSG");
      return ProtocolType.MSG;
    }
    
    // first check if the identity exchange button was clicked or policy allowed it
    // TODO: implement policy, now just check idx button
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> pendingExchangeAddr = pref.getStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, new HashSet<String>());
    if(pendingExchangeAddr.contains(connectedDevice.getAddress())) {
      Log.i(TAG, "return IDX");
      return ProtocolType.IDX;
    }

    // device not in cache, we should connect
    // TODO: hard-code PSICA or BPSICA
    if( !deviceCache.contains(connectedDevice) ) {
      Log.i(TAG, "return PSICA");
      return ProtocolType.BPSICA;
    }

    Log.i(TAG, "return NONE");
    return ProtocolType.NONE;
  }
  
  private static void runIDXProtocol(CommunicationService cs, ProtocolCallback callback,
      final Map<AuthorizationObjectType, AuthorizationObject> authMaps,
      final Interaction interaction) {
    
//    // check if the other side still wants to run IDX
//    // any better way to confirm that both side wanna do IDX?
//    byte[] send = new byte[1];
//    send[0] = (byte) 1;
//    cs.write(send);
//    
//    byte[] recv = cs.read();
//
//    if(recv == null || recv.length != 1 || recv[0] != (byte) 1) {
//      Log.i(TAG, "Error when reading recv: "+recv.toString());
//      callback.onError(null);
//      return;
//    }
    // reuse psi_ca authorizer for idx protocol
    new IdentityExchangeProtocol(cs, callback, authMaps.get(AuthorizationObjectType.PSI_CA), interaction).execute();
  }

  public static void runProtocol(ProtocolType protocol, final Map<AuthorizationObjectType, AuthorizationObject> authMaps,
      final ProtocolCallback callback, final Context c, 
      final CommunicationService cs, final Device connectedDevice, List<ProfileObject> contactList,
      final Interaction interaction) throws Exception {
  
    if(protocol.equals(ProtocolType.MSG)) {
      Log.i(TAG, "Running msg");
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
      boolean isFg = sp.getBoolean(BluetoothMessageActivity.IS_FOREGROUND, false);
      cs.write(isFg?1:0);
      boolean res = (cs.readInt()!=0?true:false);
      if(!(isFg && res)) {
        new MessageProtocol(cs, connectedDevice.getAddress(), callback).execute();
      } else {

        Intent i = new Intent(c, BluetoothChatActivity.class);
        i.putExtra(InteractionItem.EXTRA_DEVICE_ADDR, interaction.address);
        c.startActivity(i);
        callback.onComplete(0);
      }
    } else if(protocol.equals(ProtocolType.BPSICA)) {
      Log.i(TAG, "Running bpsica");
      new CommonFriendsBearerCardinalityTest(cs, authMaps.get(AuthorizationObjectType.B_PSI_CA), callback).execute(new String[0]);
    } else if(protocol.equals(ProtocolType.PSICA)) {
      Log.i(TAG, "Running psica");
      new CommonFriendsCardinalityTest(cs, authMaps.get(AuthorizationObjectType.PSI_CA), callback).execute();
    } else if(protocol.equals(ProtocolType.IDX)) {
      Log.i(TAG, "Start identity exchange");
      
      // TODO: we prolly should do PSI before IDX because one way to trigger IDX protocol is 
      // pressing id-x button after psi is done
      // if somehow the peer switched accounts before exchanging identity but after completing PSI
      // we might id-x with wrong person

      // if the connected device is in the identity-exchange list, then no need to show alert dialog
      Set<String> pendingExchangeAddr = PreferenceManager.getDefaultSharedPreferences(c).getStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, new HashSet<String>());
      if(pendingExchangeAddr.contains(connectedDevice.getAddress())) {
        runIDXProtocol(cs, callback, authMaps, interaction);
      } 
        
    } else if(protocol.equals(ProtocolType.ATWPSI)) {
//      String[] input = new String[contactList.size()];
//      for( int i=0; i < contactList.size(); i++) {
//        input[i] = contactList.get(i).getUid();
//      }
//      new CommonFriendsTest(cs, authMaps.get(AuthorizationObjectType.PSI), callback, interaction).execute(input);

      List<BigInteger> cap = authMaps.get(AuthorizationObjectType.PSI).getData();
      String[] input = new String[cap.size()];
//      Map<BigInteger, String> capMap = new HashMap<BigInteger, String>();
      for( int i=0; i < cap.size(); i++) {
        input[i] = cap.get(i).toString();
//        capMap.put(cap.get(i), input[i]);
      }
      new CommonFriendsTest(cs, authMaps.get(AuthorizationObjectType.PSI), callback, interaction).execute(input);
   } else {
      throw new Exception("Invalid protocol: "+protocol);
   }
//    else {
        
        // post notification

//        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
//        pendingIntent.send(0, new OnFinished() {
//          
//          @Override
//          public void onSendFinished(PendingIntent pendingIntent, Intent intent,
//              int resultCode, String resultData, Bundle resultExtras) {
//            Log.d(TAG, "sent!");
//            runIDXProtocol(cs, callback, authMaps, interaction);
//          }
//        }, null);
//
//       new Notification.Builder(c)
//        // Add media control buttons that invoke intents in your media service
//        .addAction(R.drawable.arrow_down_float, "Previous", pendingIntent)
//        .setContentTitle("IDX")
//        .setContentText("Do you want to exchange?")
//        .build();
        
        // show alert dialog
        
//        // get current foreground activity
//        Activity currentActivity = ((FriendFinderApplication)c.getApplicationContext()).getCurrentActivity();
//        
//        // if we dont own current activity, i guess just exit
//        // TODO: best thing to do in this case?
//        if(currentActivity == null) {
//          Log.e(TAG, "We dont own current foreground activity -> cant show alert dialog");
//          callback.onError();
//          return;
//        }
//        Log.i(TAG, "current activity: "+currentActivity.toString());
//        
//        
//        // show alert dialog
//        // TODO: need to reword on message
//        new AlertDialog.Builder(currentActivity)
//        .setCancelable(true)
//        .setTitle("Identity Exchange")
//        .setMessage("Do you want to exchange identity with device "+connectedDevice.getName())
//        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//          public void onClick(DialogInterface dialog, int whichButton)
//          {
//            dialog.dismiss();
//            
//            runIDXProtocol(cs, callback, authMaps, interaction);
//            return;
//          }
//        })
//        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//          public void onClick(DialogInterface dialog, int whichButton)
//          {
//            dialog.dismiss();
//            cs.write(new byte[1]);
//            
//            // just to complete the loop
//            cs.read();
//            
//            callback.onError();
//            return;
//          }
//        }).show();
//      }
      
//    } 
  }

  public static void runTestProtocol(ProtocolType protocolType, AuthorizationObject auth, ProtocolCallback callback,
      CommunicationService cs) {
    //TODO: extend this to other protocols
    Log.i(TAG, "Running protocol: "+protocolType.toString());
    if(protocolType.equals(ProtocolType.BPSICA)) {
      Log.i(TAG, "Running bpsica");
      new CommonFriendsBearerCardinalityTest(cs, auth, callback, true).execute(new String[0]);
    } else {
      Log.i(TAG, "Unsupported protocol: "+protocolType.toString());
      callback.onError(null);
    }
  }
  
}
