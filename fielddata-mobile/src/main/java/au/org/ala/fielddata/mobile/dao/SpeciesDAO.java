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
package au.org.ala.fielddata.mobile.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.SpeciesGroup;
import au.org.ala.fielddata.mobile.model.Survey;

public class SpeciesDAO extends GenericDAO<Species> {

	public static final String SURVEY_SPECIES_TABLE = "SURVEY_SPECIES";
	public static final String SPECIES_TABLE = "SPECIES";
	public static final String SPECIES_GROUP_TABLE = "SPECIES_GROUP";
	
	
	// Column names for the species table
	public static final String SERVER_ID_COLUMN_NAME = "server_id";
	public static final String CREATED_COLUMN_NAME = "created";
	public static final String UPDATED_COLUMN_NAME = "updated";
	public static final String LSID_COLUMN_NAME = "lsid";
	public static final String SCIENTIFIC_NAME_COLUMN_NAME = "scientific_name";
	public static final String COMMON_NAME_COLUMN_NAME = "column_name";
	public static final String IMAGE_URL_COLUMN_NAME = "image_url";
	public static final String SPECIES_GROUP_COLUMN_NAME = "species_group_id";
	
	
	public static final String SPECIES_TABLE_COLUMNS = 
			"_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
			SERVER_ID_COLUMN_NAME + " INTEGER, " +
		    CREATED_COLUMN_NAME + " INTEGER, " +
		    UPDATED_COLUMN_NAME + " INTEGER, " +
		    LSID_COLUMN_NAME+" TEXT, " +
		    SCIENTIFIC_NAME_COLUMN_NAME + " TEXT, "+
		    COMMON_NAME_COLUMN_NAME + " TEXT, "+
		    IMAGE_URL_COLUMN_NAME + " TEXT, " +
		    SPECIES_GROUP_COLUMN_NAME + " INTEGER";
	
	// Shared column indexes (select *)
	public static final int ID_COLUMN_IDX = 0;
	public static final int SERVER_ID_COLUMN_IDX = 1;
	public static final int CREATED_COLUMN_IDX = 2;
	public static final int UPDATED_COLUMN_IDX = 3;
	
	// Column indexes for the SPECIES TABLE (select *)
	public static final int LSID_COLUMN_IDX = 4;
	public static final int SCIENTIFIC_NAME_COLUMN_IDX = 5;
	public static final int COMMON_NAME_COLUMN_IDX = 6;
	public static final int IMAGE_URL_COLUMN_IDX = 7;
	public static final int SPECIES_GROUP_COLUMN_IDX = 8;
		
	
	public static final String SPECIES_TABLE_DDL = "CREATE TABLE "+SPECIES_TABLE+
				" ("+ SPECIES_TABLE_COLUMNS+ ")";
	
	public static final String SURVEY_SPECIES_TABLE_DDL = "CREATE TABLE "+SURVEY_SPECIES_TABLE+
			" (survey_id INTEGER, species_id INTEGER)";
	
	public static final String SPECIES_GROUP_DDL = "CREATE TABLE "+SPECIES_GROUP_TABLE+
			" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "server_id INTEGER, "+
			"name TEXT, " +
			"parent_group_id INTEGER)";
    public static final String SPECIES_GROUP_NAME_COLUMN_NAME = "name";

    public SpeciesDAO(Context ctx) {
		super(ctx);
	}

	protected Species map(SQLiteDatabase db, Cursor result, Class<Species> modelClass) {
		
		Species species = new Species();
		species.setId(result.getInt(ID_COLUMN_IDX));
		species.server_id = result.getInt(SERVER_ID_COLUMN_IDX);
		species.created = result.getLong(CREATED_COLUMN_IDX);
		species.updated = result.getLong(UPDATED_COLUMN_IDX);
		species.setLsid(result.getString(LSID_COLUMN_IDX));
		species.scientificName = result.getString(SCIENTIFIC_NAME_COLUMN_IDX);
		species.commonName = result.getString(COMMON_NAME_COLUMN_IDX);
		species.setImageFileName(result.getString(IMAGE_URL_COLUMN_IDX));
		species.setTaxonGroupId(result.getInt(SPECIES_GROUP_COLUMN_IDX));
		
		return species;
	}
	
	public Integer save(Species species, SQLiteDatabase db) {
		
		long now = System.currentTimeMillis();
		
		ContentValues values = new ContentValues();
		boolean update = map(species, now, values);                                                                                                                                                 
	
		Integer id;
		if (!update) {
			id = (int)db.insertOrThrow(SPECIES_TABLE, null, values);
			species.setId(id);
		}
		else {
			id = species.getId();
			String whereClause = "_id=?";
			String[] params = new String[] { Integer.toString(id)};
			int numRows = db.update(SPECIES_TABLE, values, whereClause, params);
			if (numRows != 1) {
				throw new DatabaseException("Update failed for record with id="+id+", table="+SPECIES_TABLE);
			}
		}
		return id;
	}
	
	public void saveSpeciesSurveyAssociation(Survey survey, SQLiteDatabase db) {
		
		// Delete the old association so we don't get duplicates when
		// surveys are re-downloaded.
		deleteSpeciesForSurvey(survey, db);
		if (survey.speciesIds == null || survey.speciesIds.size() == 0) {
			return;
		}
		
		Integer surveyId = survey.getId();
		InsertHelper insertHelper = new InsertHelper(db, SURVEY_SPECIES_TABLE);
		for (Integer speciesId : survey.speciesIds) {
			
			insertHelper.prepareForInsert();
			insertHelper.bind(1, surveyId);
			insertHelper.bind(2, speciesId);
			insertHelper.execute();
		}
		
	}
	
