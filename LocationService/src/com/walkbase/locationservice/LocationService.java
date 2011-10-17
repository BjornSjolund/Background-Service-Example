/**
 * This service will do indoor positioning requests through the Footprint library every 5 minutes and show the results in the notification bar.
 * 
 * @author Björn Sjölund, Niclas Jern
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
	// TODO: Replace this with your own key!
	private static final String WALKBASE_API_KEY = "dcbdda9583da5fc69bd506d458fd80e71e86685c";
	
	private static final int FIVE_MINUTES = 300000;
	private static final int TEN_SECONDS = 10000;
    
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
    
    
	@Override
	public void onCreate() {
		super.onCreate();
		this.continueScanning = true;
		Log.v(TAG, "in onCreate()");
        
		//Initialize the library with your own api key
		positioning = new Positioning(this, WALKBASE_API_KEY);
        
		// Create the receiver for intents sent from the Positioning instance.
		verificationReceiver = new VerificationReceiver();
        
		//Register the receiver that will catch the Positioning intents
		this.registerReceiver(verificationReceiver, new IntentFilter(positioning.getPositioningIntentString()));
        
		// Get a reference to the Notification Service.
		notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); 
        
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
	 * This class handles keeping track of where you are.
	 */
	class ServiceWorker implements Runnable {
        
		public ServiceWorker() {
		}
		public void run() {
			continueScanning = true;
			final String TAG2 = "ServiceWorker:" +	Thread.currentThread().getId();
            
			try {
				Log.v(TAG2, "Fetching recommendations");
                
				// Get GPS Coordinates.
				double[] gps = positioning.getCoordinates();
                
				while(continueScanning){
                    
					// Check that we have valid GPS data
					if(gps != null) {
						double latitude = gps[0];
						double longitude = gps[1];
						double accuracy = positioning.getGPSAccuracy();
                        
						// Fetch new recommendations
						positioning.fetchRecommendations(latitude, longitude, accuracy);
                        
						// Wait five minutes before trying again
						Thread.sleep(FIVE_MINUTES);
					}
                    
					else {
						// If we haven't gotten a valid GPS position yet, sleep ten seconds before trying again
						Thread.sleep(TEN_SECONDS);
					}
				}
			} 
			catch (Exception e) {
				Log.v(TAG2, "... sleep interrupted"); 
			}
            
		} 
	}
    
    
    /**
     * Handle cleanup when the service is destroyed.
     */
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
    
	/**
	 * Call this to display a notification to the user.
	 * @param String The message you want to display to the user.
	 */
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
	 * Intent catcher for Positioning events.
	 */
	public class VerificationReceiver extends BroadcastReceiver{
        
		@Override
		public void onReceive(Context context, Intent intent) {
			
			// This variable holds the kind of intent this is about.
			int intentType = intent.getIntExtra(Positioning.POSITIONING_INTENT_TYPE, 0);
			String errorMessage = "";
			
			//Check for errors
			if(intent.hasExtra(Positioning.POSITIONING_HAS_ERROR)){
				errorMessage = intent.getStringExtra(Positioning.POSITIONING_ERROR_MESSAGE);
				Log.e("debug", "Walkbase Library error: " + errorMessage);
			}
            
            
			else { // If there is no error
                
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
                        
                    case Positioning.ASSISTED_RECOMMENDATION: 
                    case Positioning.NORMAL_RECOMMENDATION: 
                        
                        // New recommendations were received.
                        Log.d("debug", "New recommendations received.");
                        
                        recommendations = positioning.getRecommendations();
                        
                        if(recommendations != null){
                            if(recommendations.size() > 0){		
                                
                                /**
                                 * Locations arrive in an ordered format, with the most likely location
                                 * first in the array. We assume this location is the correct one and
                                 * make this the current location.
                                 */
                                
                                currentLocationID = recommendations.get(0).getLocationId();
                                currentLocationReadableName = recommendations.get(0).getLocationName();
                                
                                if(!lastLocationID.equals(currentLocationID)){
                                    lastLocationID = currentLocationID;
                                    displayNotificationMessage("Found a location " + currentLocationReadableName);
                                }
                            }
                        }
                        break;
				}		
			}
		}
	}
    
}
