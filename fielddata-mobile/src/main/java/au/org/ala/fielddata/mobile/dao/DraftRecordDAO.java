package au.org.ala.fielddata.mobile.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import au.org.ala.fielddata.mobile.model.Record;

public class DraftRecordDAO extends RecordDAO {
	
	public static final String DRAFT_RECORD_TABLE = "DRAFT_RECORD";
	public static final String DRAFT_ATTRIBUTE_VALUE_TABLE = "DRAFT_ATTRIBUTE_VALUE";
	
	public static final String DRAFT_RECORD_TABLE_DDL = "CREATE TABLE "+DRAFT_RECORD_TABLE+
	" ("+RECORD_COLUMNS +", record_id INTEGER)";
	private static final int RECORD_ID_COLUMN = 18;
	
	public static final String DRAFT_ATTRIBUTE_TABLE_DDL = 
		"CREATE TABLE "+DRAFT_ATTRIBUTE_VALUE_TABLE+ ATTRIBUTE_VALUE_COLUMNS;
	
	public DraftRecordDAO(Context ctx) {
		super(ctx);
		
		recordTable = DRAFT_RECORD_TABLE;
		attributeValueTable = DRAFT_ATTRIBUTE_VALUE_TABLE;
	}
	
	protected Record map(SQLiteDatabase db, Cursor result, Class<Record> modelClass) {
		Record record = super.map(db, result, modelClass);
		if (result.isNull(RECORD_ID_COLUMN)) {
			record.setId(null);
		}
		else {
			record.setId(result.getInt(RECORD_ID_COLUMN));
		}
		
		return record;
	}
	
	public Integer save(Record record, SQLiteDatabase db) {
		
		Integer id = record.getId();
		int draftId = super.save(record, db);
		record.setId(id);
		
		return draftId;
	}
	
	protected boolean map(Record record, long now, ContentValues values) {
		super.map(record, now, values);
		values.put("record_id", record.getId());
		
		return false;
	}
}
