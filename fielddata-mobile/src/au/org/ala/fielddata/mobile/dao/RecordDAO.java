package au.org.ala.fielddata.mobile.dao;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Record.AttributeValue;
import au.org.ala.fielddata.mobile.model.Record.PropertyAttributeValue;

/**
 * The RecordDAO is responsible for storing and retrieving Records from the 
 * database.
 */
public class RecordDAO extends GenericDAO<Record> {

	public static final String ATTRIBUTE_VALUE_TABLE = "ATTRIBUTE_VALUE";
	public static final String ATTRIBUTE_ROW_TABLE = "ATTRIBUTE_ROW";
	public static final String RECORD_TABLE = "RECORD";
	
	
	// Shared column indexes (select *)
	public static final int ID_COLUMN = 0;
	public static final int SERVER_ID_COLUMN = 1;
	public static final int CREATED_COLUMN = 2;
	public static final int UPDATED_COLUMN = 3;
	
	// Column indexes for the RECORD TABLE (select *)
	public static final int UUID_COLUMN = 4;
	public static final int NUMBER_COLUMN = 5;
	public static final int WHEN_COLUMN = 6;
	public static final int NOTES_COLUMN = 7;
	public static final int LATITUDE_COLUMN = 8;
	public static final int LONGITUDE_COLUMN = 9;
	public static final int ACCURACY_COLUMN = 10;
	public static final int POINT_TIME_COLUMN = 11;
	public static final int POINT_SOURCE_COLUMN = 12;
	public static final int LOCATION_ID_COLUMN = 13;
	public static final int SURVEY_ID_COLUMN = 14;
	public static final int TAXON_ID_COLUMN = 15;
	public static final int STATUS_COLUMN = 16;
	public static final int SCIENTIFIC_NAME_COLUMN = 17;
	
	// Column indexes for the ATTRIBUTE_VALUE table (select *)
	public static final int RECORD_ID_COLUMN = 4;
	public static final int ATTRIBUTE_ID_COLUMN = 5;
	public static final int ATTRIBUTE_VALUE_COLUMN = 6;
	public static final int TYPE_COLUMN = 7;
	
	// Column indexes for the PHOTO_POINT table (select *)
	public static final int ROW_NUMBER_COLUMN = 4;
	public static final int PARENT_RECORD_ID_COLUMN = 5;
	public static final int PARENT_ATTRIBUTE_VALUE_COLUMN =6;

	
	private static final int TYPE_TEXT = 0;
	private static final int TYPE_URI = 1;
	
	protected static final String RECORD_COLUMNS = 
		"_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
		"server_id INTEGER, " +
	    "created INTEGER, " +
	    "updated INTEGER, " +
	    "uuid TEXT, " +
	    "number INTEGER, "+
	    "when_millis INTEGER, "+
	    "notes TEXT, "+
	    "latitude REAL, " +
	    "longitude REAL, " +
	    "accuracy REAL, " +
	    "point_millis INTEGER, " +
	    "point_source TEXT, " +
	    "location_id INTEGER, " +
	    "survey_id INTEGER, "+
	    "taxon_id INTEGER, "+
	    "status INTEGER, " +
	    "scientific_name TEXT";
		
    public static final String RECORD_TABLE_DDL = "CREATE TABLE "+RECORD_TABLE+
	" ("+ RECORD_COLUMNS+ ")";
	
	
    protected static final String ATTRIBUTE_VALUE_COLUMNS =  " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
	"server_id INTEGER, "+
	"created INTEGER, " +
    "updated INTEGER, " +
    "record_id INTEGER, " +
    "attribute_id INTEGER, "+
    "value TEXT, " +
    "type INTEGER, " +
    "row_id INTEGER)";
    
    protected static final String ATTRIBUTE_ROW_COLUMNS = " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
      "server_id INTEGER, "+
      "created INTEGER, "+
      "updated INTEGER, "+
      "row_number INTEGER, "+
      "parent_record_id INTEGER "+
      "parent_attribute_value INTEGER)";
	
	public static final String ATTRIBUTE_VALUE_TABLE_DDL = 
		"CREATE TABLE "+ATTRIBUTE_VALUE_TABLE+ ATTRIBUTE_VALUE_COLUMNS;
	
