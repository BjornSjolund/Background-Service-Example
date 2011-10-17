/**
 * This service will do indoor positioning requests through the Footprint library every 5 minutes and show the results in the notification bar.
 * 
 * @author Björn Sjölund
 */
package com.walkbase.locationservice;

import java.util.ArrayList;

import com.walkbase.positioning.Positioning;
import com.walkbase.positioning.data.Recommendation;


import android.app.Notification;
import android.app.NotificationManager; 
import android.app.PendingIntent; 
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


public class LocationService extends Service {
	//The walkbase api key must be entered here, use the one you got when you registered.
	private static final String WALKBASE_API_KEY = "111222333444555666777888999";
	private static final int FIVE_MINUTES = 300000;

	private static final String TAG = "BackgroundService";
	private NotificationManager notificationMgr;
	private ThreadGroup myThreads = new ThreadGroup("ServiceWorker");
	
	//Footprint library stuff
	private Positioning positioning;
	private VerificationReceiver verificationReceiver;
	private ArrayList<Recommendation> recommendations;
	
	private String currentLocationID;
	private String currentLocationReadableName;
	private boolean continueScanning;
	private String lastLocationID ="";
	private double[] gps;
	private double currentLocationScore;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.continueScanning = true;
		Log.v(TAG, "in onCreate()");
		
		//Initialize the library with your own api key
		positioning = new Positioning(this, WALKBASE_API_KEY);
		
		verificationReceiver = new VerificationReceiver();
		//Register the receiver that will catch the positioing intents
		this.registerReceiver(verificationReceiver, new IntentFilter(positioning.getPositioningIntentString()));
		
		notificationMgr =(NotificationManager)getSystemService(NOTIFICATION_SERVICE); 
		
		displayNotificationMessage("Background Service is running");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
	
		Log.v(TAG, "executing onStartCommand()");
		new Thread(myThreads, new ServiceWorker(), "BackgroundService").start();
		return START_STICKY; 
	}

	/**
	 * This is where the actual requests are made
	 * @author bjorn
	 *
	 */
	class ServiceWorker implements Runnable {
		
		public ServiceWorker() {
		}
		public void run() {
			final String TAG2 = "ServiceWorker:" +	Thread.currentThread().getId();

			try {
			
				Log.v(TAG2, "Fetching recommendations");
				
				
				while(continueScanning){
					if(gps != null){
						//positioning.fetchRecommendations(gps[0], gps[1], positioning.getGPSAccuracy());
						positioning.fetchRecommendations();
						Thread.sleep(FIVE_MINUTES);
					}
				}
			} 
			catch (Exception e) {
					Log.v(TAG2, "... sleep interrupted"); 
			}
	
	} 
}

		
		
@Override
public void onDestroy() {
	continueScanning = false;
	positioning.finish();
	Log.v(TAG, "in onDestroy(). Interrupting threads and cancelling notifications"); myThreads.interrupt();
	notificationMgr.cancelAll();
	this.unregisterReceiver(verificationReceiver);
	
	super.onDestroy();
}

@Override
public IBinder onBind(Intent intent) {
	Log.v(TAG, "in onBind()");
	return null;
}

private void displayNotificationMessage(String message) {
	Notification notification =
			new Notification(R.drawable.notification,
					message, System.currentTimeMillis());
	notification.flags = Notification.FLAG_NO_CLEAR;
	
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
			new Intent(this, LocationActivity.class), 0);
	notification.setLatestEventInfo(this, TAG, message, contentIntent);
	notificationMgr.notify(0, notification); 
	}

/**
 * Intent catcher.
 */
public class VerificationReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		//This variable holds the kind of intent this is about.
		int intentType = intent.getIntExtra(Positioning.POSITIONING_ERROR_MESSAGE, 0);
		String errorMessage = "";
		//
		//Check for errors
		if(intent.hasExtra("hasError")){
			errorMessage = intent.getStringExtra(Positioning.POSITIONING_ERROR_MESSAGE);
			Log.e("debug", "Walkbase Library error: " + errorMessage);
		}else
		{ // If there is no error
			switch(intentType){
				case Positioning.NORMAL_VERIFICATION: 
					//Normal verification
					
					break;
					
				case Positioning.ASSISTED_VERIFICATION: 
					//Assisted verification
					
					break;
				
				case Positioning.COMMIT: 
					//
					break;
					
				case Positioning.NORMAL_RECOMMENDATION: 
					// New recommendations received.
					Log.d("debug", "New recommendations received.");
	
					recommendations = positioning.getRecommendations();
					
					if(recommendations != null){
						if(recommendations.size() > 0){		
							// Assume the first location is valid, get hashtags for that location.
							currentLocationID = recommendations.get(0).getLocationId();
							currentLocationReadableName = recommendations.get(0).getLocationName();
							currentLocationScore = recommendations.get(0).getScore();
							if(!lastLocationID.equals(currentLocationID)){
								lastLocationID = currentLocationID;
								displayNotificationMessage("Found: " + currentLocationReadableName+", Score: "+ currentLocationScore);
							}
						}
					}
					break;
				
				case Positioning.GPS_POSITION_RESPONSE:
					//
					if(intent.hasExtra("latitude") && intent.hasExtra("longitude")){
						double lat = intent.getDoubleExtra(Positioning.GPS_LATITUDE,-1);
						double lon = intent.getDoubleExtra(Positioning.GPS_LONGITUDE,-1);
						gps = new double[]{lat, lon};
					}
					
					break;
				}		
		}
	}
}

}
