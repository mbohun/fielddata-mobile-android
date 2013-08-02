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
package au.org.ala.fielddata.mobile.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import au.org.ala.fielddata.mobile.Utils;
import au.org.ala.fielddata.mobile.dao.DatabaseHelper;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.dao.SpeciesDAO;
import au.org.ala.fielddata.mobile.dao.SurveyDAO;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeType;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.SpeciesGroup;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.pref.Preferences;

public class FieldDataService {

	public static interface SurveyDownloadCallback {
		public void surveysDownloaded(int number, int count);

        public void speciesDownloaded(int survey);
	}
	
	private FieldDataServiceClient webServiceClient;
	private Context ctx;
	
	public FieldDataService(Context ctx) {
		this.ctx = ctx;
		webServiceClient = new FieldDataServiceClient(ctx);
	}

	
	/**
	 * Downloads the Surveys configured in the current Portal and saves
	 * them to the database.
	 * @param callback optional callback to provide progress updates.
	 * @return a List of Surveys configured for the current Portal.
	 */
	public List<Survey> downloadSurveys(SurveyDownloadCallback callback) {
		
		long start = System.currentTimeMillis();
		List<Survey> surveys = webServiceClient.downloadSurveys(callback);
		long end = System.currentTimeMillis();
		Log.i("FieldDataService", "downloadSurveys took: "+(end-start));
		
		if (surveys.size() > 0) {
			Preferences prefs = new Preferences(ctx);
			prefs.setCurrentSurvey(surveys.get(0).server_id);
			prefs.setCurrentSurveyName(surveys.get(0).name);
			
		}
		
		DatabaseHelper helper = DatabaseHelper.getInstance(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.beginTransaction();
			SpeciesDAO speciesDAO = new SpeciesDAO(ctx);
            SurveyDAO surveyDAO = new SurveyDAO(ctx);

            List<Integer> surveysWithSpecies = new ArrayList<Integer>();
            int count = 0;

            List<SpeciesGroup> groups = webServiceClient.downloadSpeciesGroups();
            speciesDAO.saveSpeciesGroups(groups, db);


            for (Survey survey : surveys) {

				// If we already have a survey with the same id, replace it.
				Survey existingSurvey = surveyDAO.findByServerId(Survey.class, survey.server_id, db);
				if (existingSurvey != null) {
					if (Utils.DEBUG) {
						Log.i("FieldDataService", "Replacing survey with id: "+existingSurvey.server_id);
					}
					survey.setId(existingSurvey.getId());
				}
				if ("The Great Koala Count".equals(survey.name)) {
					survey.propertyByType(AttributeType.POINT).addOption("No Map");
				}
				surveyDAO.save(survey, db);
				speciesDAO.saveSpeciesSurveyAssociation(survey, db);



				int first = 0;
				int maxResults = 50;
				
				List<Species> speciesList;
				if (callback != null) {
                    callback.speciesDownloaded(++count);
                }
                do {
				
					start = System.currentTimeMillis();
					speciesList = webServiceClient.downloadSpecies(survey, surveysWithSpecies, first, maxResults);
					end = System.currentTimeMillis();
					Log.i("FieldDataService", "downloadSpecies took: "+(end-start)+" for "+speciesList.size()+" species");
					
					StorageManager manager = new StorageManager(ctx);
					
					start = System.currentTimeMillis();
					for (Species species : speciesList) {
						
						// If we already have a species with the same id, replace it.
						Species existingSpecies = speciesDAO.findByServerId(Species.class, species.server_id, db);
						if (existingSpecies != null) {
							if (Utils.DEBUG) {
								Log.i("FieldDataService", "Replacing species with id: "+existingSpecies.server_id);
							}
							species.setId(existingSpecies.getId());
						}
						speciesDAO.save(species, db);
						try {
							// Instruct the cache manager to download and cache the file
							manager.prefetchSpeciesProfileImage(species);
						} catch (Exception e) {
							Log.e("Service", "Error downloading profile image", e);
						}
			
					}
					end = System.currentTimeMillis();
					Log.i("FieldDataService", "save and download images took: "+(end-start));
					
					first += maxResults;
					
				}
				while (speciesList.size() == maxResults); 
				surveysWithSpecies.add(survey.server_id);
				
			}

			db.setTransactionSuccessful();
		} finally {
			if (db != null) {
				db.endTransaction();
				
			}
		}
		
		
		return surveys;
	}
	
}