	public static final String ATTRIBUTE_ROW_TABLE_DDL = 
			"CREATE TABLE "+ATTRIBUTE_ROW_TABLE+ATTRIBUTE_ROW_COLUMNS;
	
	
	protected String recordTable;
	protected String attributeValueTable;
	
	public RecordDAO(Context ctx) {
		super(ctx);
		
		recordTable = RECORD_TABLE;
		attributeValueTable = ATTRIBUTE_VALUE_TABLE;
	}
	
	public Integer save(Record record, SQLiteDatabase db) {
		
		long now = System.currentTimeMillis();
		
		ContentValues values = new ContentValues();
		boolean update = map(record, now, values);                                                                                                                                                 
	
		Integer id;
		if (!update) {
			id = (int)db.insertOrThrow(recordTable, null, values);
			record.setId(id);
		}
		else {
			id = record.getId();
			String whereClause = "_id=?";
			String[] params = new String[] { Integer.toString(id)};
			int numRows = db.update(recordTable, values, whereClause, params);
			if (numRows != 1) {
				throw new DatabaseException("Update failed for record with id="+id+", table="+recordTable);
			}
			// Since the number of attributes is fairly small we can probably 
			// get away with re-writing the attributes.
			whereClause = "record_id=?";
			db.delete(attributeValueTable, whereClause, params);
		}
		
		InsertHelper insertHelper = new InsertHelper(db, attributeValueTable);
		for (AttributeValue attrValue : record.getAttributeValues()) {
			// PropertyAttributeValues have already been inserted as columns of the RECORD table.
			if (!(attrValue instanceof PropertyAttributeValue)) {
				// Don't insert values that are empty, this is to prevent problems
				// when we upload. (e.g. numbers evaluate to NaN, URIs give a 
				// broken image link etc.)
				String value = attrValue.nullSafeValue();
				if (value.length() > 0) {
					insertHelper.prepareForInsert();
					insertHelper.bind(CREATED_COLUMN+1, now);
					insertHelper.bind(UPDATED_COLUMN+1, now);
					//insertHelper.bind(SERVER_ID_COLUMN+1, attrValue.server_id);
					insertHelper.bind(ATTRIBUTE_ID_COLUMN+1, attrValue.attribute_id);
					insertHelper.bind(RECORD_ID_COLUMN+1, id);
					insertHelper.bind(ATTRIBUTE_VALUE_COLUMN+1, value);
					insertHelper.bind(TYPE_COLUMN+1, attrValue.isUri() ? TYPE_URI : TYPE_TEXT);
			
					long attr_value_id = insertHelper.execute();
					attrValue.id = (int)attr_value_id;
				}
			}
		}
		
		
		return id;
	}

	protected boolean map(Record record, long now, ContentValues values) {
		
		Integer id = record.getId();
		boolean update = id != null;
	
		values.put("uuid", record.uuid);
		//values.put("server_id", record.server_id); // Since we delete after upload we don't have to worry about server_id
		
		values.put("updated", now);
		if (!update) {
			record.created = now;
			values.put("created", now);
		}
		Location location = record.getLocation();
		if (location != null) {
			values.put("latitude", location.getLatitude());
			values.put("longitude", location.getLongitude());
			values.put("point_source", location.getProvider());
			values.put("accuracy", location.getAccuracy());
			values.put("point_millis", location.getTime());
		}
		else if (update) {
			values.put("latitude", (Double)null);
			values.put("longitude", (Double)null);
			values.put("point_source", (String)null);
			values.put("accuracy", (Float)null);
			values.put("point_millis", (Long)null);
		}
		values.put("when_millis", record.when);
		values.put("notes", record.notes);
		values.put("survey_id", record.survey_id);
		values.put("number", record.number);
		values.put("taxon_id", record.taxon_id);
		values.put("status", record.getStatus().ordinal());
		values.put("scientific_name", record.scientificName);
		record.updated = now;
		return update;
	}
	
