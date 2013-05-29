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

import java.util.List;

import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Record;

/**
 * Performs a sequence of validations, terminating after the first failure
 * or all are successful.
 */
public class CompositeValidator implements Validator {

	private List<Validator> validators;
	
	public CompositeValidator(List<Validator> validators) {
		this.validators = validators;
	}
	
	
	public ValidationResult validate(Record record, Attribute attribute) {
		
		for (Validator validator : validators) {
			ValidationResult result = validator.validate(record, attribute);
			if (!result.isValid()) {
				return result;
			}
		}
		
        return new ValidationResult(attribute);
	}
	
}
