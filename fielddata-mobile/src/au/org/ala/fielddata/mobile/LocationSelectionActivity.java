/*******************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *  
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *  
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ******************************************************************************/
package au.org.ala.fielddata.mobile;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.map.AreaOverlay;
import au.org.ala.fielddata.mobile.model.MapDefaults;
import au.org.ala.fielddata.mobile.ui.MenuHelper;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

/**
 * Displays a map that allows the user to select the location of their 
 * observation.
 */
public class LocationSelectionActivity extends SherlockMapActivity implements
		LocationListener {

	public static final String LOCATION_BUNDLE_KEY = "Location";
	public static final String MAP_DEFAULTS_BUNDLE_KEY = "MapDefaults";
	private MapView mapView;
	private AreaOverlay selectionOverlay;
	private Location selectedLocation;
	private MyLocationOverlay myLocationOverlay;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_selection);
		boolean setZoom = true;
		Location location = null;
		if (savedInstanceState != null) {
			setZoom = false;
			location = (Location)savedInstanceState.getParcelable(LOCATION_BUNDLE_KEY);
		}
		else {
		    location = (Location)getIntent().getParcelableExtra(LOCATION_BUNDLE_KEY);
		}
		mapView = (MapView) findViewById(R.id.mapview);
		initialiseOverlays();
		initialiseMap(location, setZoom);
		addEventHandlers();

	}
	
	private void initialiseOverlays() {
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		
		mapView.getOverlays().add(myLocationOverlay);
		Drawable marker = getResources().getDrawable(R.drawable.iconr);

		marker.setBounds(0, 0, marker.getIntrinsicWidth(),
				marker.getIntrinsicHeight());

		ImageView dragImage = (ImageView) findViewById(R.id.drag);
		selectionOverlay = new AreaOverlay(mapView, marker, dragImage, this);
		
		mapView.getOverlays().add(selectionOverlay);
	}
	
	/**
	 * Adds the overlay, and the previously selected point.
	 * Sets the map zoom and centre.
	 */
	private void initialiseMap(Location location, boolean setZoom) {
		mapView.setBuiltInZoomControls(true);
		
		
		if (location != null) {
			selectionOverlay.selectLocation(location);
			if (setZoom) {
				mapView.getController().setZoom(16);
				mapView.getController().setCenter(selectionOverlay.getItem(0).getPoint());
			}
		}
		else {
			if (setZoom) {
				MapDefaults mapDefaults = (MapDefaults)getIntent().getParcelableExtra(MAP_DEFAULTS_BUNDLE_KEY);
				if (mapDefaults != null && mapDefaults.center != null) {
					mapView.getController().setZoom(mapDefaults.zoom);
					
					mapView.getController().setCenter(
							new GeoPoint((int)(mapDefaults.center.y*1000000), (int)(mapDefaults.center.x*1000000)));
				}
			}
		}
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		new MenuInflater(this).inflate(R.menu.common_menu_items, menu);

		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(LOCATION_BUNDLE_KEY, selectedLocation);
		
	}

	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return new MenuHelper(this).handleMenuItemSelection(item);
	}

	
	private void addEventHandlers() {
		Button button = (Button) findViewById(R.id.mapNext);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent result = new Intent();
				result.putExtra(LOCATION_BUNDLE_KEY, selectedLocation);
				setResult(RESULT_OK, result);
				finish();

			}
		});
		button.setEnabled(selectedLocation !=  null);

		ImageButton gps = (ImageButton) findViewById(R.id.mapCurrentLocation);
		gps.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				updateLocation();
			}
		});
	}

	@Override
	protected boolean isRouteDisplayed() {

		return false;
	}

	public void onLocationChanged(Location location) {
		selectedLocation = location;
		Button button = (Button) findViewById(R.id.mapNext);
		button.setEnabled(location != null);

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {}

	public void onProviderEnabled(String provider) {}

	public void onProviderDisabled(String provider) {}

	private void updateLocation() {
		
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {

			public void run() {
				final GeoPoint point = myLocationOverlay.getMyLocation();
				if (point != null) {
					mapView.getController().setCenter(point);
					mapView.getController().setZoom(18);
					
					
					runOnUiThread(new Runnable() {
						public void run() {
							selectionOverlay.selectLocation(point);
						}
					});

				}
			}
		});

	}

}
