package au.org.ala.fielddata.mobile;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.FieldDataServiceClient;

import com.actionbarsherlock.app.SherlockListFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Fragment that displays a list of Surveys that are available for use
 * by the application.
 */
public class SurveyListFragment extends SherlockListFragment implements Reloadable {
	
	private Preferences preferences;
	private FieldDataServiceClient fieldDataClient;
	
	@Override
	public void onResume() {
		super.onResume();
		preferences = new Preferences(getActivity());
		fieldDataClient = new FieldDataServiceClient(getActivity());
		reload();
	}

    @Override
    public void reload() {
        new InitDataTask().execute();
    }

	private void updateSurveyList(List<Survey> surveys) {
		final Survey[] surveyArray = surveys
				.toArray(new Survey[surveys.size()]);

		// This can be called after the activity has been destroyed.
		if (getActivity() == null) {
			return;
		}
		if (surveys.size() > 0) {
			SurveyListAdapter items = new SurveyListAdapter(getActivity(), surveys);
			setListAdapter(items);
			
			Integer selected = preferences.getCurrentSurvey();
			if (selected == null || selected <= 0) {
				preferences.setCurrentSurvey(surveyArray[0].server_id);
				preferences.setCurrentSurveyName(surveyArray[0].name);
			}
		} else {
			ArrayAdapter<String> items = new ArrayAdapter<String>(
					getActivity(),
					R.layout.sherlock_spinner_item,
					new String[] { "No surveys" });
			setListAdapter(items);
			
		}
	}
	
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Survey survey = (Survey)getListAdapter().getItem(position);
		if (survey != null) {
			preferences.setCurrentSurveyName(survey.name);
			preferences.setCurrentSurvey(survey.server_id);
			((SurveyListAdapter)getListAdapter()).refresh();
			
		}
	}



	class SurveyListAdapter extends ArrayAdapter<Survey> {
		public SurveyListAdapter(Context context, List<Survey> surveys) {
			super(context, R.layout.survey_row, R.id.surveyName, surveys);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Survey survey = getItem(position);
			View row = super.getView(position, convertView, parent);
			TextView name = (TextView)row.findViewById(R.id.surveyName);
			name.setText(survey.name);
			TextView description = (TextView)row.findViewById(R.id.surveyDescription);
			description.setText(survey.description);
			
			ImageView defaultIcon = (ImageView)row.findViewById(R.id.defaulticon);
			if (survey.server_id.equals(preferences.getCurrentSurvey())) {
				defaultIcon.setVisibility(View.VISIBLE);
			}
			else {
				defaultIcon.setVisibility(View.GONE);
			}
			if ((survey.imageUrl != null) && (survey.imageUrl.length() > 0)) {
				ImageView surveyImage = (ImageView)row.findViewById(R.id.surveyImage); 
				fieldDataClient.loadSurveyImage(surveyImage, survey.imageUrl);						
			}
			return row;
			
		}
		
		public void refresh() {
			notifyDataSetChanged();
		}
		
	}
	
	class InitDataTask extends AsyncTask<Void, Void, List<Survey>> {
		
		@Override
		protected List<Survey> doInBackground(Void... params) {
			List<Survey> surveys = new ArrayList<Survey>();
			if (getActivity() != null) {
				GenericDAO<Survey> surveyDAO = new GenericDAO<Survey>(getActivity().getApplicationContext());
				surveys.addAll(surveyDAO.loadAll(Survey.class));
			}
					
			return surveys;
		}
		@Override
		protected void onPostExecute(List<Survey> surveys) {
			
			updateSurveyList(surveys);
			
		}
	}

}
