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

import android.content.Context;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Record;


public interface Validator {

	public static final int VALID = 0;
	public static final int INVALID = 1;
	
	public static class ValidationResult {
		private Attribute attribute;
		private int error;
		private boolean valid;
		
		public ValidationResult(Attribute attribute) {
			valid = true;
			this.attribute = attribute;
		}
		
		public ValidationResult(Attribute attribute, int errorCode) {
			valid = false;
			this.attribute = attribute;
			this.error = errorCode;
		}
		
		public boolean isValid() {
			return valid;
		}
		
		public int getMessageId() {
			return error;
		}
		public CharSequence getMessage(Context context) {
			
			return context.getResources().getString(error);
			
		}

		public Attribute getAttribute() {
			return attribute;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(attribute).append(": ");
			if (valid) {
				builder.append("valid");
			}
			else {
				builder.append("invalid");
			}
			return builder.toString();
		}
		
	}
	
	public ValidationResult validate(Record record, Attribute attribute);
}
