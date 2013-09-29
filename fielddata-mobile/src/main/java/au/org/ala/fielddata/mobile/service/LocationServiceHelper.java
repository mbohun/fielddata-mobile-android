package au.org.ala.fielddata.mobile.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

/**
 * The purpose of this class is to allow activities to turn on the system
 * location services (such as GPS) to speed up the location acquisition when
 * filling out the survey form.
 */
public class LocationServiceHelper extends Service implements LocationListener {

	private LocationManager locationManager;
	private final IBinder binder = new LocationServiceBinder();
	private Location bestLocation;
	private LocationListener listener;
	private float locationListenerRequiredAccuracy;
	private boolean hasGps;
	private boolean hasNetwork;
	
	public class LocationServiceBinder extends Binder {
		public LocationServiceHelper getService() {
			return LocationServiceHelper.this;
		}
	}
	
	public static class LocationServiceConnection implements ServiceConnection {
		
		private LocationListener listener;
		private float accuracyThreshold;
		private LocationServiceBinder locationBinder;
		public LocationServiceConnection(LocationListener listener, float accuracyThreshold) {
			this.listener = listener;
			this.accuracyThreshold = accuracyThreshold;
		}
		
		public void onServiceDisconnected(ComponentName name) {
		}
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			locationBinder = (LocationServiceBinder)service;
			locationBinder.getService().registerLocationListener(listener, accuracyThreshold);
		}
		
	};
	
	@Override
	public void onCreate() {
		hasGps = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
		hasNetwork = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
		
		if (hasGps || hasNetwork) {
			locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			requestLocationUpdates(false);
		}
	}
	
	private void requestLocationUpdates(boolean active) {
		if (active) {
			if (hasGps) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TEN_SECONDS, DISTANCE_CHANGE, this);
			}
		}
		else {
			if (hasGps) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TEN_SECONDS, DISTANCE_CHANGE, this);
			}
		}
	}
	
	
	@Override
	public void onDestroy() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		listener = null;
	}
	
	/**
	 * Clients can use this.  Go clients.
	 */
	public Location getCurrentLocation() {
		return bestLocation;
	}
	
	public void registerLocationListener(LocationListener listener, float accuracyThreshold) {
		this.listener = listener;
		locationListenerRequiredAccuracy = accuracyThreshold;
		fireLocationChanged();
	}
	
	
	private void fireLocationChanged() {
		if (listener != null && bestLocation != null && bestLocation.getAccuracy() < locationListenerRequiredAccuracy) {
			listener.onLocationChanged(bestLocation);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// We have an active client, increase the frequency of the updates.
		
		if (hasGps || hasNetwork) {
			requestLocationUpdates(true);
		}
		notify("Location service bound");
		
		return binder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		// We have an active client, increase the frequency of the updates.
		
		requestLocationUpdates(true);
		notify("Location service bound");
		
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		
		super.onUnbind(intent);
		notify("Location service unbound");
		
		requestLocationUpdates(false);
		listener = null;
		return true;
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	public void onProviderEnabled(String provider) {
		
	}

	public void onProviderDisabled(String provider) {
		
	}

	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, bestLocation)) {
			bestLocation = location;
			notify("New best location obtained");
			fireLocationChanged();
		}
	}
	
	private void notify(String message) {
//		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
//		int icon = android.R.drawable.stat_notify_sync;
//		Notification notification = new Notification(icon, message, System.currentTimeMillis());
//		Intent blah = new Intent(this, MobileFieldDataDashboard.class);
//		PendingIntent blah2 = PendingIntent.getActivity(this, 0, blah, 0);
//		notification.setLatestEventInfo(this, message, message, blah2);
//		notificationManager.notify(1, notification);
		
	}

	
	// Makes a call about the quality of location updates.
	// Taken from the Android developers guide.
	private static final int TEN_SECONDS = 1000 * 10;
	private static final int DISTANCE_CHANGE = 5;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TEN_SECONDS;
	    boolean isSignificantlyOlder = timeDelta < -TEN_SECONDS;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return true;
	}

	
	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	
}
