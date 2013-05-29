package au.org.ala.fielddata.mobile.validation;

import java.text.DecimalFormat;

import android.location.Location;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.CollectSurveyData;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;

/**
 * Manages the display of the location information and handles Map and GPS
 * button presses.
 * State information is managed by the CollectSurveyData activity by 
 * necessity (because of the activity lifecycle)
 */
public class LocationBinder extends AbsBinder {

	private TextView latitude;
	private TextView longitude;
	private TextView accuracy;
	private TextView noLocation;
	private TextView time;
	private SurveyViewModel model;
	private CollectSurveyData ctx;
	private Button gpsButton;
	
	public LocationBinder(CollectSurveyData context, View locationView, Attribute locationAttribute, SurveyViewModel model) {
		super(locationAttribute, locationView);
		
		latitude = (TextView)locationView.findViewById(R.id.latitude);
		longitude = (TextView)locationView.findViewById(R.id.longitude);
		accuracy = (TextView)locationView.findViewById(R.id.accuracy);
		noLocation = (TextView)locationView.findViewById(R.id.noLocation);
		time = (TextView)locationView.findViewById(R.id.time);
		
		this.model = model;
		this.ctx = context;
		
		addEventHandlers(locationView);
		updateText();
	}
	
	private void addEventHandlers(View view) {
		gpsButton = (Button)view.findViewById(R.id.gpsButton);
		gpsButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if (!ctx.isGpsTrackingEnabled()) {
					startLocationUpdates();		
				}
				else {
					stopLocationUpdates();
				}
			}
	
			
		});
		
		
		Button showOnMapButton = (Button)view.findViewById(R.id.showMapButton);
		
		String option = getAttribute().getOptionValue(0);
		if (option == null) {
			showOnMapButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					ctx.selectLocation();
				}
			});
		}
		else {
			
			showOnMapButton.setVisibility(View.GONE);
		}
	}
	
	private void startLocationUpdates() {
		ctx.startLocationUpdates();
		updateText();
	}
	
	public void onAttributeChange(Attribute attribute) {
		stopLocationUpdates();
	}

	private void stopLocationUpdates() {
		
		ctx.stopLocationUpdates();
		updateText();
	}

	private void updateText() {
		boolean gpsTrackingOn = ctx.isGpsTrackingEnabled();
		Location location = model.getLocation();
		if (location != null && !gpsTrackingOn) {
			DecimalFormat format = new DecimalFormat("###.000000");
			
			latitude.setText("lat: "+format.format(location.getLatitude()));
			longitude.setText("lon: "+format.format(location.getLongitude()));
			if (location.hasAccuracy()) {
				accuracy.setText("accuracy: "+location.getAccuracy()+ " m");
			}
			else {
				accuracy.setText("accuracy: unknown");
			}	
			
			time.setText("time: "+DateFormat.getTimeFormat(ctx).format(location.getTime()));
			
			noLocation.setVisibility(View.GONE);
			latitude.setVisibility(View.VISIBLE);
			longitude.setVisibility(View.VISIBLE);
			accuracy.setVisibility(View.VISIBLE);
			time.setVisibility(View.VISIBLE);
		}
		else {
			if (gpsTrackingOn) {
				noLocation.setHint("Acquiring location...");
			}
			else {
				noLocation.setHint("No location supplied");
			}
			noLocation.setVisibility(View.VISIBLE);
			latitude.setVisibility(View.GONE);
			longitude.setVisibility(View.GONE);
			accuracy.setVisibility(View.GONE);
			time.setVisibility(View.GONE);
			
		}
		if (gpsTrackingOn) {
			gpsButton.setText("Cancel");
		}
		else {
			gpsButton.setText("Start GPS");
		}
		
	}
	
	/**
	 * This method does nothing, the binding of the location is performed
	 * by the CollectSurveyData activity as it requires the involvement 
	 * of other activities (such as the LocationSelectionActivity).
	 */
	public void bind() {}

}
