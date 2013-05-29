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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import au.org.ala.fielddata.mobile.Utils;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.model.User;

/**
 * Responsible for creating and configuring the database used by the
 * application.
 * As a first cut, all objects are stored in a table equal to their class 
 * name in a serialized form (json) along side a little bit of metadata.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "FieldData.db";
	private static final int SCHEMA_VERSION = 4;

	private static final String[] TABLES = { 
		Survey.class.getSimpleName(),
		User.class.getSimpleName()};

	private static DatabaseHelper instance;
	
	public static interface UpdateWork {
		public void doUpdate();
	}
	
	public synchronized static DatabaseHelper getInstance(Context ctx) {
		if (instance == null) {
			instance = new DatabaseHelper(ctx.getApplicationContext());
		}
		return instance;
	}
	
	private DatabaseHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, SCHEMA_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			db.beginTransaction();

			for (String table : TABLES) {
				db.execSQL("CREATE TABLE "
						+ table
						+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, server_id INTEGER, " +
							"created INTEGER, updated INTEGER, last_sync INTEGER" +
							"name TEXT, json TEXT)");
				
				
			}
			createRecordTable(db);
			createSpeciesTables(db);
			createAttributeRowTable(db);
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
	}

	public synchronized void doInTransaction(UpdateWork work) {
		
		SQLiteDatabase db =getWritableDatabase();
		try {
			db.beginTransaction();
			
			work.doUpdate();

			db.setTransactionSuccessful();
		} finally {
			if (db != null) {
				db.endTransaction();
				close();
			}
		}
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (int version=oldVersion+1; version<=newVersion; version++) {
			switch (version) {
			case 2:
				version2(db);
				break;
			
			case 3:
				version3(db);
				break;
			case 4:
				version4(db);
				break;
			}
			
		}
	}
	
	private void version2(SQLiteDatabase db) {
		if (Utils.DEBUG) {
			Log.i("DatabaseHelper", "Upgrading to version 2 of the schema");
		}
		db.execSQL("DROP TABLE "+Record.class.getSimpleName());
		createRecordTable(db);
	}
	
	private void version3(SQLiteDatabase db) {
		if (Utils.DEBUG) {
			Log.i("DatabaseHelper", "Upgrading to version 3 of the schema");
		}
		db.execSQL("DROP TABLE "+Species.class.getSimpleName());
		createSpeciesTables(db);
	}
	
	private void version4(SQLiteDatabase db) {
		if (Utils.DEBUG) {
			Log.i("DatabaseHelper", "Upgrading to version 4 of the schema");
		}
		createAttributeRowTable(db);
		db.execSQL("ALTER TABLE "+RecordDAO.ATTRIBUTE_VALUE_TABLE+" ADD COLUMN row_id INTEGER");
	}

	private void createRecordTable(SQLiteDatabase db) {
		
		db.execSQL(RecordDAO.RECORD_TABLE_DDL);
		db.execSQL(RecordDAO.ATTRIBUTE_VALUE_TABLE_DDL);
		db.execSQL(DraftRecordDAO.DRAFT_RECORD_TABLE_DDL);
		db.execSQL(DraftRecordDAO.DRAFT_ATTRIBUTE_TABLE_DDL);
		
		
	}
	
	private void createSpeciesTables(SQLiteDatabase db) {
		db.execSQL(SpeciesDAO.SPECIES_TABLE_DDL);
		db.execSQL(SpeciesDAO.SURVEY_SPECIES_TABLE_DDL);
		db.execSQL(SpeciesDAO.SPECIES_GROUP_DDL);
	}
	
	private void createAttributeRowTable(SQLiteDatabase db) {
		db.execSQL(RecordDAO.ATTRIBUTE_ROW_TABLE_DDL);
	}
	
}
