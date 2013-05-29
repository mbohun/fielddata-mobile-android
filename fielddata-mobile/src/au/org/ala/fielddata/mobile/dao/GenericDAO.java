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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import au.org.ala.fielddata.mobile.model.Persistent;
import au.org.ala.fielddata.mobile.service.dto.Mapper;

import com.google.gson.Gson;

public class GenericDAO<T extends Persistent> {

	protected DatabaseHelper helper;
	protected Context context;

	public static final String SELECT_BY_ID = "SELECT json FROM %s WHERE _id=?";

	public GenericDAO(Context ctx) {
		helper = DatabaseHelper.getInstance(ctx);
		context = ctx;
	}

	public T findByServerId(Class<T> modelClass, Integer id) {
		return findByColumn(modelClass, "server_id", Integer.toString(id), true);
	}
	
	public T findByServerId(Class<T> modelClass, Integer id, SQLiteDatabase db) {
		return findByColumn(modelClass, "server_id", Integer.toString(id), true, db);
	}

	public T load(Class<T> modelClass, Integer id) {
		return findByColumn(modelClass, "_id", Integer.toString(id), false);
	}

	public T loadIfExists(Class<T> modelClass, Integer id) {
		return findByColumn(modelClass, "_id", Integer.toString(id), true);
	}
	
	protected T findByColumn(Class<T> modelClass, String column, String value,
			boolean allowNoResults, SQLiteDatabase db) {
		Cursor result = null;
		T modelObject = null;
		try {
			result = db.query(true, tableName(modelClass), null, column + " = ?",
				new String[] { value }, null, null, null, null);

			if (result.getCount() != 1) {
	
				if (!allowNoResults) {
					Log.e("GenericDAO", "Expected 1 " + tableName(modelClass) + ", found: "
							+ result.getCount());
					throw new DatabaseException("Expected 1 result, found: "
							+ result.getCount());
				}
			} else {
				result.moveToFirst();
				modelObject = map(db, result, modelClass);
			}
		}
		finally {
			if (result != null) {
				result.close();
			}
		}
		return modelObject;
	}
	

	protected T findByColumn(Class<T> modelClass, String column, String value,
			boolean allowNoResults) {
		synchronized (helper) {

			T result = null;
			SQLiteDatabase db = helper.getReadableDatabase();
			
			try {
				db.beginTransaction();
				
				result = findByColumn(modelClass, column, value, allowNoResults, db);
				
				db.setTransactionSuccessful();
			} finally {
				
				if (db != null) {
					db.endTransaction();

				}
			}

			return result;
		}
	}

	public Integer save(T modelObject) {
		synchronized (helper) {
			Integer id = null;
			SQLiteDatabase db = helper.getWritableDatabase();
			try {
				db.beginTransaction();

				id = save(modelObject, db);

				db.setTransactionSuccessful();
			} finally {
				if (db != null) {
					db.endTransaction();
					
				}
			}
			return id;
		}
	}

	public Integer save(T modelObject, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		long now = System.currentTimeMillis();
		values.put("updated", now);
		modelObject.updated = now;
		Gson gson = Mapper.getGson(context);
		String value = gson.toJson(modelObject);
		values.put("json", value);
		values.put("server_id", modelObject.server_id);

		@SuppressWarnings("unchecked")
		Class<T> class1 = (Class<T>) modelObject.getClass();
		if (modelObject.getId() != null) {
			db.update(tableName(class1), values, "_id=?",
					new String[] { Integer.toString(modelObject.getId()) });
		} else {
			values.put("created", now);
			modelObject.created = now;
			long id = db.insertOrThrow(tableName(class1), null, values);
			if (id == -1) {
				throw new RuntimeException("Error saving object: " + modelObject);
			}
			modelObject.setId((int) id);
		}
		return modelObject.getId();
	}

	public List<T> loadAll(Class<T> modelClass) {
		synchronized (helper) {
			List<T> results = new ArrayList<T>();
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor result = null;
			T modelObject = null;
			try {
				db.beginTransaction();
				result = db.query(false, tableName(modelClass), null, null, null, null, null, null,
						null);

				if (result.getCount() > 0) {

					result.moveToFirst();
					while (!result.isAfterLast()) {
						modelObject = map(db, result, modelClass);
						results.add(modelObject);
						result.moveToNext();

					}
				}
				db.setTransactionSuccessful();

			} finally {
				if (result != null) {
					result.close();
				}
				if (db != null) {
					db.endTransaction();
					
				}
			}

			return results;
		}
	}

	public int count(Class<T> modelClass) {
		synchronized (helper) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor result = null;

			int count = 0;
			try {
				db.beginTransaction();

				result = db.rawQuery(
						String.format("SELECT count(*) from %s", tableName(modelClass)), null);
				if (result.getCount() != 1) {
					throw new DatabaseException("Error performing query");
				}
				result.moveToFirst();
				count = result.getInt(0);

				db.setTransactionSuccessful();
			} finally {
				if (result != null) {
					result.close();
				}
				if (db != null) {
					db.endTransaction();
					
				}
			}
			return count;
		}
	}

	protected T map(SQLiteDatabase db, Cursor result, Class<T> modelClass) {
		T modelObject;
		String json = result.getString(5);
		Gson gson = Mapper.getGson(context);
		modelObject = (T) gson.fromJson(json, modelClass);
		modelObject.setId(result.getInt(0));

		return modelObject;
	}

	public void deleteAll(Class<T> modelClass) {
		synchronized (helper) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			try {
				db.beginTransaction();
				db.delete(tableName(modelClass), null, null);
				db.setTransactionSuccessful();
			} finally {
				if (db != null) {
					db.endTransaction();
				}
			}
		}
	}

	public void delete(Class<T> modelClass, Integer id) {
		synchronized (helper) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			try {
				db.beginTransaction();
				db.delete(tableName(modelClass), "_id=?", new String[] { Integer.toString(id) });
				db.setTransactionSuccessful();
			} finally {
				if (db != null) {
					db.endTransaction();
				}
			}
		}
	}

	protected String tableName(Class<T> modelClass) {
		return modelClass.getSimpleName();
	}
}
