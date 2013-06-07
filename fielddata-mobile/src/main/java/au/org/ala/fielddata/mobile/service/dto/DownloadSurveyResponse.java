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
package au.org.ala.fielddata.mobile.service.dto;

import java.util.List;

import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.MapDefaults;
import au.org.ala.fielddata.mobile.model.RecordProperty;

import com.google.gson.annotations.SerializedName;

public class DownloadSurveyResponse {

	public static class SurveyDetails {
		public int id;
		public Long startDate;
		public Long endDate;
		public String name;
		public String description;
		@SerializedName("species")
		public List<Integer> speciesIds;
	}
	
	public String imageUrl;
	public MapDefaults map;
		
	public String name;
	public String description;

    public boolean locationPolygon = false;
    public int photoPointAttribute = -1;
	
	@SerializedName("attributesAndOptions")
	public List<Attribute> attributes;
	public List<RecordProperty> recordProperties;
	
	@SerializedName("survey")
	public SurveyDetails details;
	
	
	public boolean hasSpecies() {
		return true;
	}
	
	public boolean hasNumber() {
		return true;
	}
	
	public boolean hasLocation() {
		return true;
	}
	
	public boolean hasAccuracy() {
		return true;
	}
	
	public boolean hasWhen() {
		return true;
	}
	
	public boolean hasTime() {
		return true;
	}
	
	public boolean hasCreated() {
		return true;
	}
	
	public boolean hasUpdated() {
		return true;
	}
	
	public Attribute getAttribute(int id) {
		for (Attribute attribute : attributes) {
			if (id == attribute.server_id) {
				return attribute;
			}
		}
		return null;
	}
	
	public String toString() {
		return name;
	}
	
}
