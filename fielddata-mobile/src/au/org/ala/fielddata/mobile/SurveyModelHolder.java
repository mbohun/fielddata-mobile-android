package au.org.ala.fielddata.mobile;

import android.os.Bundle;
import au.org.ala.fielddata.mobile.dao.DraftRecordDAO;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.dao.SpeciesDAO;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;
import au.org.ala.fielddata.mobile.model.SurveyViewModel.TempValue;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Makes use of the behaviour that fragments can survive the re-creation of
 * their containing activity to keep a reference to the view model alive during
 * configuration changes.
 */
public class SurveyModelHolder extends SherlockFragment {

	private SurveyViewModel model;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int surveyId = getActivity().getIntent()
				.getIntExtra(CollectSurveyData.SURVEY_BUNDLE_KEY, 0);
		int recordId = getActivity().getIntent()
				.getIntExtra(CollectSurveyData.RECORD_BUNDLE_KEY, 0);

		if (savedInstanceState != null) {
			surveyId = savedInstanceState.getInt(CollectSurveyData.SURVEY_BUNDLE_KEY, surveyId);
			recordId = savedInstanceState.getInt(CollectSurveyData.RECORD_BUNDLE_KEY, recordId);
		}

		setRetainInstance(true);
		updateModel(surveyId, recordId, savedInstanceState != null);

		// Now restore the default value if required
		if (savedInstanceState != null) {

			int attributeId = savedInstanceState.getInt("TempAttribute", -1);
			if (attributeId > 0) {
				Attribute attr = model.getSurvey().getAttribute(attributeId);
				String value = savedInstanceState.getString("TempAttributeValue");

				model.setTempValue(attr, value);
			}
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		DraftRecordDAO recordDao = new DraftRecordDAO(getActivity());
		int draftId = recordDao.save(model.getRecord());

		outState.putInt(CollectSurveyData.SURVEY_BUNDLE_KEY, model.getSurvey().server_id);
		outState.putInt(CollectSurveyData.RECORD_BUNDLE_KEY, draftId);

		TempValue toSave = model.getTempValue();
		if (toSave != null) {
			outState.putInt("TempAttribute", toSave.getAttribute().server_id);
			outState.putString("TempAttributeValue", toSave.getValue());
		}
	}

	private synchronized void updateModel(int surveyId, int recordId, boolean updateFromDraft) {
		if (model == null) {
			Record record = null;
			Survey survey;
			if (recordId > 0) {
				record = initRecord(recordId, surveyId, updateFromDraft);
				surveyId = record.survey_id;

			}
			try {
				survey = initSurvey(surveyId);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (recordId <= 0) {
				record = initRecord(recordId, surveyId, updateFromDraft);
			}

			model = new SurveyViewModel(survey, record, getActivity().getPackageManager());
			if (record.taxon_id != null && record.taxon_id > 0) {
				Species species = new SpeciesDAO(getActivity()).findByServerId(Species.class,
						record.taxon_id);
				model.speciesSelected(species);
			}

		}
		((CollectSurveyData) getActivity()).setViewModel(model);
	}

	private Survey initSurvey(int surveyId) throws Exception {
		GenericDAO<Survey> surveyDAO = new GenericDAO<Survey>(getActivity().getApplicationContext());
		return surveyDAO.findByServerId(Survey.class, surveyId);
	}

	private Record initRecord(int recordId, int surveyId, boolean draft) {
		Record record;
		if (recordId <= 0) {
			record = new Record();
			record.survey_id = surveyId;
			record.when = System.currentTimeMillis();

			SpeciesDAO speciesDao = new SpeciesDAO(getActivity());
			if (speciesDao.count(Species.class) == 1) {
				Species species = speciesDao.loadAll(Species.class).get(0);
				record.taxon_id = species.server_id;
				record.scientificName = species.scientificName;
			}

		} else {
			RecordDAO recordDAO;
			if (draft) {
				recordDAO = new DraftRecordDAO(getActivity().getApplicationContext());
			} else {
				recordDAO = new RecordDAO(getActivity().getApplicationContext());
			}
			record = recordDAO.load(Record.class, recordId);

		}
		return record;
	}

}