	protected Record map(SQLiteDatabase db, Cursor result, Class<Record> modelClass) {
		Record record = new Record();
		record.setId(result.getInt(ID_COLUMN));
		record.server_id = result.getInt(SERVER_ID_COLUMN);
		record.created = result.getInt(CREATED_COLUMN);
		record.updated = result.getInt(UPDATED_COLUMN);
		record.uuid = result.getString(UUID_COLUMN);
		record.number = result.getInt(NUMBER_COLUMN);
		record.when = result.getLong(WHEN_COLUMN);
		record.notes = result.getString(NOTES_COLUMN);
		
		String locationSource = result.getString(POINT_SOURCE_COLUMN);
		if (locationSource != null) {
			Location location = new Location(locationSource);
			location.setLatitude(result.getDouble(LATITUDE_COLUMN));
			location.setLongitude(result.getDouble(LONGITUDE_COLUMN));
			location.setAccuracy(result.getFloat(ACCURACY_COLUMN));
			location.setTime(result.getLong(POINT_TIME_COLUMN));
			record.setLocation(location);
		}
		record.location = result.getInt(LOCATION_ID_COLUMN);
		record.survey_id = result.getInt(SURVEY_ID_COLUMN);
		record.taxon_id = result.getInt(TAXON_ID_COLUMN);
		int status = result.getInt(STATUS_COLUMN);
		record.setStatus(Record.Status.values()[status]);
		record.scientificName = result.getString(SCIENTIFIC_NAME_COLUMN);
		
		Cursor values = db.query(false, attributeValueTable, new String[]{"_id", "attribute_id", "value", "type"}, "record_id = ?", new String[] {Integer.toString(record.getId())}, null, null, null, null);
		try {
			values.moveToFirst();
			List<AttributeValue> attrValues = record.getAttributeValues();
			while (!values.isAfterLast()) {
				
				AttributeValue value = new AttributeValue(values.getInt(1), values.getString(2), values.getInt(3) == TYPE_URI);
				attrValues.add(value);
				values.moveToNext();
			}
		}
		finally {
			values.close();
		}
		return record;
	}
	
	public void deleteAll(Class<Record> recordClass) {
		synchronized(helper) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		try {
			db.beginTransaction();
			db.delete(recordTable, null, null);
			db.delete(attributeValueTable, null, null);
			db.setTransactionSuccessful();
		}
		finally {
			if (db != null) {
				db.endTransaction();
			}
		}
		}
	}

	public void delete(Class<Record> recordClass, Integer id) {
		synchronized(helper) {
			SQLiteDatabase db = helper.getWritableDatabase();
			
			try {
				db.beginTransaction();
				String[] recordId = new String[] {Integer.toString(id)};
				db.delete(recordTable, "_id=?", recordId);
				db.delete(attributeValueTable, "record_id=?", recordId);
				db.setTransactionSuccessful();
			}
			finally {
				if (db != null) {
					db.endTransaction();
				}
			}
		}
	}
	
	protected String tableName(Class<Record> modelClass) {
		return recordTable;
	}
	
	/**
	 * Updates the status of the Records identified by the supplied ids.
	 * If the recordIds parameter is null (or length 0), the status change
	 * will be applied to all records. 
	 * @param recordIds identifies the Records to be updated.
	 * @param status the new status for the Records.
	 */
	public void updateStatus(int[] recordIds, Record.Status status) {
		
		synchronized(helper) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.beginTransaction();
			try {
				ContentValues values = new ContentValues();
				values.put("status", status.ordinal());
				
				StringBuilder whereClause = new StringBuilder();
				whereClause.append("status != ").append(Record.Status.DRAFT.ordinal());
				String[] recordIdStrings = null;
				if (recordIds != null && recordIds.length > 0) {
					recordIdStrings = new String[recordIds.length];
					
					whereClause.append(" and _id in (");
					for (int i=0; i<recordIds.length; i++) {
						whereClause.append("?");
						if (i < recordIds.length-1) {
							whereClause.append(",");
						}
						else {
							whereClause.append(")");
						}
						recordIdStrings[i] = Integer.toString(recordIds[i]);
					}
				}
				
				db.update(recordTable, values, whereClause.toString(), recordIdStrings);
				db.setTransactionSuccessful();
			}
			finally {
				if (db != null) {
					db.endTransaction();
					
				}
			}
		}
	}
	
	public void updateStatus(List<Integer> recordIds, Record.Status status) {
		int[] ids = new int[recordIds.size()];
		
		for (int i=0; i<recordIds.size(); i++) {
			ids[i] = recordIds.get(i);
		}
		updateStatus(ids, status);
	}
}
