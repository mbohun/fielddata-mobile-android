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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SearchViewCompat;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import au.org.ala.fielddata.mobile.dao.SurveyDAO;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.service.SurveyDownloadService;
import au.org.ala.fielddata.mobile.ui.SpeciesListFragment;
import au.org.ala.fielddata.mobile.ui.SpeciesSelectionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Presents a list of species to the user for information purposes.
 */
public class SpeciesListActivity extends SpeciesListFragment implements SpeciesSelectionListener {


	class OnQueryTextListener extends OnQueryTextListenerCompat {

		@Override
		public boolean onQueryTextChange(String newText) {
			return true;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			return false;
		}
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setHasOptionsMenu(true);
		init();
	}
	
	@Override
	public void onResume() {
		super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		Species species = ((SpeciesAdapter)l.getAdapter()).getSpecies(position);
		onSpeciesSelected(species);
		
	}
	
	/** This won't currently be called as we haven't called setHasOptionsMenu(true) */
	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Place an action bar item for searching.
        MenuItem item = menu.add("Search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        View searchView = SearchViewCompat.newSearchView(getActivity());
        SearchViewCompat.setOnQueryTextListener(searchView, new OnQueryTextListener());
        item.setActionView(searchView);
    }

	public void onSpeciesSelected(final Species species) {
		
		final List<Survey> surveys = getSurveys(species);
		final String[] items = buildMenuItems(surveys);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.species_action_title));
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        if (item > 0) {
		        	recordObservation(species, surveys.get(item-1));
		        } else {
		        	showFieldGuide(species);
		        }
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private String[] buildMenuItems(List<Survey> surveys) {
		String[] items = new String[surveys.size() + 1];
		String recordItem = getString(R.string.record_observation);
		items[0] = getString(R.string.view_field_guide);
		
		int i = 1;
		for (Survey survey : surveys) {
			items[i++] = String.format(recordItem, survey.name);
		}
		
		return items;
	}
	
	private List<Survey> getSurveys(Species species) {
		return new SurveyDAO(getActivity()).surveysForSpecies(species.server_id);
	}

	private void recordObservation(Species species, Survey survey) {
		Intent intent = new Intent(getActivity(), CollectSurveyData.class);
		intent.putExtra(CollectSurveyData.SURVEY_BUNDLE_KEY, survey.server_id);
		intent.putExtra(CollectSurveyData.SPECIES, species.getId());
		startActivity(intent);
	}
	
	private void showFieldGuide(final Species species) {
		
		Intent intent = new Intent(getActivity(), FieldGuideActivity.class);
		intent.putExtra(CollectSurveyData.SPECIES, species.getId());
		startActivity(intent);
	}
    
}
