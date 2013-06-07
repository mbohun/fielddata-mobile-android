package au.org.ala.fielddata.mobile.map;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

import java.io.IOException;
import java.util.List;

import au.org.ala.fielddata.mobile.CollectSurveyData;
import au.org.ala.fielddata.mobile.model.MapDefaults;
import au.org.ala.fielddata.mobile.model.WayPoint;
import au.org.ala.fielddata.mobile.model.WayPoints;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.service.StorageManager;

/**
 * This activity displays a google map which allows the user to select a single location for
 * a survey.
 */
@TargetApi(8)
public class PointLocationSelectionActivity extends SherlockFragmentActivity implements  OnMarkerDragListener {
	
	/** Bundle keys */
    public static final String LOCATION_BUNDLE_KEY = "Location";
    public static final String MAP_DEFAULTS_BUNDLE_KEY = "MapDefaults";


	protected GoogleMap map;
	private LocationManager locationManager;
    private Location selectedLocation;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collect_waypoints);
		boolean setZoom = true;
		if (savedInstanceState != null) {
			setZoom = false;
			selectedLocation = savedInstanceState.getParcelable(LOCATION_BUNDLE_KEY);
		}
        else {
            selectedLocation = getIntent().getParcelableExtra(LOCATION_BUNDLE_KEY);
        }
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		initialiseMap(setZoom);
		addEventHandlers();

	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_collect_waypoints, menu);
        menu.removeItem(R.id.add_photopoint);
        menu.removeItem(R.id.open_polygon);
        menu.removeItem(R.id.close_polygon);

        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.done) {

            Intent result = getIntent();
            result.putExtra(LOCATION_BUNDLE_KEY, selectedLocation);
            setResult(RESULT_OK, result);
            finish();

		}
        else if (item.getItemId() == R.id.add_waypoint) {
            LatLng newLatLng  = setLocation(locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
            map.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));

        }
		return true;
	}


    private LatLng setLocation(Location location) {
        selectedLocation = location;
        LatLng selectedLatLng = new LatLng(selectedLocation.getLatitude(), selectedLocation.getLongitude());
        addMarker(selectedLatLng, BitmapDescriptorFactory.HUE_RED);
        return selectedLatLng;
    }

	/**
	 * Adds the overlay, and the previously selected point. Sets the map zoom
	 * and centre.
	 */
	private void initialiseMap(boolean setZoom) {
		
		if (this.map == null) {
			SupportMapFragment mf = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map));
			// Check if we were successful in obtaining the map.
			if (mf != null) {
				mf.setRetainInstance(true);
				
				this.map = mf.getMap();
				this.map.setMyLocationEnabled(true);
				this.map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				this.map.setOnMarkerDragListener(this);
				
			}
		}
		
		if (selectedLocation != null) {

            map.clear();

            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(setLocation(selectedLocation));

			if (setZoom) {
				findViewById(android.R.id.content).post(new Runnable() {
					public void run() {
						map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
					}
				});
			}
			
		} else {
            if (setZoom) {
				MapDefaults mapDefaults = getIntent().getParcelableExtra(
						MAP_DEFAULTS_BUNDLE_KEY);
				if (mapDefaults != null && mapDefaults.center != null) {
					
					LatLng latlng = new LatLng(mapDefaults.center.y, mapDefaults.center.x);
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, mapDefaults.zoom));
				}
			}
		}

	}
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(LOCATION_BUNDLE_KEY, selectedLocation);
	}


	private Marker addMarker(LatLng latlng, float colour) {
		MarkerOptions options = new MarkerOptions()
			.position(latlng)
			.draggable(true)
            .title("Long press and drag to move marker")
			.icon(BitmapDescriptorFactory.defaultMarker(colour));
			
		return map.addMarker(options);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	private void addEventHandlers() {
		map.setOnMapLongClickListener(new OnMapLongClickListener() {
			
			public void onMapLongClick(LatLng location) {
				
				setLocation(locationFromClick(location));
			}

			
		});
		

	}

	private Location locationFromClick(LatLng arg0) {
		Location selectedLocation = new Location("On-screen map");
		selectedLocation.setTime(System.currentTimeMillis());
		selectedLocation.setLatitude(arg0.latitude);
		selectedLocation.setLongitude(arg0.longitude);
		return selectedLocation;
	}
	
	public void onMarkerDrag(Marker marker) {

	}
	public void onMarkerDragStart(Marker arg0) {
		
		
	}
	public void onMarkerDragEnd(Marker marker) {
		selectedLocation = locationFromClick(marker.getPosition());
	}
	
}
