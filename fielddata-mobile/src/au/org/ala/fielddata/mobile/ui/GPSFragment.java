package au.org.ala.fielddata.mobile.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import au.org.ala.fielddata.mobile.CollectSurveyData;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper.LocationServiceConnection;

public class GPSFragment extends DialogFragment implements LocationListener, DialogInterface.OnClickListener {

	private EditText latitude;
	private EditText longitude;
	private EditText accuracy;
	
	private Location bestLocation;
	
	private LocationServiceConnection locationService;
	
	
	@TargetApi(8)
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		View view = getActivity().getLayoutInflater().inflate(R.layout.location_layout, null);
		latitude = (EditText)view.findViewById(R.id.latitude);
		longitude = (EditText)view.findViewById(R.id.longitude);
		accuracy = (EditText)view.findViewById(R.id.accuracy);
		
		builder.setView(view);
		builder.setPositiveButton("Accept", this);
		builder.setNegativeButton("Cancel", null);
		
		AlertDialog dialog = builder.create();
		
		// For 2.1 (which is the minimum API level we support), the
		// "Accept" button will not be disabled before the first update
		// from the location manager. It just means we handle the null case
		// in the accept button listener.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
	        dialog.setOnShowListener(new OnShowListener() {
			
			public void onShow(DialogInterface dialog) {
				((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				
			}
		});
		}
		
		return dialog;
	}
	
	private void startLocationUpdates() {
		locationService = new LocationServiceConnection(this, 1000f);
		Intent intent = new Intent(getActivity(), LocationServiceHelper.class);
		getActivity().bindService(intent, locationService, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdates();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unbindService(locationService);
	}

	public void onLocationChanged(Location location) {
		if (bestLocation == null) {
			bestLocation = location;
			
			((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
			
		}
		else if (location.getAccuracy() < bestLocation.getAccuracy()) {
			bestLocation = location;
		}
	    latitude.setText(Double.toString(bestLocation.getLatitude()));
	    longitude.setText(Double.toString(bestLocation.getLongitude()));
	    accuracy.setText(Double.toString(bestLocation.getAccuracy()));
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {}

	public void onProviderEnabled(String provider) {}

	public void onProviderDisabled(String provider) {}

	public void onClick(DialogInterface dialog, int which) {
		if (bestLocation != null) {
			CollectSurveyData activity = (CollectSurveyData)getActivity();
			activity.getViewModel().setLocation(bestLocation);
		}
		
	}

}
