package au.org.ala.fielddata.mobile.map;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import au.org.ala.fielddata.mobile.CollectSurveyData;
import au.org.ala.fielddata.mobile.Utils;
import au.org.ala.fielddata.mobile.model.MapDefaults;
import au.org.ala.fielddata.mobile.model.PhotoPoint;
import au.org.ala.fielddata.mobile.model.WayPoint;
import au.org.ala.fielddata.mobile.model.WayPoints;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper;
import au.org.ala.fielddata.mobile.service.StorageManager;

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

/**
 * This activity displays a google map which allows a polygon to be drawn via adding a series
 * of WayPoints in sequential order.
 * It also allows Photopoints to be taken during the process.
 */
@TargetApi(8)
public class WayPointActivity extends SherlockFragmentActivity implements InfoWindowAdapter, OnMarkerDragListener, LocationListener {
	
	/** Bundle keys */
	public static final String WAY_POINTS_KEY = "WAYPOINTS";
	public static final String MAP_DEFAULTS_BUNDLE_KEY = "MapDefaults";
    public static final String ATTRIBUTE_ID_KEY = "PhotoPointAttributeID";
	private static final String PHOTO_URI_KEY = "PhotoURI";
	
	/** Used to identify a request to the Camera when a result is returned */
	public static final int TAKE_PHOTO_REQUEST = 10000;
	
	protected GoogleMap map;
	private WayPoints wayPoints;
    private Polyline polyline;
	private LocationManager locationManager;
	private Uri photoInProgress;
    private Location currentLocation;

