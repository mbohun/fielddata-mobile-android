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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.User;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.pref.EditPreferences;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.FieldDataService;
import au.org.ala.fielddata.mobile.service.FieldDataServiceClient;
import au.org.ala.fielddata.mobile.service.LocationServiceHelper;
import au.org.ala.fielddata.mobile.service.SurveyDownloadService;
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
    private static final String REDIRECTED_TO_LOGIN_BUNDLE_KEY = "loginRedirect";

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
    private BroadcastReceiver broadcastReceiver;
    private boolean redirectedToLogin;



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
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        preferences = new Preferences(this);
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference1, true);
		PreferenceManager
				.setDefaultValues(getApplicationContext(), R.xml.network_preferences, true);
		
		status = (TextView) findViewById(R.id.status);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		String[] titles = getResources().getStringArray(R.array.tab_titles);

        final boolean noSpecies = getResources().getBoolean(R.bool.no_species);
        final String[] tabs = noSpecies?
                new String[]{ SurveyListFragment.class.getName(), ViewSavedRecordsActivity.class.getName() }
                :
                new String[]{ SurveyListFragment.class.getName(),
                SpeciesListActivity.class.getName(), ViewSavedRecordsActivity.class.getName() };

		viewPager = (ViewPager) findViewById(R.id.tabContent);
		TabsAdapter tabsAdapter = new TabsAdapter(this, viewPager, tabs);
		viewPager.setAdapter(tabsAdapter);

		int selectedTabIndex = 0;
		if (savedInstanceState != null) {
			askedAboutGPS = savedInstanceState.getBoolean(GPS_QUESTION_BUNDLE_KEY);
            redirectedToLogin = savedInstanceState.getBoolean(REDIRECTED_TO_LOGIN_BUNDLE_KEY);
			selectedTabIndex = savedInstanceState.getInt(SELECTED_TAB_BUNDLE_KEY, 0);
		}
		selectedTabIndex = getIntent().getIntExtra(SELECTED_TAB_BUNDLE_KEY, selectedTabIndex);

        for (int i=0; i<titles.length; i++) {
            if (noSpecies && i == 1) continue;
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

        private String[] tabClasses;
        private boolean[] needsReload;

		private SherlockFragmentActivity ctx;
		private ViewPager viewPager;
        private Fragment current;

		public TabsAdapter(SherlockFragmentActivity ctx, ViewPager viewPager, String[] tabs) {
			super(ctx.getSupportFragmentManager());
			this.ctx = ctx;
			this.viewPager = viewPager;
            tabClasses = tabs;
            needsReload = new boolean[tabs.length];
            Arrays.fill(needsReload, false);
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
        public Object instantiateItem(ViewGroup container, int position) {
            Object f = super.instantiateItem(container, position);
            current = (Fragment)f;
            if (needsReload[position]) {
                reload(current, position);
            }
            return f;
        }

        public void reload() {
            Arrays.fill(needsReload, true);
            reload(current, viewPager.getCurrentItem());
        }

        private void reload(Fragment f, int position) {
            if (f instanceof Reloadable) {
                ((Reloadable)current).reload();
                needsReload[position] = false;
            }
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
                final String uponLogin = getResources().getString(R.string.upon_login);
                if (uponLogin.length() == 0) {
                    showWifiPreferenceDialog();
                } else {
                    new AlertDialog.Builder(this)
                    .setTitle("Please Note:")
                    .setMessage(uponLogin)
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            showWifiPreferenceDialog();
                        }
                    })
                    .create().show();
                }
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
        if (SurveyDownloadService.isDownloading()) {
            listenForSurveyDownload();
        }
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
        refreshPage();
        // If the user has changed their login, we need to reload surveys/species from
        // the database.
        if (redirectedToLogin) {
            redirectedToLogin = false;
            reloadTabs();

        }
	}

	@Override
	public void onPause() {
		super.onPause();

        stopListeningForSurveyDownload();
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
        outState.putBoolean(REDIRECTED_TO_LOGIN_BUNDLE_KEY, redirectedToLogin);
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

        redirectedToLogin = true;
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

        if (viewPager.getCurrentItem() == 0) {
            newRecordMenuItem = menu.add(R.string.new_record_description);

            newRecordMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.sync) {

            RecordDAO recordDAO = new RecordDAO(this);
            if (recordDAO.count(Record.class) > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Unable to reload surveys")
                        .setMessage("Please upload your saved Records and try again.")
                        .setPositiveButton("OK", null)
                        .show();
            }
            else {
                setSupportProgressBarIndeterminateVisibility(true);

                Intent downloadSurveys = new Intent(MobileFieldDataDashboard.this, SurveyDownloadService.class);
                listenForSurveyDownload();
                startService(downloadSurveys);
            }
			return true;
		} else if (item == newRecordMenuItem) {
			Intent intent = new Intent(this, CollectSurveyData.class);
			intent.putExtra(CollectSurveyData.SURVEY_BUNDLE_KEY,
					new Preferences(this).getCurrentSurvey());
			startActivity(intent);
		}
        else if (item.getItemId() == R.id.login_screen) {
            redirectToLogin();
        }
        else {
		    return new MenuHelper(this).handleMenuItemSelection(item);
        }
        return true;
	}

    private void listenForSurveyDownload() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(SurveyDownloadService.RESULT_EXTRA, false);
                if (success) {
                    Toast.makeText(MobileFieldDataDashboard.this, "Surveys refreshed", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MobileFieldDataDashboard.this, "Refresh failed - please check your network", Toast.LENGTH_LONG).show();
                }
                reloadTabs();
                setSupportProgressBarIndeterminateVisibility(false);
                stopListeningForSurveyDownload();
            }
        };
        IntentFilter downloadFilter = new IntentFilter(SurveyDownloadService.FINISHED_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, downloadFilter);
    }

    private void stopListeningForSurveyDownload() {
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("serverHostName") || key.equals("contextName")) {
			preferences.setFieldDataSessionKey(null);
		}
	}

    private void reloadTabs() {
        ((TabsAdapter)viewPager.getAdapter()).reload();

    }

}
