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

public class Attribute extends SurveyProperty {
	
	public static enum AttributeType {
		
		INTEGER("IN", "Integer"),
	    INTEGER_WITH_RANGE("IR", "Integer Range"),
	    DECIMAL("DE", "Decimal"),
	    
	    BARCODE("BC", "Bar Code"),
	    REGEX("RE", "Regular Expression"),
	    
	    DATE("DA", "Date"),
	    TIME("TM", "Time"),

	    STRING("ST", "Short Text"),
	    STRING_AUTOCOMPLETE("SA", "Short Text (Auto Complete)"),
	    TEXT("TA", "Long Text"),
	    
	    HTML("HL", "HTML (Validated)"),
	    HTML_NO_VALIDATION("HV", "HTML (Not Validated)"),
	    HTML_COMMENT("CM", "Comment"),
	    HTML_HORIZONTAL_RULE("HR", "Horizontal Rule"),

	    STRING_WITH_VALID_VALUES("SV", "Selection"),
	    
	    SINGLE_CHECKBOX("SC", "Single Checkbox"),
	    MULTI_CHECKBOX("MC", "Multi Checkbox"),
	    MULTI_SELECT("MS", "Multi Select"),
	    CATEGORIZED_MULTI_SELECT("CS", "Categorized Multi Select"),

	    IMAGE("IM", "Image File"),
	    AUDIO("AU", "Audio File"),
	    FILE("FI", "Data File"),

	    SPECIES("SP", "Species"),
	    
	    CENSUS_METHOD_ROW("CR", "Data Matrix Rows"),
	    CENSUS_METHOD_COL("CC", "Data Matrix Columns"),
		
	    POINT_SOURCE("PS", "The method by which the Point property was obtained"),
	    
		// record properties
		SPECIES_P("Species", "Species"),
		NUMBER("Number", "Number"),
		POINT("Point", "Point"),
		LOCATION("Location", "Location"),
		ACCURACY("AccuracyInMeters", "AccuracyInMeters"),
		WHEN("When", "When"),
		DWC_TIME("Time", "Time"),
		NOTES("Notes", "Notes");
		
		private String code;
		private String name;
		private AttributeType(String code, String name) {
			this.code = code;
			this.name = name;
		}
		
		public String getCode() {
			return code;
		}
		
		public String getName() {
			return name;
		}
		
		public static AttributeType fromCode(String code) {
			for (AttributeType type : AttributeType.values()) {
				if (type.code.equals(code)) {
					return type;
				}
			}
			return null;
		}
		
		public boolean isDateType() {
			return (this == DWC_TIME || this == WHEN || this == DATE || this == TIME);
		}

        public boolean supportsNestedValues() {
            return (this == CENSUS_METHOD_COL || this == CENSUS_METHOD_ROW);
        }
	}
	
	public static class AttributeOption {
		public Integer server_id;
		public Integer weight;
		public String value;
		
		public String toString() {
			return value;
		}
	}
	public String typeCode;
	public AttributeOption[] options;
	public List<Attribute> nestedAttributes;

    /** Default constructor used by the JSON serialization / de-serialization process */
    public Attribute() {}

    public Attribute(Integer server_id, AttributeType type, String name) {
        this.server_id = server_id;
        this.name = name;
        setType(type);

    }

    public List<Attribute> getNestedAttributes() {
        return nestedAttributes;
    }

    public void addNestedAttribute(Integer id, AttributeType type, String name) {
        if (nestedAttributes == null) {
            nestedAttributes = new ArrayList<Attribute>();
        }
        nestedAttributes.add(new Attribute(id, type, name));

    }

	public String getOptionValue(int index) {
		return options != null && options.length > index ? options[index].value : null;
	}
	
	public void addOption(String value) {
		if (options == null) {
			options = new AttributeOption[1];
			options[0] = new AttributeOption();
			options[0].value = value;
		}
		else {
			throw new IllegalArgumentException("Cannot add options to an Attribute with defined options");
		}
	}
	
	public AttributeType getType() {
		return AttributeType.fromCode(typeCode);
	}
	
	public void setType(AttributeType type) {
		typeCode = type.getCode();
	}
	
	public boolean isModeratorAttribute() {
		return (scope != null && scope.contains("MODERATION"));
	}
	
	public boolean equals(Attribute attribute) {
		if (attribute == null || attribute.getServerId() == null) {
			return false;
		}
		return attribute.getServerId().equals(getServerId());
	}
	
	public int hashCode() {
		if (getServerId() == null) {
			return -1;
		}
		return getServerId();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Attribute: ").append(getType().name);
		return builder.toString();
	}
}
