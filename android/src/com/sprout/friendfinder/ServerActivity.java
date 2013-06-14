/* Ron
 * deprecated; use old WiFi functionality instead
 */

package com.sprout.friendfinder;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ServerActivity extends AsyncTask {
	
	private Activity activity; 

	public ServerActivity(Activity activity) {
		this.activity = activity;
	}
	
	/*public ServerActivity() {
		
	}*/
	
	@Override
	protected Object doInBackground(Object... arg0) {

		try {
			ServerSocket serverSocket = new ServerSocket(8888);
			Socket client = serverSocket.accept();
			
			InputStream in = client.getInputStream();
			
			byte[] buf = new byte[1024];
			in.read(buf);
			
			Toast.makeText(activity, "Message from client: " + buf, Toast.LENGTH_SHORT).show();
			
			in.close();
			
			client.close();
			serverSocket.close();
			
			
			
		} catch (Exception e) {
			
		}
		
		
		
		return null;
	}

}
