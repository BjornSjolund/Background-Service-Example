/**
 * This is a simple activity to start and stop the positioning service.
 * 
 * @author Bjšrn Sjšlund
 */

package com.walkbase.locationservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LocationActivity extends Activity {
	
	private static final String TAG = "MainActivity"; 
	private Button start, stop;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		start = (Button) this.findViewById(R.id.StartService);
		stop = (Button) this.findViewById(R.id.StopService);
		
		start.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Log.v(TAG, "Starting service..."); 
				Intent intent = new Intent(LocationActivity.this, LocationService.class); 
				startService(intent);
				
			}
		});
		
		stop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				stopService();
			}
		});

	}

	
	private void stopService() {
		Log.v(TAG, "Stopping service..."); 
		
		if(stopService(new Intent(LocationActivity.this,LocationService.class))) {
			Log.v(TAG, "stopService was successful");
		}
		else{
			Log.v(TAG, "stopService was unsuccessful");
		}
	}
	
	@Override
	public void onDestroy() {
		stopService();
		
		super.onDestroy(); }
}


