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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ScrollView;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.dao.DraftRecordDAO;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.dao.SpeciesDAO;
import au.org.ala.fielddata.mobile.map.PointLocationSelectionActivity;
import au.org.ala.fielddata.mobile.map.WayPointActivity;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.MapDefaults;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;
import au.org.ala.fielddata.mobile.model.SurveyViewModel.TempValue;
import au.org.ala.fielddata.mobile.model.WayPoints;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper.LocationServiceConnection;
import au.org.ala.fielddata.mobile.service.StorageManager;
import au.org.ala.fielddata.mobile.service.UploadService;
import au.org.ala.fielddata.mobile.ui.SpeciesSelectionListener;
import au.org.ala.fielddata.mobile.ui.ValidatingViewPager;
import au.org.ala.fielddata.mobile.validation.Binder;
import au.org.ala.fielddata.mobile.validation.RecordValidator.RecordValidationResult;

import com.actionbarsherlock.app.ActionBar.LayoutParams;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.viewpagerindicator.TitlePageIndicator;

/**
 * The CollectSurveyData activity presents a survey form to the user to fill
 * out.
 */
public class CollectSurveyData extends SherlockFragmentActivity implements
		SpeciesSelectionListener, OnPageChangeListener, LocationListener {

	private static final String GPS_TRACKING_BUNDLE_KEY = "GpsTracking";
	private static final String GPS_TIMEOUT_BUNDLE_KEY = "GpsTimeout";
	
	private static final int GPS_TIMEOUT = 60; // seconds
	
	
	public static final String SURVEY_BUNDLE_KEY = "SurveyIdKey";
	public static final String RECORD_BUNDLE_KEY = "RecordIdKey";
	public static final String SPECIES = "species";
	
	/** The accuracy required to auto-populate the location from GPS */
	private static final float ACCURACY_THESHOLD = 21f;
	
	private static final float FALLBACK_ACCURACY_THRESHOLD = 200f;

	/**
	 * Used to identify a request to the LocationSelectionActivity when a result
	 * is returned
	 */
	public static final int SELECT_LOCATION_REQUEST = 1;

    public static final int SELECT_POLYGON_LOCATION_REQUEST = 2;

	/** Used to identify a request to the Camera when a result is returned */
	public static final int TAKE_PHOTO_REQUEST = 10000;

	/**
	 * Used to identify a request to the Image Gallery when a result is returned
	 */
	public static final int SELECT_FROM_GALLERY_REQUEST = 3;

	private SurveyViewModel surveyViewModel;

	private SurveyPagerAdapter pagerAdapter;
	private ValidatingViewPager pager;
	private Species selectedSpecies;
	private View leftArrow;
	private View rightArrow;
	private Attribute autoScrollAttribute;

	// GPS stuff.
	private LocationServiceConnection locationServiceConnection;
	private ScheduledFuture<?> timer;
	private ScheduledExecutorService scheduler;

	// GPS acquisition state
	private boolean gpsTrackingOn;
	private int gpsTimeoutCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collect_survey_data);

		buildCustomActionBar();
		
		if (savedInstanceState == null) {
			// create and attach our model fragment.
			getSupportFragmentManager().beginTransaction().add(new SurveyModelHolder(), "model")
					.commit();
		}
		else {
			gpsTrackingOn = savedInstanceState.getBoolean(GPS_TRACKING_BUNDLE_KEY);
			gpsTimeoutCount = savedInstanceState.getInt(GPS_TIMEOUT_BUNDLE_KEY);
		}

		pagerAdapter = new SurveyPagerAdapter(getSupportFragmentManager());
		pager = (ValidatingViewPager) findViewById(R.id.surveyPager);
		pager.setAdapter(pagerAdapter);
		leftArrow = findViewById(R.id.leftArrow);
		rightArrow = findViewById(R.id.rightArrow);
		Intent i = getIntent();
		int speciesId = i.getIntExtra(CollectSurveyData.SPECIES, 0);
		if (speciesId > 0) {
			SpeciesDAO speciesDao = new SpeciesDAO(this);
			selectedSpecies = speciesDao.load(Species.class, speciesId);
		}

	}
	
	public void startLocationUpdates() {
		if (locationServiceConnection == null) {
			locationServiceConnection = new LocationServiceConnection(this, getRequiredLocationAccuracy());
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
		gpsTrackingOn = true;
	}
	
	public float getRequiredLocationAccuracy() {
		return gpsTimeoutCount == 0 ? ACCURACY_THESHOLD : FALLBACK_ACCURACY_THRESHOLD; 
	}
	
	public void stopLocationUpdates() {
		stopLocationUpdates(false);
	}
	
	private void stopLocationUpdates(boolean systemChange) {
		
		if (locationServiceConnection != null) {
			unbindService(locationServiceConnection);
			locationServiceConnection = null;
		}
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}
		if (!systemChange) {
			gpsTrackingOn = false;
		}
	}
	
	public void cancelLocationUpdates() {
		if (Utils.DEBUG) {
			Log.i("LocationBinder", "Cancelling location updates due to timeout!");	
		}
		new Handler(getMainLooper()).post(new Runnable() {
			public void run() {		
				gpsTimeoutCount++;
				stopLocationUpdates();
				// We do this to force the LocationBinder to update the display.
				surveyViewModel.setLocation(surveyViewModel.getLocation());
				
				AlertDialog.Builder builder = new AlertDialog.Builder(CollectSurveyData.this);
				if (gpsTimeoutCount == 1) {
				builder.setTitle("GPS timeout")
				       .setMessage("Unable to acquire a location via GPS.  Please try again.")
				       .setPositiveButton("Ok", null)
				       .show();
				}
				else {
					builder.setTitle("GPS timeout")
				       .setMessage("Unable to acquire a location via GPS.  Please edit the location using the web site after saving this record.")
				       .setPositiveButton("Ok", null)
				       .show();
					surveyViewModel.disableLocationValidation();
				}
			}
		});
	}
	
	public void onLocationChanged(Location location) {
		Toast.makeText(this, R.string.locationSelectedByGPS, Toast.LENGTH_SHORT).show();
		surveyViewModel.setLocation(location);
	}
	
	// State management for the GPS acquisition functionality.
	public boolean isGpsTrackingEnabled() {
		return gpsTrackingOn;
	}
	
	public int getGpsTimeoutCount() {
		return gpsTimeoutCount;
	}
	
	public void setGpsTimeoutCount(int gpsTimeoutCount) {
		this.gpsTimeoutCount = gpsTimeoutCount;
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {}

	public void onProviderEnabled(String provider) {}

	public void onProviderDisabled(String provider) {}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (gpsTrackingOn) {
			startLocationUpdates();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (gpsTrackingOn) {
			stopLocationUpdates(true);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(GPS_TRACKING_BUNDLE_KEY, gpsTrackingOn);
		outState.putInt(GPS_TIMEOUT_BUNDLE_KEY, gpsTimeoutCount);
	}

	private void buildCustomActionBar() {
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		View customNav = LayoutInflater.from(this).inflate(R.layout.cancel_done, null);

		customNav.findViewById(R.id.action_done).setOnClickListener(customActionBarListener);
		customNav.findViewById(R.id.action_cancel).setOnClickListener(customActionBarListener);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT, Gravity.FILL_HORIZONTAL);
		getSupportActionBar().setCustomView(customNav, params);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	public void setViewModel(SurveyViewModel model) {
		this.surveyViewModel = model;
		getSupportActionBar().setTitle(Utils.bold(model.getSurvey().name));
		getSupportActionBar().setSubtitle(model.getSurvey().description);

		if (surveyViewModel.getPageCount() > 1) {
			TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
			titleIndicator.setViewPager(pager);
			titleIndicator.setOnPageChangeListener(this);
			titleIndicator.setVisibility(View.VISIBLE);
			rightArrow.setVisibility(View.VISIBLE);
		}
		if (selectedSpecies != null) {
			onSpeciesSelected(selectedSpecies);
		}
	}

	public void onPageSelected(int page) {
		int leftVisiblity = View.VISIBLE;
		int rightVisibility = View.VISIBLE;
		if (page == 0) {
			leftVisiblity = View.GONE;
		}
		int count = pagerAdapter.getCount();
		if (page == count - 1) {
			rightVisibility = View.GONE;
		}

		leftArrow.setVisibility(leftVisiblity);
		rightArrow.setVisibility(rightVisibility);

		if (autoScrollAttribute != null) {
			final Attribute invalid = autoScrollAttribute;
			scrollTo(invalid);
			autoScrollAttribute = null;
		}

	}

	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	public void onPageScrollStateChanged(int arg0) {
	}

	public SurveyViewModel getViewModel() {
		return surveyViewModel;
	}

	public void scrollTo(final Attribute attribute) {
		pager.post(new Runnable() {
			public void run() {

				Binder binder = (Binder) surveyViewModel.getAttributeListener(attribute);
				View boundView = binder.getView();
				ViewParent parent = boundView.getParent();
				while (parent != null && !(parent instanceof ScrollView)) {
					parent = parent.getParent();
				}
				
				if (parent != null) {
					final ScrollView view = (ScrollView) parent;
					final Rect r = new Rect();

					view.offsetDescendantRectToMyCoords((View) boundView.getParent(), r);
					view.post(new Runnable() {
						public void run() {
							view.scrollTo(0, r.bottom);
						}
					});

				}
			}
		});
	}

	public void onSpeciesSelected(Species selectedSpecies) {

		surveyViewModel.speciesSelected(selectedSpecies);
		pager.setCurrentItem(1);
		SpannableString title = new SpannableString(selectedSpecies.scientificName);
		title.setSpan(new StyleSpan(Typeface.ITALIC), 0, title.length(), 0);
		getSupportActionBar().setTitle(title);
		getSupportActionBar().setSubtitle(selectedSpecies.commonName);

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		onActionBarItemSelected(item.getItemId());
		return true;
	}

	public void onActionBarItemSelected(int itemId) {
		if (itemId == R.id.action_done) {
			
			RecordValidationResult result = surveyViewModel.validate();

			if (result.valid()) {	
				Preferences prefs = new Preferences(this);
				boolean attemptUpload = prefs.getUploadAutomatically();
				
				new SaveRecordTask(this, attemptUpload).execute(surveyViewModel.getRecord());
				if (!attemptUpload) {
					Toast.makeText(this, "Record saved.", Toast.LENGTH_LONG).show();
				}
				
				finish();
			} else {
				new SaveRecordTask(this, false).execute(surveyViewModel.getRecord());
				
				Toast.makeText(this, R.string.validationMessage, Toast.LENGTH_LONG).show();
				Attribute firstInvalid = result.invalidAttributes().get(0).getAttribute();
				int firstInvalidPage = surveyViewModel.pageOf(firstInvalid);
				if (pager.getCurrentItem() != firstInvalidPage) {
					pager.setCurrentItem(firstInvalidPage);
				}
				else {
					autoScrollAttribute = firstInvalid;
					scrollTo(autoScrollAttribute);
				}

			}
		} else if (itemId == R.id.action_cancel) {
			finish();
		}
	}
	
	@Override
	public void finish() {
		// Delete our draft.
		DraftRecordDAO recordDAO = new DraftRecordDAO(getApplicationContext());
		recordDAO.deleteAll(Record.class);
		
		super.finish();
	}
	

	class SurveyPagerAdapter extends FragmentPagerAdapter {

		public SurveyPagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int page) {

			SurveyPage surveyPage = new SurveyPage();
			Bundle args = new Bundle();
			args.putInt("pageNum", page);
			surveyPage.setArguments(args);

			return surveyPage;
		}

		@Override
		public int getCount() {
			return surveyViewModel.getPageCount();
		}

		@Override
		public String getPageTitle(int page) {
			return "PAGE " + (page + 1);
		}

	}

	/**
	 * Launches the default camera application to take a photo and store the
	 * result for the supplied attribute.
	 * 
	 * @param attribute
	 *            the attribute the photo relates to.
	 */
	public void takePhoto(Attribute attribute) {
		if (StorageManager.canWriteToExternalStorage()) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri fileUri = StorageManager.getOutputMediaFileUri(StorageManager.MEDIA_TYPE_IMAGE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
			// Unfortunately, this URI isn't being returned in the
			// result as expected so we have to save it somewhere it can
			// survive an activity restart.
			surveyViewModel.setTempValue(attribute, fileUri.toString());
			startActivityForResult(intent, CollectSurveyData.TAKE_PHOTO_REQUEST);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Cannot take photo")
					.setMessage("Please ensure you have mounted your SD card and it is writable")
					.setPositiveButton("OK", null).show();
		}
	}

	/**
	 * Launches the default gallery application to allow the user to select an
	 * image to be attached to the supplied attribute.
	 * 
	 * @param attribute
	 *            the attribute the image is being selected for.
	 */
	public void selectFromGallery(Attribute attribute) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		// Just saving the attribute we are working with.
		surveyViewModel.setTempValue(attribute, "");
		startActivityForResult(Intent.createChooser(intent, "Select Photo"),
				CollectSurveyData.SELECT_FROM_GALLERY_REQUEST);
	}

	public void selectLocation() {

        int checkGooglePlayServices =  GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {

            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, 1122).show();
        }
        else {

            if (surveyViewModel.getSurvey().locationPolygon) {
                selectPointLocation();
            }
            else {
                selectPointLocation();
                //selectPointLocation();
            }
        }
	}

    public void selectPolygonLocation() {

        Intent intent = new Intent(this, WayPointActivity.class);
        Location location = surveyViewModel.getLocation();
        MapDefaults defaults = surveyViewModel.getSurvey().map;

        Attribute photoPointAttribute = getViewModel().getSurvey().getPhotoPointAttribute();
        if (photoPointAttribute != null) {
            intent.putExtra(WayPointActivity.ATTRIBUTE_ID_KEY, photoPointAttribute.getServerId());
        }

        intent.putExtra(PointLocationSelectionActivity.MAP_DEFAULTS_BUNDLE_KEY, defaults);
        if (location != null) {
            intent.putExtra(PointLocationSelectionActivity.LOCATION_BUNDLE_KEY, location);
        }

        startActivityForResult(intent, CollectSurveyData.SELECT_POLYGON_LOCATION_REQUEST);
    }

    public void selectPointLocation() {
        Intent intent = new Intent(this, PointLocationSelectionActivity.class);
        Location location = surveyViewModel.getLocation();
        MapDefaults defaults = surveyViewModel.getSurvey().map;

        intent.putExtra(PointLocationSelectionActivity.MAP_DEFAULTS_BUNDLE_KEY, defaults);
        if (location != null) {
            intent.putExtra(PointLocationSelectionActivity.LOCATION_BUNDLE_KEY, location);

        }
        startActivityForResult(intent, CollectSurveyData.SELECT_LOCATION_REQUEST);
    }

	/**
	 * Callback made to this activity after the camera, gallery or map activity
	 * has finished.
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SELECT_LOCATION_REQUEST) {
			if (resultCode == RESULT_OK) {
				Location location = (Location) data.getExtras().get(
						LocationSelectionActivity.LOCATION_BUNDLE_KEY);
				surveyViewModel.setLocation(location);
			}
		} else if (requestCode == SELECT_POLYGON_LOCATION_REQUEST) {
            if (resultCode == RESULT_OK) {

                WayPoints result = (WayPoints)data.getExtras().get(WayPointActivity.WAY_POINTS_KEY);
                result.setPhotoPointAttribute(data.getIntExtra(WayPointActivity.ATTRIBUTE_ID_KEY, -1));
                surveyViewModel.setWayPoints(result);
            }
        }
        else if (requestCode == TAKE_PHOTO_REQUEST) {
			if (resultCode == RESULT_OK) {
				surveyViewModel.persistTempValue();
			} else {
				surveyViewModel.clearTempValue();
			}
		} else if (requestCode == SELECT_FROM_GALLERY_REQUEST) {
			TempValue value = surveyViewModel.clearTempValue();
			if (resultCode == RESULT_OK) {
				Uri selected = data.getData();
				if (selected != null) {
					surveyViewModel.setValue(value.getAttribute(), selected.toString());
				} else {
					Log.e("CollectSurveyData", "Null data returned from gallery intent!" + data);
				}
			}
		}
	}

	public static class SurveyPage extends Fragment {

		private int pageNum;
		private SurveyViewModel viewModel;
		private BinderManager binder;
		private CollectSurveyData ctx;
		private ScrollView scroller;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			pageNum = getArguments() != null ? getArguments().getInt("pageNum") : 0;

		}

		@Override
		public void onAttach(Activity activity) {

			super.onAttach(activity);
			ctx = (CollectSurveyData) activity;
		}

		@Override
		public void onActivityCreated(Bundle bundle) {
			super.onActivityCreated(bundle);

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			viewModel = ctx.getViewModel();
			binder = new BinderManager(ctx);
			View view = inflater.inflate(R.layout.survey_data_page, container, false);
			view.setTag(binder);
			scroller = (ScrollView) view.findViewById(R.id.tableScroller);
			scroller.setTag(binder);
			
			SurveyBuilder builder = new SurveyBuilder(getActivity(), viewModel, binder);

			builder.buildSurveyForm(view, pageNum);

			return view;
		}

		@Override
		public void onDestroyView() {
			super.onDestroyView();
			binder.clearBindings();
		}
	}

	static class SaveRecordTask extends AsyncTask<Record, Void, Boolean> {

		private CollectSurveyData ctx;
		private boolean upload;

		public SaveRecordTask(CollectSurveyData ctx, boolean upload) {
			this.ctx = ctx;
			this.upload = upload;
		}

		@Override
		protected Boolean doInBackground(Record... params) {
			boolean success = true;
			try {

				RecordDAO recordDao = new RecordDAO(ctx.getApplicationContext());
				recordDao.save(ctx.getViewModel().getRecord());
				
				if (upload) {
					Intent upload = new Intent(ctx, UploadService.class);
					ctx.startService(upload);
				}

			} catch (Exception e) {
				success = false;
				Log.e("SurveyUpload", "Upload failed", e);
			}
			return success;
		}

	}

	private final View.OnClickListener customActionBarListener = new View.OnClickListener() {
		public void onClick(View v) {
			onActionBarItemSelected(v.getId());
		}
	};
}
