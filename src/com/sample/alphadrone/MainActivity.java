package com.sample.alphadrone;

import java.io.IOException;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.ARDrone.State;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends Activity {
	
	private static String TAG = MainActivity.class.getName();
	
	private static final long CONNECTION_TIMEOUT = 10 * 1000;
	
	private static final int DATA_TIMEOUT = 10000;
	private static final int VIDEO_TIMEOUT = 60000;
	
	private static ARDrone sDrone;
	//private ARDrone drone;
	
	private boolean landed = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        
		setUpTakeOffBtn();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_connect:
			connect();
			return true;
		default:
			return false;
		}
	}
	
	protected void onResume(){
        super.onResume();
        if(connected()){
            sDrone.resumeNavData();
            sDrone.resumeVideo();
        }
    }
	
    @Override
    protected void onPause(){
        super.onPause();
        if(connected()){
            sDrone.pauseNavData();
            sDrone.pauseVideo();
        }
    }
    
    @Override
    protected void onDestroy(){
        super.onDestroy();
        disconnect();
    }

    private void disconnect(){
        if(connected()){
            try {
                sDrone.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "failed to stop drone", e);
            }
        }
    }
    
    private boolean disconnected(){
    	return sDrone == null;
    }
    
    private boolean connected(){
    	return !disconnected();
    }
	
	private void setUpTakeOffBtn(){
		Button btn = (Button)findViewById(R.id.drone_take_off);
		
		btn.setOnClickListener(new TakeOffListener());
	}
	
	private void setUpLandBtn(){
		Button btn = (Button)findViewById(R.id.drone_take_off);
		
		btn.setText(R.string.action_land);
	}
	
	private void restoreTakeOffBtn(){
		Button btn = (Button)findViewById(R.id.drone_take_off);
		
		btn.setText(R.string.action_take_off);
	}
	
	private void connect(){
        WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        
        if(manager.isWifiEnabled()){
            Crouton.showText(this, "Connecting via " +  manager.getConnectionInfo().getSSID() + "...", Style.INFO);
            (new DroneConnector()).execute(MainActivity.sDrone);
            //(new DroneStarter()).execute(MainActivity.sDrone);
        }else{
        	Crouton.showText(this, "Connect to your drone's wifi first.", Style.ALERT);
        }
    }
	
	private boolean takeOff(){
		boolean success = false;
		
		try{
			sDrone.clearEmergencySignal();
			sDrone.trim();
			sDrone.takeOff();
            success = true;
		}catch(IOException e){
			Log.e(TAG, "Faliled to execute take off command.", e);
		}
		
		return success;
	}
	
	private boolean land(){
		boolean success = false;
		
		try{
            sDrone.land();
            success = true;
        }catch(IOException e){
            Log.e(TAG, "Faliled to execute land command.", e);
        }
		
		return success;
	}
	
	private class TakeOffListener implements OnClickListener{
		@Override
		public void onClick(View v){
			if(sDrone == null || sDrone.getState() == State.DISCONNECTED){
				Log.w(TAG, "Not landing/taking off, drone is not connected.");
                return;
            }
            
            if(landed){
            	landed = !takeOff();
            	if(!landed){
            		setUpLandBtn();
            	}
            }else{
            	landed = land();
            	if(landed){
            		restoreTakeOffBtn();
            	}
            }
		}
	}
	
	private class DroneStarter extends AsyncTask<ARDrone, Integer, Boolean> {
	    
	    @Override
	    protected Boolean doInBackground(ARDrone... drones) {
	        ARDrone drone = drones[0];
	        try {
	            drone = new ARDrone(InetAddress.getByAddress(ARDrone.DEFAULT_DRONE_IP), 10000, 60000);
	            MainActivity.sDrone = drone;
	            drone.connect();
	            drone.clearEmergencySignal();
	            drone.trim();
	            drone.waitForReady(CONNECTION_TIMEOUT);
	            drone.playLED(1, 10, 4);
	            drone.selectVideoChannel(ARDrone.VideoChannel.HORIZONTAL_ONLY);
	            drone.setCombinedYawMode(true);
	            return true;
	        } catch (Exception e) {
	            Log.e(TAG, "Failed to connect to drone", e);
	            try {
	                drone.clearEmergencySignal();
	                drone.clearImageListeners();
	                drone.clearNavDataListeners();
	                drone.clearStatusChangeListeners();
	                drone.disconnect();
	            } catch (Exception ex) {
	                Log.e(TAG, "Failed to clear drone state", ex);
	            }
	          
	        }
	        return false;
	    }

	    protected void onPostExecute(Boolean success) {
	        if (success.booleanValue()) {
	            //droneOnConnected();
	        } else {
	        }
	    }
	   }
	
	private class DroneConnector extends AsyncTask<ARDrone, Integer, ARDrone>{
		
	    @Override
	    protected ARDrone doInBackground(ARDrone... drones){
	    	ARDrone drone = drones[0];
	    	
	    	try{
	    		drone = new ARDrone(InetAddress.getByAddress(ARDrone.DEFAULT_DRONE_IP), DATA_TIMEOUT, VIDEO_TIMEOUT);
	            drone.connect();
	            drone.clearEmergencySignal();
	            drone.trim();
	            drone.waitForReady(CONNECTION_TIMEOUT);
	            drone.playLED(1, 10, 4);
	            drone.selectVideoChannel(ARDrone.VideoChannel.HORIZONTAL_ONLY);
	            drone.setCombinedYawMode(true);
	            
	            //drones[0] = drone;
	            sDrone = drone;
	        }catch(Exception e){
	            Log.e(TAG, "Failed to connect to drone.", e);
	            try{
	                drone.clearEmergencySignal();
	                drone.clearImageListeners();
	                drone.clearNavDataListeners();
	                drone.clearStatusChangeListeners();
	                drone.disconnect();
	                
	                drone = null;
	                sDrone = null;
	            }catch(Exception ex){
	                Log.e(TAG, "Failed to clear drone state.", ex);
	            }
	        }
	        return drone;
	    }
	    
	    protected void onPostExecute(ARDrone drone){
	        if(connected()){
	        	Crouton.showText(MainActivity.this, "Connected.", Style.CONFIRM);
	            //droneOnConnected();
	        }else{
	        	Crouton.showText(MainActivity.this, "Connection failed.", Style.ALERT);
	        }
	    }
   }

}
