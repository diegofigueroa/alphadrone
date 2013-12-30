package com.sample.alphadrone;

import java.io.IOException;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
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
	
	private boolean landed = true;
	private SparseArray<String> buttonIdMap = new SparseArray<String>();
		
	private final static String UP = "up";
	private final static String DOWN = "down";
	private final static String LEFT = "left";
	private final static String RIGHT = "right";
	private final static String FORWARD = "forward";
	private final static String BACKWARD = "backward";
	private final static String SPIN_LEFT = "spin left";
	private final static String SPIN_RIGHT = "spin right";
	private final static String HOVER = "hover";
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        
		setUpTakeOffBtn();
		setupButtons();
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
    
	private void setupButtons() {
		ControlsListener listener = new ControlsListener();
		
		Button btn = (Button)findViewById(R.id.btnUp);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), UP);

		btn = (Button)findViewById(R.id.btnDown);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), DOWN);

		btn = (Button)findViewById(R.id.btnLeft);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), LEFT);

		btn = (Button)findViewById(R.id.btnRight);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), RIGHT);
		
		btn = (Button)findViewById(R.id.btnForward);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), FORWARD);
		
		btn = (Button)findViewById(R.id.btnBackward);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), BACKWARD);
		
		btn = (Button)findViewById(R.id.btnSpinLeft);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), SPIN_LEFT);
		
		btn = (Button)findViewById(R.id.btnSpinRight);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), SPIN_RIGHT);		

		btn = (Button)findViewById(R.id.btnHover);
		btn.setOnClickListener(listener);
		buttonIdMap.put(btn.getId(), HOVER);		
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
	
	private class ControlsListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Button btn = (Button)v;
			String action = buttonIdMap.get(btn.getId());
			if (action != null) {
				if (action.equals(RIGHT)) {
					right(1.0f);
				} else if (action.equals(LEFT)) {
					left(1.0f);
				} else if (action.equals(FORWARD)) {
					forward(1.0f);
				} else if (action.equals(BACKWARD)) {
					backward(1.0f);					
				} else if (action.equals(UP)) {
					up(1.0f);
				} else if (action.equals(DOWN)) {
					down(1.0f);
				} else if (action.equals(SPIN_LEFT)) {
					turnLeft(1.0f);
				} else if (action.equals(SPIN_RIGHT)) {
					turnRight(1.0f);
				} else if (action.equals(HOVER)) {
					try {
						sDrone.hover();
					} catch (IOException e) {
						Log.e(TAG, "Faliled to execute land command.", e);
					}
				}
			} 
		}
		
	}
	
	/*
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
	*/
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
	        }else{
	        	Crouton.showText(MainActivity.this, "Connection failed.", Style.ALERT);
	        }
	    }   
    }
	
	public void right(float tilt) {
		if (connected()) {
			try {
				sDrone.move(tilt, 0.0f, 0.0f, 0.0f);
	        }catch(IOException e){
	            Log.e(TAG, "Faliled to execute land command.", e);
	        } 
		}
	}
	
	public void left(float tilt) {
		right(tilt * -1);
	}
	
	public void backward(float tilt) {
		if (connected()) {
			try {
				sDrone.move(0.0f, tilt, 0.0f, 0.0f);
	        }catch(IOException e){
	            Log.e(TAG, "Faliled to execute land command.", e);
	        } 
		}
	}	
	
	public void forward(float tilt) {
		backward(tilt * -1);
	}
	
	public void up(float speed) {
		if (connected()) {
			try {
				sDrone.move(0.0f, 0.0f, speed, 0.0f);
	        }catch(IOException e){
	            Log.e(TAG, "Faliled to execute land command.", e);
	        } 
		}
	}	
	
	public void down(float speed) {
		up(speed * -1);
	}
	
	public void turnRight(float speed) {
		if (connected()) {
			try {
				sDrone.move(0.0f, 0.0f, 0.0f, speed);
	        }catch(IOException e){
	            Log.e(TAG, "Faliled to execute land command.", e);
	        } 
		}
	}
	
	public void turnLeft(float speed) {
		turnRight(speed * -1);
	}	
}
