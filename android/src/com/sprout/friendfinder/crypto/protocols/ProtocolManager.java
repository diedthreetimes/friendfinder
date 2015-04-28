package com.sprout.friendfinder.crypto.protocols;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sprout.finderlib.communication.CommunicationService;
import com.sprout.finderlib.communication.Device;
import com.sprout.friendfinder.backend.DeviceCache;
import com.sprout.friendfinder.backend.DiscoveryService.ProfileDownloadCallback;
import com.sprout.friendfinder.crypto.AuthorizationObject;
import com.sprout.friendfinder.crypto.AuthorizationObject.AuthorizationObjectType;
import com.sprout.friendfinder.models.Interaction;
import com.sprout.friendfinder.models.ProfileObject;
import com.sprout.friendfinder.ui.InteractionItem;

public class ProtocolManager {
  
  // going to add protocol type/policy type in policy when implemented
  public static String PSICA = "PSICA";
  public static String PSI = "PSI";
  public static String IDX = "IDENTITY_EXCHANGE";
  public static String NONE = "NONE";
  
  static final String TAG = ProtocolManager.class.getSimpleName();
  
  public static List<String> getAllSupportedProtocols() {
    List<String> allProts = new ArrayList<String>() {{
      add(PSICA);
      add(PSI);
      add(IDX);
    }};
    return allProts;
  }
  
  /**
   * get type of protocol based on status of connected device and user's policy
   * @param context
   * @param connectedDevice
   * @return
   */
  public static String getProtocolType(Context context, DeviceCache deviceCache, Device connectedDevice) {
    Log.i(TAG, "getting type of protocol");
    
    // first check if the identity exchange button was clicked or policy allowed it
    // TODO: implement policy, now just check idx button
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> pendingExchangeAddr = pref.getStringSet(InteractionItem.EXCHANGE_IDENTITY_ADDR, new HashSet<String>());
    if(pendingExchangeAddr.contains(connectedDevice.getAddress())) {
      Log.i(TAG, "return IDX");
      return IDX;
    }

    // device not in cache, we should connect
    // TODO: depend on PSI
    if( !deviceCache.contains(connectedDevice) ) {
      Log.i(TAG, "return PSICA");
      return PSICA;
    }

    Log.i(TAG, "return NONE");
    return NONE;
    
  }

  public static void runProtocol(String protocol, Map<AuthorizationObjectType, AuthorizationObject> authMaps,
      final ProfileDownloadCallback callback, final Context c, 
      final CommunicationService cs, final Device connectedDevice, List<ProfileObject> contactList,
      Interaction interaction) throws Exception {
    
    if(protocol.equals(PSICA)) {

      // prepare before running PSiCA
      if (authMaps == null || contactList == null) {
        Log.i(TAG, "Authorization or contactList not loaded, aborting test");  
        throw new Exception("invalid protocol");
      }
      
      if(!authMaps.containsKey(AuthorizationObjectType.PSI) && !authMaps.containsKey(AuthorizationObjectType.PSI_CA)) {
        Log.e(TAG, "auths dont exist, aborting test");
        throw new Exception("invalid protocol");
      }

      String[] input = new String[contactList.size()];
      for( int i=0; i < contactList.size(); i++) {
        input[i] = contactList.get(i).getUid();
      }
      
      Log.i(TAG, "Start common friends cardinality");
      new CommonFriendsCardinalityTest(cs, authMaps.get(AuthorizationObjectType.PSI_CA), authMaps.get(AuthorizationObjectType.PSI), callback, interaction, input).execute(input);
    } else if(protocol.equals(IDX)) {
      Log.i(TAG, "Start identity exchange");
      new IdentityExchangeProtocol(cs, callback, authMaps.get(AuthorizationObjectType.PSI_CA), interaction).execute();
    } else {
      throw new Exception("Invalid protocol: "+protocol);
    }
  }
}