    private LocationServiceHelper.LocationServiceConnection locationServiceConnection;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collect_waypoints);
        boolean setZoom = true;
		if (savedInstanceState != null) {
			setZoom = false;
			wayPoints = savedInstanceState.getParcelable(WAY_POINTS_KEY);
			photoInProgress = savedInstanceState.getParcelable(PHOTO_URI_KEY);
			
		} else {
			wayPoints = getIntent().getParcelableExtra(WAY_POINTS_KEY);
		}
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		initialiseMap(setZoom);
		addEventHandlers();
        initHelp();
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_collect_waypoints, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem openPolygon = menu.findItem(R.id.open_polygon);
		MenuItem closePolygon = menu.findItem(R.id.close_polygon);
        MenuItem photoPoints = menu.findItem(R.id.add_photopoint);
	
		boolean closed = wayPoints != null ? wayPoints.isClosed() : false;
		openPolygon.setVisible(closed);
		closePolygon.setVisible(!closed);

        photoPoints.setVisible(wayPoints != null && wayPoints.getPhotoPointAttribute() > 0);
		
		return true;
	}


	@Override
	@TargetApi(8)
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_waypoint) {
            if (currentLocation == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(WayPointActivity.this);
                if (gpsTimeoutCount > 0) {
                    builder.setTitle("No GPS")
                            .setMessage("Unable to acquire a location via GPS.  Please add markers by long pressing on the map.")
                            .setPositiveButton("Ok", null)
                            .show();
                }
                else {
                    builder.setTitle("Waiting for GPS")
                            .setMessage("Unable to add a marker until a GPS lock is obtained.")
                            .setPositiveButton("Ok", null)
                            .show();
                }
            }
            else {
                addVertex(currentLocation);
            }
		}
		else if (item.getItemId() == R.id.add_photopoint) {
			takePhoto();
		}
		else if (item.getItemId() == R.id.close_polygon) {
			wayPoints.setClosed(true);
			drawLine();
			supportInvalidateOptionsMenu();
		}
		else if (item.getItemId() == R.id.open_polygon) {
			wayPoints.setClosed(false);
			drawLine();
			supportInvalidateOptionsMenu();
		}
		
		else if (item.getItemId() == R.id.done) {

            Intent result = getIntent();
            result.putExtra(WAY_POINTS_KEY, wayPoints);
            setResult(RESULT_OK, result);
            finish();

		}
		return true;
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
				this.map.setInfoWindowAdapter(this);
				this.map.setOnMarkerDragListener(this);
				
			}
		}
		
		if (wayPoints != null) {
			
			// There doesn't appear to be any way to get an instance of the
			// polyline after the map is restored from a bundle so I have
			// to clear the map and re-add everything.
			map.clear();
			final LatLngBounds.Builder builder = new LatLngBounds.Builder();
			boolean hasArea = restoreWayPoints(wayPoints.getVerticies(), builder, BitmapDescriptorFactory.HUE_RED);
			restoreWayPoints(wayPoints.getPhotoPoints(), builder, BitmapDescriptorFactory.HUE_BLUE);
			
			drawLine();
			if (hasArea) {
				findViewById(android.R.id.content).post(new Runnable() {
					public void run() {
						map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
					}
				});
			}
            else {
                MapDefaults mapDefaults = getIntent().getParcelableExtra(
                        MAP_DEFAULTS_BUNDLE_KEY);
                if (mapDefaults != null && mapDefaults.center != null) {

                    LatLng latlng = new LatLng(mapDefaults.center.y, mapDefaults.center.x);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, mapDefaults.zoom));
                }
            }

			
		}
	}
	
	private boolean restoreWayPoints(List<? extends WayPoint> wayPoints, LatLngBounds.Builder bounds, float markerColour) {
		LatLng location;
		for (WayPoint wayPoint : wayPoints) {
			location = wayPoint.coordinate();
			bounds.include(location);
			Marker marker = addMarker(location, markerColour);
			wayPoint.markerId = marker.getId();
		}
        return wayPoints.size() > 0;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(WAY_POINTS_KEY, wayPoints);
		outState.putParcelable(PHOTO_URI_KEY, photoInProgress);
	}
	
	
	private View photoPointInfoView;
	
	
	public View getInfoContents(Marker marker) {
		if (photoPointInfoView != null && marker.getId().equals(photoPointInfoView.getTag())) {
			return photoPointInfoView;
		}
		photoPointInfoView = null;
		final Marker tmpMarker = marker;
		PhotoPoint photoPoint = wayPoints.findPhotoPointById(marker.getId());
		if (photoPoint != null && photoPoint.photo != null) {
			photoPointInfoView = getLayoutInflater().inflate(R.layout.waypoint_photo, null);
			photoPointInfoView.setTag(marker.getId());
			ImageView view = (ImageView)photoPointInfoView.findViewById(R.id.photo);
			ImageLoader.getInstance().displayImage(photoPoint.photo.toString(), view, new SimpleImageLoadingListener() {

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					if (tmpMarker.isInfoWindowShown()) {
						tmpMarker.showInfoWindow();
					}
				}
				
			});
				
		}
		return photoPointInfoView;
	}

	public View getInfoWindow(Marker arg0) {
		return null;
	}

	private void addPhotopoint(Location location) {
		if (photoInProgress == null) {
			return;
		}
		Marker marker = addMarker(new LatLng(location.getLatitude(), location.getLongitude()), BitmapDescriptorFactory.HUE_BLUE);
		
		PhotoPoint point = new PhotoPoint(location, marker.getId());
		point.photo = photoInProgress;
		photoInProgress = null;
		
		wayPoints.addPhotoPoint(point);
	}
	
	private void addVertex(Location location) {
		Marker marker = addMarker(new LatLng(location.getLatitude(), location.getLongitude()), BitmapDescriptorFactory.HUE_RED);
		
		WayPoint point = new WayPoint(location, marker.getId());
		wayPoints.addVertex(point);
		drawLine();
		
	}
	
	
	private void drawLine() {
		if (polyline == null) {
			PolylineOptions options = new PolylineOptions()
				.color(Color.RED)
				.width(3)
				.addAll(wayPoints.verticies());
			
			polyline = map.addPolyline(options);
			
		}
		else {
			polyline.setPoints(wayPoints.verticies());
		}
	}
	
	
	private Marker addMarker(LatLng latlng, float colour) {
		MarkerOptions options = new MarkerOptions()
			.position(latlng)
			.draggable(true)
			.icon(BitmapDescriptorFactory.defaultMarker(colour));
			
		return map.addMarker(options);
	}

	@Override
	public void onPause() {

        if (!takingPhotoPoint) {
            stopLocationUpdates();
        }
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

	private void addEventHandlers() {
		map.setOnMapLongClickListener(new OnMapLongClickListener() {
			
			public void onMapLongClick(LatLng location) {
				
				Location selectedLocation = locationFromClick(location);
				
				addVertex(selectedLocation);
                hideHelp();
			}

			
		});
		
		map.setOnMarkerClickListener(new OnMarkerClickListener() {
			public boolean onMarkerClick(Marker marker) {
				// Still want the default behaviour
				return false;
			}
		});

        this.map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                showHelp();
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
		updateLine(marker);
	}
	public void onMarkerDragStart(Marker arg0) {
		
		
	}
	public void onMarkerDragEnd(Marker marker) {
		updateLine(marker);
	}
	
	private void updateLine(Marker marker) {
		WayPoint wayPoint = wayPoints.findById(marker.getId());
        if (wayPoint != null && !(wayPoint instanceof PhotoPoint)) {
            wayPoint.location = locationFromClick(marker.getPosition());

		    drawLine();
        }
	}

	
	private void takePhoto() {
		if (StorageManager.canWriteToExternalStorage()) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			photoInProgress = StorageManager.getOutputMediaFileUri(StorageManager.MEDIA_TYPE_IMAGE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoInProgress);
			takingPhotoPoint = true;
			startActivityForResult(intent, CollectSurveyData.TAKE_PHOTO_REQUEST);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Cannot take photo")
					.setMessage("Please ensure you have mounted your SD card and it is writable")
					.setPositiveButton("OK", null).show();
		}
	}
	
	/**
	 * Callback made to this activity after the camera, gallery or map activity
	 * has finished.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        takingPhotoPoint = false;

        if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK && photoInProgress != null) {
			new PhotoLocationLoader(this, photoInProgress, currentLocation).execute();
		}
	}

    private void initHelp() {
        final View help = findViewById(R.id.help);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            help.setVisibility(View.INVISIBLE);
        }
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideHelp();
            }
        });
    }

    @TargetApi(12)
    private void showHelp() {
        final View help = findViewById(R.id.help);
        if (help.getVisibility() != View.VISIBLE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                help.setY(-help.getHeight());
                help.setVisibility(View.VISIBLE);

                help.animate().y(0);
            }
            else {
                help.setVisibility(View.VISIBLE);
            }

        }
    }

    @TargetApi(12)
    private void hideHelp() {

        View help = findViewById(R.id.help);
        if (help.getVisibility() == View.VISIBLE) {

            // Note we don't set the visibility back to GONE as we only want to show the
            // help once and this saves us having to store state.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                help.animate().y(-help.getHeight());
            }
            else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)help.getLayoutParams();

                params.height = 0;
                help.setLayoutParams(params);
            }
        }
    }
	
	static class PhotoLocationLoader extends AsyncTask<Void, Void, Location> {

		private WayPointActivity context;
		private Uri photo;
		private Location currentLocation;

		public PhotoLocationLoader(WayPointActivity context, Uri photoUri, Location currentLocation) {
			this.context = context;
			this.photo = photoUri;
            this.currentLocation = currentLocation;
		}
		
		@Override
		protected Location doInBackground(Void... params) {
			return readPhotoMetadata(photo);
		}

		@Override
		protected void onPostExecute(Location location) {
			context.addPhotopoint(location);
		}
		
		private Location readPhotoMetadata(Uri photoUri) {
			Location location = currentLocation;

            if (location == null) {
                try {
                    ExifInterface exif = new ExifInterface(photoUri.getPath());
                    float[] latlong= new float[2];
                    boolean hasLatLong = exif.getLatLong(latlong);
                    if (hasLatLong) {
                        location = new Location("EXIF");
                        location.setLatitude(latlong[0]);
                        location.setLongitude(latlong[1]);
                    }
                }
                catch (IOException e) {
                    Log.e("RecordSightingActivity", "Error reading EXIF for file: "+photoUri, e);
                }
            }
			if (location == null) {
				location = context.locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			}
			return location;
		}
		
	}


    // GPS Location Updates
    public void startLocationUpdates() {
        if (locationServiceConnection == null) {
            locationServiceConnection = new LocationServiceHelper.LocationServiceConnection(this, getRequiredLocationAccuracy());
            Intent intent = new Intent(this, LocationServiceHelper.class);
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
        }
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
        }

        timer = scheduler.schedule(new Runnable() {
            public void run() {
                cancelLocationUpdates();
            }
        }, GPS_TIMEOUT, TimeUnit.SECONDS);

    }

    public void cancelLocationUpdates() {
        if (Utils.DEBUG) {
            Log.i("LocationBinder", "Cancelling location updates due to timeout!");
        }
        new Handler(getMainLooper()).post(new Runnable() {
            public void run() {
                gpsTimeoutCount++;
                stopLocationUpdates();

                if (WayPointActivity.this != null && WayPointActivity.this.getWindow() != null && WayPointActivity.this.getWindow().isActive()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WayPointActivity.this);

                    builder.setTitle("GPS timeout")
                            .setMessage("Unable to acquire a location via GPS.  You can use a long press on the map to outline your location manually.")
                            .setPositiveButton("Ok", null)
                            .show();

                }
            }

        });
    }

    private void stopLocationUpdates() {

        if (locationServiceConnection != null) {
            unbindService(locationServiceConnection);
            locationServiceConnection = null;
        }
        if (timer != null) {
            timer.cancel(false);
            timer = null;
        }

    }

    private static final int GPS_TIMEOUT = 60; // seconds

    /** The accuracy required to auto-populate the location from GPS */
    private static final float ACCURACY_THESHOLD = 21f;

    private static final float FALLBACK_ACCURACY_THRESHOLD = 200f;

    private boolean takingPhotoPoint = false;
    private int gpsTimeoutCount;

    private ScheduledFuture<?> timer;
    private ScheduledExecutorService scheduler;


    public float getRequiredLocationAccuracy() {
        return gpsTimeoutCount == 0 ? ACCURACY_THESHOLD : FALLBACK_ACCURACY_THRESHOLD;
    }

    // Location listener implementation.
    public void onLocationChanged(Location location) {
        if (currentLocation == null) {
            Toast.makeText(this, "GPS lock obtained", Toast.LENGTH_SHORT).show();
        }

        currentLocation = location;
        if (timer != null) {
            timer.cancel(true);
            timer = null;
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}


}
