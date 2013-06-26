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

import android.util.Log;

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
    public boolean locationPolygon = true;

	@SerializedName("attributesAndOptions")
	public List<Attribute> attributes;
	public List<RecordProperty> recordProperties;
	public List<Integer> speciesIds;
	
	@SerializedName("indicatorSpecies_server_ids")
	public SurveyDetails details;
	
	public String imageUrl;

    public Attribute getPhotoPointAttribute() {
        for (Attribute attribute : attributes) {
            if (attribute.name.equalsIgnoreCase("photopoints")) {
                return attribute;
            }
        }
        return null;
    }

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
		return findAttribute(id, attributes);
	}

    private Attribute findAttribute(int id, List<Attribute> attributes) {

        Log.i("Survey", "Finding attribute: " + id+ " in " + attributes);
        for (Attribute attribute : attributes) {
            if (attribute.server_id == id) {
                return attribute;
            }
            else if (attribute.nestedAttributes != null) {
                Attribute found = findAttribute(id, attribute.nestedAttributes);
                if (found != null) {
                    return found;
                }
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

    public void addAttribute(Attribute attribute) {
        if (attributes == null) {
            attributes = new ArrayList<Attribute>();
        }
        attributes.add(attribute);
    }

    public void addAttribute(AttributeType type, String name, int serverId) {
        Attribute attribute = new Attribute();
        attribute.server_id = serverId;
        attribute.setType(type);
        attribute.name = name;
        addAttribute(attribute);
    }
}
