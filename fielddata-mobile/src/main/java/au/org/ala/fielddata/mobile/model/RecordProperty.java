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


public class RecordProperty extends Attribute {


	public AttributeType getType() {
		return AttributeType.fromCode(name);
	}
	
	public Integer getServerId() {
		return getType().ordinal()*-1;
	}
	
	public boolean equals(RecordProperty attribute) {
		if (attribute == null || attribute.name == null) {
			return false;
		}
		return attribute.name.equals(name);
	}
	
	public boolean equals(Attribute attribute) {
		if (!(attribute instanceof RecordProperty)) {
			return false;
		}
		return equals((RecordProperty)attribute);
	}
	
	public int hashCode() {
		if (name == null) {
			return -1;
		}
		return name.hashCode();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Record property: ").append(name);
		return builder.toString();
	}
}
