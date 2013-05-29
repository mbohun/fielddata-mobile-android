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
package au.org.ala.fielddata.mobile.test;

import android.location.Location;
import android.net.Uri;
import android.test.AndroidTestCase;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeType;
import au.org.ala.fielddata.mobile.model.Record;

public class GenericDaoTest extends AndroidTestCase {

	/**
	 * Tests a new Record can be inserted into the database.
	 */
	public void testRecordInsert() throws Exception {
		RecordDAO recordDao = new RecordDAO(getContext());
		
		
		long now = System.currentTimeMillis();
				
		Location location = createLocation(now);
		Record record = createRecord(now, location);
		
		String value = "Test";
		Attribute attr = addAttribute(record, 1, AttributeType.CATEGORIZED_MULTI_SELECT, value);
		
		Attribute attr2 = new Attribute();
		attr2.server_id = 2;
		attr2.typeCode = AttributeType.IMAGE.getCode();
		Uri uri = Uri.parse("file:///test/photo.jpg");
		record.setUri(attr2, uri);
		
		recordDao.save(record);
		
		Record record2 = recordDao.load(Record.class, record.getId());
		
		Location location2 = record2.getLocation();
		
		assertEquals(location.getLatitude(), location2.getLatitude());
		assertEquals(location.getLongitude(), location2.getLongitude());
		assertEquals(location.getAccuracy(), location2.getAccuracy());
		assertEquals(location.getProvider(), location2.getProvider());
		assertEquals(location.getTime(), location2.getTime());
		
		assertEquals(record.notes, record2.notes);
		assertEquals(record.number, record2.number);
		assertEquals(record.scientificName, record2.scientificName);
		assertEquals(record.survey_id, record2.survey_id);
		assertEquals(record.taxon_id, record2.taxon_id);
		assertEquals(record.when, record2.when);
		
		assertEquals(value, record2.getValue(attr));
		assertEquals("file:///test/photo.jpg", record2.getUri(attr2).toString());
	}

	private Attribute addAttribute(Record record, int serverId, AttributeType type, String value) {
		Attribute attr = new Attribute();
		attr.server_id = serverId;
		attr.typeCode = type.getCode();
		record.setValue(attr, value);
		return attr;
	}

	private Record createRecord(long now, Location location) {
		Record record = new Record();
		
		record.setLocation(location);
		
		record.notes = "test from android";
		record.number = 2;
		record.scientificName = "Test";
		record.survey_id = 1;
		record.taxon_id = 2;
		record.when = now;
		return record;
	}

	private Location createLocation(long now) {
		Location location = new Location("GPS");
		location.setLatitude(-36.885845);
		location.setLongitude( 149.912548);
		location.setAccuracy(10.0f);
		location.setTime(now);
		return location;
	}
	
	/**
	 * Tests a new Record with a null location can be inserted into the database.
	 */
	public void testRecordInsertWithNullLocation() throws Exception {
		RecordDAO recordDao = new RecordDAO(getContext());
		
		long now = System.currentTimeMillis();
		Record record = createRecord(now, null);
		
		String value = "Test";
		Attribute attr = addAttribute(record, 1, AttributeType.CATEGORIZED_MULTI_SELECT, value);
		
		recordDao.save(record);
		
		Record record2 = recordDao.load(Record.class, record.getId());
		
		assertNull(record2.getLocation());
		assertEquals(record.notes, record2.notes);
		assertEquals(record.number, record2.number);
		assertEquals(record.scientificName, record2.scientificName);
		assertEquals(record.survey_id, record2.survey_id);
		assertEquals(record.taxon_id, record2.taxon_id);
		assertEquals(record.when, record2.when);
		
		assertEquals("Test", record2.getValue(attr));
	}
	
	/**
	 * Tests updating a record.
	 * Assumes that the save method works correctly.
	 */
	public void testRecordUpdate() throws Exception {
		RecordDAO recordDao = new RecordDAO(getContext());
		
		long now = System.currentTimeMillis();
		Record record = createRecord(now, null);
		
		Attribute attr = new Attribute();
		attr.server_id = 1;
		attr.typeCode = AttributeType.CATEGORIZED_MULTI_SELECT.getCode();
		record.setValue(attr, "Test");
		
		recordDao.save(record);
		
		record.notes = "Test2";
		recordDao.save(record);
		
		Record record2 = recordDao.load(Record.class, record.getId());
		
		assertNull(record2.getLocation());
		assertEquals(record.notes, record2.notes);
		assertEquals(record.number, record2.number);
		assertEquals(record.scientificName, record2.scientificName);
		assertEquals(record.survey_id, record2.survey_id);
		assertEquals(record.taxon_id, record2.taxon_id);
		assertEquals(record.when, record2.when);
		
		assertEquals(1, record2.getAttributeValues().size());
		assertEquals("Test", record2.getValue(attr));
	}
}
