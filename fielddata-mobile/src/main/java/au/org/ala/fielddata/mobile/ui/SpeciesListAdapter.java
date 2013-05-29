package au.org.ala.fielddata.mobile.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import au.org.ala.fielddata.mobile.dao.SpeciesDAO;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.nrmplus.R;

/**
 * A list adapter for displaying lists of Species.
 */
public class SpeciesListAdapter extends ArrayAdapter<Species> {
	
	public SpeciesListAdapter(Context ctx) {
		this(ctx, -1);
	}
	
	public SpeciesListAdapter(Context ctx, int excludedGroupId) {
		super(ctx, R.layout.species_row, R.id.scientificName,
				new ArrayList<Species>());
		SpeciesDAO speciesDao = new SpeciesDAO(ctx);
		new GetSpeciesTask(speciesDao, this, excludedGroupId, null).execute();
	}
	
	public SpeciesListAdapter(Context ctx, Integer surveyId) {
		super(ctx, R.layout.species_row, R.id.scientificName,
				new ArrayList<Species>());
		SpeciesDAO speciesDao = new SpeciesDAO(ctx);
		new GetSpeciesTask(speciesDao, this, -1, surveyId).execute();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = super.getView(position, convertView, parent);
		SpeciesViewHolder viewHolder = (SpeciesViewHolder) row.getTag();
		if (viewHolder == null) {
			viewHolder = new SpeciesViewHolder(row);
			row.setTag(viewHolder);
		}

		viewHolder.populate(getItem(position));
		return row;
	}

	static class GetSpeciesTask extends AsyncTask<Void, Void, List<Species>> {

		private SpeciesDAO dao;
		private SpeciesListAdapter adapter;
		private int excludedGroupId;
		private Integer surveyId;

		public GetSpeciesTask(SpeciesDAO speciesDao,
				SpeciesListAdapter adapter, int excludedGroupId, Integer surveyId) {
			this.dao = speciesDao;
			this.adapter = adapter;
			this.excludedGroupId = excludedGroupId;
			this.surveyId = surveyId;
		}

		protected List<Species> doInBackground(Void... ignored) {

			List<Species> species = new ArrayList<Species>();
			try {
				List<Species> allSpecies;
				if (surveyId == null) {
					allSpecies = dao.loadAll(Species.class);
				}
				else {
					allSpecies = dao.speciesForSurvey(surveyId);
				}
				for (Species taxon : allSpecies) {
					if (taxon.getTaxonGroupId()
							!= excludedGroupId) {
						species.add(taxon);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return species;
		}

		protected void onPostExecute(List<Species> speciesList) {
			adapter.setNotifyOnChange(false);
			for (Species species : speciesList) {
				adapter.add(species);
			}
			adapter.notifyDataSetChanged();
		}
	}

}
