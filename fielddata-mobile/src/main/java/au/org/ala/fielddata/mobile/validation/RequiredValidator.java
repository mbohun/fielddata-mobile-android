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
package au.org.ala.fielddata.mobile.validation;

import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Record;

public class RequiredValidator implements Validator {

	public ValidationResult validate(Record record, Attribute attribute) {
		String value = record.getValue(attribute);
		
		boolean valid = value != null && !"".equals(value);
		
		if (valid) {
			return new ValidationResult(attribute);
		}
		else {
			return new ValidationResult(attribute, R.string.requiredMessage);
		}
	}
	
	
}
