/*
 * Ron: 
 * deprecated; use old WiFi functionality instead
 */

package com.sprout.friendfinder.communication;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.content.*;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;



/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 * Ron, Apr. 2013, adapted from Android Developer Tutorial
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private Activity mActivity;
    
    private List peers = new ArrayList();
    
    private PeerListListener myPeerListListener; 
    
    //for data transfer:
    private String host;
    int port;
    int len;
    Socket socket;
    byte buf[];

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
            Activity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        //which action is triggered when the other device connects to this one? -> when Wifi button is pressed, open up server that listens on port 8888 for incoming requests
        
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        	if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
        		Log.d("Wifi Broadcastreceiver", "Wifi Direct enabled");
        	} else {
        		Log.d("Wifi Broadcastreceiver", "Wifi Direct not enabled");
        	}
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
        	Log.d("Wifi Broadcastreceiver", "WiFi P2P Peers changed");
        	if (mManager != null) {
        		mManager.requestPeers(mChannel, myPeerListListener = new PeerListListener() {
        	    	@Override
        	    	public void onPeersAvailable(WifiP2pDeviceList peerList) { //is called when a peer discovery is triggered by pressing the Wifi button
        	    		peers.clear();
        	    		peers.addAll(peerList.getDeviceList());
        	    		
        	    		
        	    		//((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged(); -> could be used if we had a ListView of available peers
        	    		if (peers.size() == 0) {
        	    			Log.d("WiFi Peer Listener", "No WiFi P2P devices found");
        	    			return;
        	    		} else {
        	    			Log.d("WiFi Peer Listener", peers.size() + " WiFi P2P devices found");
        	    		}
        	    		
        	    		//right place here to connect to peers? -> here: connection could only happen if peer set changes..
        	    		//check first whether a connection to that peer (based on MAC address) has been done before (if so, do not re-connect again)
        	    		//final WifiP2pDevice device; //really final?
        	    		
        	    		final WifiP2pDevice device = (WifiP2pDevice) peers.get(0); //new: final
        	    		WifiP2pConfig config = new WifiP2pConfig();
        	    		config.deviceAddress = device.deviceAddress;
        	    		//config.wps.setup = WpsInfo.PBC;
        	    		
        	    		mManager.connect(mChannel, config, new ActionListener() {

							@Override
							public void onFailure(int arg0) {
								// TODO Auto-generated method stub
								Log.d("Wifi Peer Listener", "Connection to peer failed");
							}

							@Override
							public void onSuccess() {
								// TODO Auto-generated method stub
								Log.d("Wifi Peer Listener", "Connection to peer ok");
								
								//start transfer data: (i.e. do the PSI)
								socket = new Socket();
								buf = new byte[1024];
								host = device.deviceName; 
								port = 8888;
								
								try {
									socket.bind(null);
									socket.connect((new InetSocketAddress(host, port)), 500);
									
									OutputStream out = socket.getOutputStream();
									out.write(buf);
									out.close();
									
									socket.close();
								} catch (Exception e) {
									
								}
							}
        	    			
        	    		}); 
        	    		
        	    		
        	    		/*
        	    		
        	    		WifiP2pConfig config = new WifiP2pConfig();
        	    		
        	    		Collection<WifiP2pDevice> peerCollection = (Collection<WifiP2pDevice>) peers; //not necessary...
        	    		
        	    		for (final WifiP2pDevice device : peerCollection) {
        	    			
        	    			config.deviceAddress = device.deviceAddress;
        	    		
        	    			mManager.connect(mChannel, config, new ActionListener() {
        	    				@Override
        	    				public void onSuccess() {
        	    					Log.d("WiFi Peer Listener", "Connected to device " + device.deviceAddress.toString());
        	    				}
        	    			
        	    				@Override
        	    				public void onFailure(int reason) {
        	    					Log.d("WiFi Peer Listener", "Could not connect to device because of " + reason);
        	    				}
        	    			});
        	    		
        	    		} //end for
        	    		
        	    		
        	    		*/
        	    		
        	    		
        	    	} 
        	    }); //async. call -> callback on PeerListListener.onPeersAvailable()
        	}
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }
    
    
}