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
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.model.User;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.UploadService;
import au.org.ala.fielddata.mobile.ui.SavedRecordHolder;
import au.org.ala.fielddata.mobile.ui.SavedRecordHolder.RecordView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Allows the user to view records that have been created but not
 * yet uploaded to the FieldData server.
 */
public class ViewSavedRecordsActivity extends SherlockListFragment implements ActionMode.Callback, OnClickListener {

	private List<Record> records;
	private ActionMode actionMode;
	private List<Survey> surveys;
	private BroadcastReceiver uploadReceiver;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
	

	@Override
	public void onResume() {
		super.onResume();
		
		refresh();
		
		uploadReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				refresh();
			}
		};
	
		IntentFilter filter = new IntentFilter();
		filter.addAction(UploadService.UPLOAD_FAILED);
		filter.addAction(UploadService.UPLOADED);
		filter.addAction(UploadService.STATUS_CHANGE);
		
		
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(uploadReceiver, filter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(uploadReceiver);
		uploadReceiver = null;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.saved_records_layout, menu);
	}

	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.upload) {
			boolean allDrafts = true;
			for (int i=1; i<getListAdapter().getCount(); i++) {
				Record record = ((RecordView)getListAdapter().getItem(i)).record;
				if (record.getStatus() != Record.Status.DRAFT) {
					allDrafts = false;
				}
			}
			if (allDrafts) {
				showDraftsError();
			}
			else {
				Intent intent = new Intent(getActivity(), UploadService.class);
				getActivity().startService(intent);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// The first item is explanatory text so we just ignore clicks on it.
		if (position > 0)  {
			int recordId = records.get(position-1).getId();
			Intent intent = new Intent(getActivity(), CollectSurveyData.class);
			intent.putExtra(CollectSurveyData.RECORD_BUNDLE_KEY, recordId);
			startActivity(intent);
		}
	}

	private void refresh() {
		new GetRecordsTask(getActivity().getApplicationContext()).execute();
	}
	
	class GetRecordsTask extends AsyncTask<Void, Void, List<RecordView>> {

		private Context ctx;
		private int userId = -1;
		
		public GetRecordsTask(Context ctx) {
			this.ctx = ctx;
		}
		protected List<RecordView> doInBackground(Void... ignored) {
			
			RecordDAO dao = new RecordDAO(ctx);

			records = dao.loadAll(Record.class);
			
			GenericDAO<Survey> surveyDao = new GenericDAO<Survey>(ctx);
			surveys = surveyDao.loadAll(Survey.class);
			
			List<RecordView> recordViews = new ArrayList<RecordView>();
			for (Record record : records) {
				
				Survey survey = null;
				for (Survey tmpSurvey : surveys) {
					if (tmpSurvey.server_id.equals(record.survey_id)) {
						survey = tmpSurvey;
						break;
					}
				}
				
				recordViews.add(new RecordView(record, survey));
				
			}
			
			GenericDAO<User> userDao = new GenericDAO<User>(ctx);
			List<User> user = userDao.loadAll(User.class);
			if (user.size() > 0) {
				userId = user.get(0).server_id;
			}
			return recordViews;
		}

		protected void onPostExecute(List<RecordView> records) {
		
			Context context = ViewSavedRecordsActivity.this.getActivity();
			if (context != null) {
				RecordAdapter adapter = new RecordAdapter(ViewSavedRecordsActivity.this, records, userId);
				setListAdapter(adapter);
			}
		}
	}

	public static class RecordAdapter extends ArrayAdapter<RecordView> {
		
		private ViewSavedRecordsActivity fragment;
		private Preferences prefs;
		private int userId;
		
		public RecordAdapter(ViewSavedRecordsActivity fragment, List<RecordView> records, int userId) {
			super(fragment.getActivity(), R.layout.saved_records_layout, R.id.record_description_species);
			this.userId = userId;
			prefs = new Preferences(fragment.getActivity());
			setNotifyOnChange(false);
			add(null);
			
			for (RecordView record : records) {
				add(record);
			}
			notifyDataSetChanged();
			this.fragment = fragment;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == 0) {
				View helpView = convertView;
				if (helpView == null) {
					helpView = createHelpView(parent);
				}
				setHelpText(helpView);
				return helpView;
			}
			else {
				View row = super.getView(position, convertView, parent);
				SavedRecordHolder viewHolder = (SavedRecordHolder) row.getTag();
				if (viewHolder == null) {
					viewHolder = new SavedRecordHolder(row);
					viewHolder.checkbox.setOnClickListener(fragment);
					row.setTag(viewHolder);
					
				}
				boolean checked = ((ListView)parent).getCheckedItemPositions().get(position);
				viewHolder.checkbox.setChecked(checked);
				viewHolder.checkbox.setTag(position);
				viewHolder.populate(getItem(position));
				return row;
			}
		}
		
		private View createHelpView(ViewGroup parent) {
			LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View helpView = layoutInflater.inflate(R.layout.saved_records_help, null);
			
			
			return helpView;
		}
		
		private void setHelpText(View helpView) {
			TextView helpTextView = (TextView)helpView.findViewById(R.id.savedRecordsHelp);
			
			Spannable helpText;
			if (prefs.getUploadAutomatically()) {
				if (prefs.getUploadOverWifiOnly()) {
					helpText = buildHelpText("Records will be uploaded automatically then deleted when the phone is connected to a WIFI network.\n" +
							"Uploaded records can be edited using the ");
				}
				else {
					helpText = buildHelpText("Records will be uploaded automatically then deleted when the phone is connected to a data network.\n" +
							"Uploaded records can be can be edited using the ");
				}
			}
			else {
				helpText = buildHelpText("Records can be uploaded to the server using the upload icon in the action bar.\n" +
						"Records are deleted after being uploaded, however they can be edited using the ");
			}
			helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
			helpTextView.setText(helpText);			
		}
		
		private Spannable buildHelpText(String prefix) {
			
			String suffix = prefs.getFieldDataPortalName()+" web site.";
			Spannable helpText = new SpannableString(prefix+suffix);
			helpText.setSpan(new URLSpan(String.format(prefs.getReviewUrl(), userId)), prefix.length(), helpText.length(), 0);
			
			return helpText;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return position == 0 ? 0:1;
		}
	}
	
	public void onClick(View view) {
		CheckBox checkBox = (CheckBox)view;
		getListView().setItemChecked((Integer)view.getTag(), checkBox.isChecked());
		
		int count = countSelected();
		if (count > 0) {
			if (actionMode == null) {
				getSherlockActivity().startActionMode(this);
			}
			else {
				actionMode.setTitle(count+" selected");
			}
		}
		else {
			finishActionMode();
		}
		
	}
	
	private void finishActionMode() {
		if (actionMode != null) {
			actionMode.finish();
			actionMode = null;
		}
	}
	
	private int countSelected() {
		
		int count = 0;
		SparseBooleanArray selected = getListView().getCheckedItemPositions();
		for (int i=0; i<selected.size(); i++) {
			// Ignore the first item as it is help text.
			if (selected.keyAt(i) > 0) {
				if (selected.valueAt(i)) {
					count++;
				}
			}
		}
		return count;
	}
	
	
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		this.actionMode = mode;
		mode.setTitle(countSelected() + " selected");
		
		menu.add("Delete").setIcon(android.R.drawable.ic_menu_delete);
		menu.add("Upload").setIcon(android.R.drawable.ic_menu_upload);
		
		return true;
	}

	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		
		return false;
	}

	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if ("Delete".equals(item.getTitle())) {
			deleteSelectedRecords();
		}
		else if ("Upload".equals(item.getTitle())) {
			uploadSelectedRecords();
		}
		return true;
	}

	public void onDestroyActionMode(ActionMode mode) {
		
		for (int i=0; i<getListAdapter().getCount(); i++) {
			getListView().setItemChecked(i, false);
		}
	}
	
	
	private void deleteSelectedRecords() {
		RecordDAO recordDao = new RecordDAO(getActivity());
		SparseBooleanArray selected = getListView().getCheckedItemPositions();
		int deleteCount = 0;
		for (int i=0; i<selected.size(); i++) {
			// Ignore the first item as it is help text.
			if (selected.keyAt(i) > 0) {
				
				if (selected.valueAt(i) == true) {
					Record record = ((RecordView)getListAdapter().getItem(selected.keyAt(i))).record;
					recordDao.delete(Record.class, record.getId());
					deleteCount++;
				}
			}
		}
		if (deleteCount > 0) {
			String message = "%d records deleted";
			if (deleteCount == 1) {
				message = "%d record deleted";
			}
			Toast.makeText(getActivity(), String.format(message, deleteCount), Toast.LENGTH_SHORT).show();	
				
		}
		
		finishActionMode();
		
		refresh();
		
	}
	
	private void uploadSelectedRecords() {
		
		boolean allDrafts = true;
		SparseBooleanArray selected = getListView().getCheckedItemPositions();
		int count = countSelected();
		int index = 0;
		int[] recordIds = new int[count];
		for (int i=0; i<selected.size(); i++) {
			// Ignore the first item as it is help text.
			if (selected.keyAt(i) > 0) {
				if (selected.valueAt(i) == true) {
					Record record = ((RecordView)getListAdapter().getItem(selected.keyAt(i))).record;
					if (record.getStatus() != Record.Status.DRAFT) {
						allDrafts = false;
					}
					recordIds[index++] = record.getId();
				}
			}
		}
		
		if (allDrafts) {
			showDraftsError();
		}
		else {
			Intent intent = new Intent(getActivity(), UploadService.class);
			intent.putExtra(UploadService.RECORD_IDS_EXTRA, recordIds);
			getActivity().startService(intent);
		}
		finishActionMode();
	}
	
	private void showDraftsError() {
		Toast.makeText(getActivity(), "Draft records cannot be uploaded.", Toast.LENGTH_LONG).show();
	}


	/**
	 * Because this fragment is managed by a ViewPager, the lifecycle
	 * callbacks aren't a reliable indication of the fragment being visible.
	 * 
	 * This callback though seems to do the trick, we can use it to dismiss
	 * the action mode if this view is paged away.
	 */
	@Override
	public void setMenuVisibility(boolean menuVisible) {
		
		super.setMenuVisibility(menuVisible);
		if (!menuVisible) {
			finishActionMode();
		}
	}
	
	
	
	
}
