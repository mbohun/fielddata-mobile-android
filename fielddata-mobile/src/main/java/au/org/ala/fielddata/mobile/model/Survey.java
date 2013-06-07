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
package au.org.ala.fielddata.mobile.model;

import java.util.ArrayList;
import java.util.List;

import au.org.ala.fielddata.mobile.model.Attribute.AttributeType;

import com.google.gson.annotations.SerializedName;

public class Survey extends Persistent {

	
	public static class SurveyDetails {
		public int id;
		public Long startDate;
		public Long endDate;
		public String name;
		public String description;
	}
	
	public String name;
	public String description;
	public MapDefaults map;
    public boolean locationPolygon = false;
    public int photoPointAttribute = -1;
	
	@SerializedName("attributesAndOptions")
	public List<Attribute> attributes;
	public List<RecordProperty> recordProperties;
	public List<Integer> speciesIds;
	
	@SerializedName("indicatorSpecies_server_ids")
	public SurveyDetails details;
	
	public String imageUrl;
	
	public boolean hasSpecies() {
		return propertyByType(AttributeType.SPECIES_P) != null;
	}
	
	public boolean isAllSpeciesSurvey() {
		return hasSpecies() && (speciesIds == null || speciesIds.isEmpty());
	}
	
	public boolean recordsSpecies(Integer speciesServerId) {
		return hasSpecies() && (isAllSpeciesSurvey() || speciesIds.contains(speciesServerId));
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
	

	public RecordProperty propertyByType(AttributeType type) {
		for (RecordProperty prop : recordProperties) {
			if (prop.getType() == type) {
				return prop;
			}
		}
		return null;
	}
	
	public String toString() {
		return name;
	}
	
	
	public List<Attribute> allAttributes() {
		List<Attribute> all = new ArrayList<Attribute>();
		all.addAll(recordProperties);
		all.addAll(attributes);
		
		return all;
	}
}
