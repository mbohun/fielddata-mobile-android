package au.org.ala.fielddata.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.UploadService;
import au.org.ala.fielddata.mobile.ui.NumberedImageButton;

/**
 * Controller for the action bar displayed at the bottom of most activities
 * in the application.
 */
public class ActionBarFragment extends Fragment implements OnClickListener {

	private NumberedImageButton savedRecords;
	
	private BroadcastReceiver broadcastReceiver;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	
		View actions = inflater.inflate(R.layout.action_bar, container);
		addEventHandlers(actions);
		setRetainInstance(true);
		
		return actions;
	}
	
	
	/**
	 * Counts the number of saved records and updates the display on the saved 
	 * records button.	
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		refresh();	
		addBroadcastListener();
	}
	
	private void refresh() {
		new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				RecordDAO recordDAO = new RecordDAO(getActivity());
				return recordDAO.count(Record.class);
			}

			@Override
			protected void onPostExecute(Integer numSavedRecords) {
				savedRecords.setNumber(numSavedRecords > 0 ? numSavedRecords : null);
			}
			
		}.execute();

	}
	
	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
			
	}
	
	private void addEventHandlers(View actionBar) {
		savedRecords = (NumberedImageButton)actionBar.findViewById(R.id.viewSavedRecordsButton);
		savedRecords.setOnClickListener(this);
		ImageButton button = (ImageButton)actionBar.findViewById(R.id.newRecordButton);
		button.setOnClickListener(this);
		button = (ImageButton)actionBar.findViewById(R.id.viewSpeciesListButton);
        if (getResources().getBoolean(R.bool.no_species))
            button.setVisibility(View.GONE);
        else
    		button.setOnClickListener(this);
	}

	/**
	 * Handles selection of the buttons in the action bar.
	 */
	public void onClick(View v) {
		if (v.getId() == R.id.viewSavedRecordsButton) {
			Intent intent = new Intent(getActivity(), ViewSavedRecordsActivity.class);
			startActivity(intent);
		} else if (v.getId() == R.id.newRecordButton) {
			Intent intent = new Intent(getActivity(), CollectSurveyData.class);
			intent.putExtra(CollectSurveyData.SURVEY_BUNDLE_KEY, new Preferences(getActivity()).getCurrentSurvey());
			startActivity(intent);
		} else if (v.getId() == R.id.viewSpeciesListButton) {
			Intent intent = new Intent(getActivity(), SpeciesListActivity.class);
			startActivity(intent);
		}

	}

	private void addBroadcastListener() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(UploadService.UPLOAD_FAILED);
		filter.addAction(UploadService.UPLOADED);
		
		broadcastReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (UploadService.UPLOAD_FAILED.equals(intent.getAction())) {
					Toast.makeText(getActivity(), "Upload failed!", Toast.LENGTH_SHORT).show();
				}
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, filter);
	}
	

}