	public void deleteSpeciesForSurvey(Survey survey, SQLiteDatabase db) {
		db.delete(SURVEY_SPECIES_TABLE, "survey_id=?", new String[] {Integer.toString(survey.getId())});
	}
	
	public void deleteAll(Class<Species> modelClass) {
		synchronized (helper) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			try {
				db.beginTransaction();

                deleteAll(db);

				db.setTransactionSuccessful();
			} finally {
				if (db != null) {
					db.endTransaction();
				}
			}
		}
	}

    public void deleteAll(SQLiteDatabase db) {
        db.delete(SPECIES_TABLE, null, null);
        db.delete(SURVEY_SPECIES_TABLE, null, null);
        db.delete(SPECIES_GROUP_TABLE, null, null);
    }


	private boolean map(Species species, long now, ContentValues values) {
		Integer id = species.getId();
		boolean update = id != null;
	
		values.put(SERVER_ID_COLUMN_NAME, species.server_id);
		values.put(UPDATED_COLUMN_NAME, now);
		if (!update) {
			species.created = now;
			values.put(CREATED_COLUMN_NAME, now);
		}
		values.put(LSID_COLUMN_NAME, species.getLsid());
		values.put(SCIENTIFIC_NAME_COLUMN_NAME, species.scientificName);
		values.put(COMMON_NAME_COLUMN_NAME, species.commonName);
		values.put(IMAGE_URL_COLUMN_NAME, species.getImageFileName());
		values.put(SPECIES_GROUP_COLUMN_NAME, species.getTaxonGroupId());
		
		return update;
	}
	
	public List<Species> speciesForSurvey(Integer surveyId) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor result = null;
		List<Species> speciesList = new ArrayList<Species>();
		try {
			
			String query = "SELECT * from "+SPECIES_TABLE+" s inner join "+SURVEY_SPECIES_TABLE+
					" ss on s.server_id = ss.species_id where ss.survey_id = ?";
			result = db.rawQuery(query, new String[] {Integer.toString(surveyId)});

			if (result.getCount() == 0) {
				speciesList = loadAll(Species.class);
				
			} else {
				result.moveToFirst();
				while (!result.isAfterLast()) {
					speciesList.add( map(db, result, Species.class) );
					result.moveToNext();
				}
			}
		}
		finally {
			if (result != null) {
				result.close();
			}
		}
		return speciesList;
	}
	
	public Cursor loadSpecies() {

        boolean useGroup = false;
        String query;
        if (!useGroup) {
            query = "SELECT s._id, s."+SCIENTIFIC_NAME_COLUMN_NAME+", s."+COMMON_NAME_COLUMN_NAME+
            ", s."+IMAGE_URL_COLUMN_NAME+" , 'All Weeds' as name from "+SPECIES_TABLE+" s order by s."+COMMON_NAME_COLUMN_NAME;
        }
        else {
            query = "SELECT s._id, s."+SCIENTIFIC_NAME_COLUMN_NAME+", s."+COMMON_NAME_COLUMN_NAME+
                    ", s."+IMAGE_URL_COLUMN_NAME+", g.name"+
                    " from "+SPECIES_TABLE+" s left outer join "+SPECIES_GROUP_TABLE+
                    " g on s."+SPECIES_GROUP_COLUMN_NAME+"=g.server_id order by g.name, s."+COMMON_NAME_COLUMN_NAME;
        }

		return helper.getReadableDatabase().rawQuery(query, null);
	}

    public Cursor searchSpecies(String queryString) {

        String query = "SELECT s._id, s."+SCIENTIFIC_NAME_COLUMN_NAME+", s."+COMMON_NAME_COLUMN_NAME+
                ", s."+IMAGE_URL_COLUMN_NAME+", g.name"+
                " from "+SPECIES_TABLE+" s left outer join "+SPECIES_GROUP_TABLE+
                " g on s."+SPECIES_GROUP_COLUMN_NAME+"=g.server_id "+
                " where s."+SCIENTIFIC_NAME_COLUMN_NAME+" like ? or s."+COMMON_NAME_COLUMN_NAME+" like ?"+
                " order by g.name, s."+COMMON_NAME_COLUMN_NAME;

        StringBuffer like = new StringBuffer();
        like.append("%").append(queryString).append("%");
        String param = like.toString();
        return helper.getReadableDatabase().rawQuery(query, new String[]{param, param});
    }

    public void saveSpeciesGroups(List<SpeciesGroup> groups, SQLiteDatabase db) {
        db.execSQL("delete from "+SPECIES_GROUP_TABLE);

        InsertHelper insertHelper = new InsertHelper(db, SPECIES_GROUP_TABLE);

        saveSpeciesGroups(groups, insertHelper);
    }

    private void saveSpeciesGroups(List<SpeciesGroup> groups, InsertHelper insertHelper) {
        for (SpeciesGroup group : groups) {

            insertHelper.prepareForInsert();
            insertHelper.bind(2, group.getId());
            insertHelper.bind(3, group.name);
            insertHelper.bind(4, group.getId() != null ? group.getId() : -1);
            insertHelper.execute();
            Log.i("SpeciesDAO", "Saving group: " + group.name + ", id: " + group.getId());

            saveSpeciesGroups(group.subgroups, insertHelper);

        }
    }
	
}
