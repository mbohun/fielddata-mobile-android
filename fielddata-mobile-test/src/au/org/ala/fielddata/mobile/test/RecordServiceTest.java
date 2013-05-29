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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import android.test.AndroidTestCase;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.service.FieldDataServiceClient.SurveysAndSpecies;
import au.org.ala.fielddata.mobile.service.LoginService;
import au.org.ala.fielddata.mobile.service.FieldDataServiceClient;
import au.org.ala.fielddata.mobile.service.dto.LoginResponse;

/**
 * The class <code>RecordServiceTest</code> contains tests for the class {@link
 * <code>RecordService</code>}
 */
public class RecordServiceTest extends AndroidTestCase {

	private LoginResponse login;
	
	
	protected void setUp() {
		//login = new LoginService(getContext()).login(username, password, portalName);
	}

	/**
	 * Run the void sync(List<Record>) method test
	 */
	public void testSync() throws Exception
	{
		// add test code here
		FieldDataServiceClient fixture = new FieldDataServiceClient(getContext());
		List<Record> records = new ArrayList<Record>();
		Record r = new Record();
		records.add(r);
		fixture.sync(records);
		
	}
	
	public void testDownloadSurveys() {
		FieldDataServiceClient fixture = new FieldDataServiceClient(getContext());
		SurveysAndSpecies surveys = fixture.downloadSurveys();
		System.out.println("Surveys: "+surveys);
		
	}
}
