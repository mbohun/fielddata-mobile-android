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

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.model.User;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.pref.EditPreferences;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.FieldDataService;
import au.org.ala.fielddata.mobile.service.FieldDataServiceClient;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper;
import au.org.ala.fielddata.mobile.ui.MenuHelper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * This class is the main activity for the Mobile Field Data application.
 */
public class MobileFieldDataDashboard extends SherlockFragmentActivity implements
		OnSharedPreferenceChangeListener {

	public static final String SELECTED_TAB_BUNDLE_KEY = "tab";
	public static final int RECORDS_TAB_INDEX = 2;
	
	private static final String GPS_QUESTION_BUNDLE_KEY = "gps";
	
	@TargetApi(11)
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}
		else {
			task.execute(params);
		}
	}
	
	protected Dialog splashDialog;
	private Preferences preferences;
	private TextView status;
	private ViewPager viewPager;
	
	/** 
	 * Tracks whether we have asked the user if they want to turn on their
	 * GPS
	 */
	private boolean askedAboutGPS;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Only show the splash screen on startup, and if the variant has been configured to do so.
        if (savedInstanceState == null && getResources().getBoolean(R.bool.show_splash_screen)) {

            showSplashScreen(getResources().getInteger(R.integer.splash_screen_duration));
        }
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_mobile_data_dashboard);
		preferences = new Preferences(this);
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference1, true);
		PreferenceManager
				.setDefaultValues(getApplicationContext(), R.xml.network_preferences, true);
		
		status = (TextView) findViewById(R.id.status);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		String[] titles = getResources().getStringArray(R.array.tab_titles);

		viewPager = (ViewPager) findViewById(R.id.tabContent);
		TabsAdapter tabsAdapter = new TabsAdapter(this, viewPager);
		viewPager.setAdapter(tabsAdapter);

		int selectedTabIndex = 0;
		if (savedInstanceState != null) {
			askedAboutGPS = savedInstanceState.getBoolean(GPS_QUESTION_BUNDLE_KEY);
			selectedTabIndex = savedInstanceState.getInt(SELECTED_TAB_BUNDLE_KEY, 0);
		}
		selectedTabIndex = getIntent().getIntExtra(SELECTED_TAB_BUNDLE_KEY, selectedTabIndex);
		
		for (int i=0; i<titles.length; i++) {
			String title = titles[i];
			ActionBar.Tab tab = getSupportActionBar().newTab();
			tab.setText(title);
			tab.setTabListener(tabsAdapter);
			getSupportActionBar().addTab(tab);
		}
		
		getSupportActionBar().setSelectedNavigationItem(selectedTabIndex);

	}
	
	protected void showSplashScreen(int duration) {

	    splashDialog = new Dialog(this, R.style.SplashScreen) {
	    	@Override
	    	public void onBackPressed() {
	    		removeSplashScreen();
	    		MobileFieldDataDashboard.this.onBackPressed();
	    	}
	    };
	    splashDialog.setContentView(R.layout.splash_screen);
	    splashDialog.setCancelable(false);
	    splashDialog.show();

	    // Set Runnable to remove splash screen
	    final Handler handler = new Handler();
	    handler.postDelayed(new Runnable() {
	      public void run() {
	        removeSplashScreen();
	      }
	    }, duration);
	}
	
	/**
	 * Removes the Dialog that displays the splash screen
	 */
	protected void removeSplashScreen() {
	    if (splashDialog != null) {
	    	splashDialog.dismiss();
	    	splashDialog = null;
	    }
	}

	public static class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener,
			ViewPager.OnPageChangeListener {

		private String[] tabClasses = { SurveyListFragment.class.getName(),
				SpeciesListActivity.class.getName(), ViewSavedRecordsActivity.class.getName() };

		private SherlockFragmentActivity ctx;
		private ViewPager viewPager;

		public TabsAdapter(SherlockFragmentActivity ctx, ViewPager viewPager) {
			super(ctx.getSupportFragmentManager());
			this.ctx = ctx;
			this.viewPager = viewPager;

			viewPager.setOnPageChangeListener(this);
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (viewPager.getCurrentItem() != tab.getPosition()) {
				viewPager.setCurrentItem(tab.getPosition());
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public Fragment getItem(int arg0) {
			return Fragment.instantiate(ctx, tabClasses[arg0]);
		}

		@Override
		public int getCount() {
			return tabClasses.length;
		}

		public void onPageScrollStateChanged(int arg0) {
		}

		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		public void onPageSelected(int arg0) {
			ctx.getSupportActionBar().setSelectedNavigationItem(arg0);

		}

	}

	@Override
	public void onStart() {
		super.onStart();
		if (isLoggedIn()) {
			if (!preferences.getAskedAboutWifi()) {
				showWifiPreferenceDialog();
			}
			
			if (!askedAboutGPS) {
				if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
					LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
						showNoGpsDialog();
					}
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Intent locationIntent = new Intent(MobileFieldDataDashboard.this,
				LocationServiceHelper.class);

		stopService(locationIntent);
	}

	private void showNoGpsDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enable GPS?")
				.setMessage(
						"The GPS on this device is currently disabled, do you want to enable it? \nEnabling GPS will allow accurate survey locations to be recorded.")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
		askedAboutGPS = true;
	}
	
	private void showWifiPreferenceDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Uploading records")
				.setMessage("Do you want to use your phone's data connection to upload records? \nIf you select 'No' saved records will only be uploaded when your phone is connected to a WIFI network.")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						preferences.setUploadOverWifiOnly(false);
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						preferences.setUploadOverWifiOnly(true);
					}
				});
		builder.create().show();
		preferences.setAskedAboutWifi();
	}


	class Model {
		private User user;
		private String portal;

		public Model(User user, String portal) {
			this.user = user;
			this.portal = portal;
		}

		public User getUser() {
			return user;
		}

		public String getPortal() {
			return portal;
		}

	}

	class InitDataTask extends AsyncTask<Void, Void, Model> {

		@Override
		protected Model doInBackground(Void... params) {
			GenericDAO<User> userDAO = new GenericDAO<User>(getApplicationContext());
			List<User> users = userDAO.loadAll(User.class);
			User user = null;
			if (users.size() > 0) {
				user = users.get(0);

			}

			return new Model(user, "");
		}

		@Override
		protected void onPostExecute(Model model) {

			User user = model.getUser();
			if (user != null) {
				getSupportActionBar().setSubtitle(
						Utils.bold("Welcome " + user.firstName + " " + user.lastName));
			}

		}
	}

	class StatusTask extends AsyncTask<Void, Void, AppStatus> {

		@Override
		protected AppStatus doInBackground(Void... params) {
			return checkStatus();
		}

		@Override
		protected void onPostExecute(AppStatus result) {

			if (result.isInitialised()) {
				if (result.isOnline()) {
					status.setText("Online");
				} else {
					status.setText("Offline");
				}
			} else {
				if (!result.isOnline()) {
					showConnectionError();
				} else {
					redirectToLogin();
				}
			}
			setSupportProgressBarIndeterminateVisibility(false);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		refreshPage();

	}

	@Override
	public void onPause() {
		super.onPause();

        removeSplashScreen();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_TAB_BUNDLE_KEY, getSupportActionBar().getSelectedNavigationIndex());
		outState.putBoolean(GPS_QUESTION_BUNDLE_KEY, askedAboutGPS);
	}

	private void refreshPage() {

		// check if the preferences are set if not redirect
		if (preferences.getFieldDataServerHostName().equals("")
				|| preferences.getFieldDataContextName().equals("")) {
			redirectToPreferences();
		} else {
			setSupportProgressBarIndeterminateVisibility(true);

			executeAsyncTask(new InitDataTask());
			executeAsyncTask(new StatusTask());
		}
	}

	class AppStatus {

		private boolean online;
		private boolean initialised;

		public AppStatus(boolean initialised, boolean online) {
			this.online = online;
			this.initialised = initialised;
		}

		public boolean isOnline() {
			return online;
		}

		public boolean isInitialised() {
			return initialised;
		}

	}

	private AppStatus checkStatus() {

		boolean online = canAccessFieldDataServer();
		boolean initialised = isLoggedIn();
		return new AppStatus(initialised, online);
	}

	private void showConnectionError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.initialisationErrorTitle);
		builder.setMessage(String.format(getResources().getString(R.string.initialisationError),
				preferences.getFieldDataServerUrl()));
		builder.setNegativeButton(R.string.close, new Dialog.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
		return;
	}

	private void redirectToPreferences() {
		Intent intent = new Intent(this, EditPreferences.class);
		startActivity(intent);
	}

	private void redirectToLogin() {

		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);

	}

	private boolean isLoggedIn() {

		String sessionKey = preferences.getFieldDataSessionKey();
		return sessionKey != null;
	}

	private boolean canAccessFieldDataServer() {
		boolean success = false;
		String fieldDataServer = preferences.getFieldDataServerHostName();
		try {
			FieldDataServiceClient service = new FieldDataServiceClient(this);
			success = service.ping(5000);
			if (!success) {
				if (Utils.DEBUG) {
					Log.i("Status", "Field data server at: " + fieldDataServer + " is not reachable");
				}
			}
			
		} catch (Exception e) {
			if (Utils.DEBUG) {
				Log.e("Error",
					"Unable to location field data server at: "
							+ preferences.getFieldDataServerHostName(), e);
			}
		}
		return success;
	}

	private MenuItem newRecordMenuItem;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.common_menu_items, menu);
		inflater.inflate(R.menu.dashboard_menu, menu);

		newRecordMenuItem = menu.add("New Record");

		newRecordMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.sync) {
			setSupportProgressBarIndeterminateVisibility(true);

			new AsyncTask<Void, Void, Void>() {

				private boolean success;
				
				@Override
				protected Void doInBackground(Void... params) {
					try {
						new FieldDataService(MobileFieldDataDashboard.this).downloadSurveys(null);
						success = true;
					}
					catch (Exception e) {
						success = false;
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					if (success) {
						Toast.makeText(MobileFieldDataDashboard.this, "Surveys refreshed", Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(MobileFieldDataDashboard.this, "Refresh failed - please check your network", Toast.LENGTH_LONG).show();
					}
					refreshPage();
				}
			}.execute();

			refreshPage();
			return true;
		} else if (item == newRecordMenuItem) {
			Intent intent = new Intent(this, CollectSurveyData.class);
			intent.putExtra(CollectSurveyData.SURVEY_BUNDLE_KEY,
					new Preferences(this).getCurrentSurvey());
			startActivity(intent);
		}
		return new MenuHelper(this).handleMenuItemSelection(item);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("serverHostName") || key.equals("contextName")) {
			preferences.setFieldDataSessionKey(null);
		}
	}

}
