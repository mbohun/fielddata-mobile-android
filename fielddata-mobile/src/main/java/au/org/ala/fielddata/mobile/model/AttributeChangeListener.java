package au.org.ala.fielddata.mobile.model;

import au.org.ala.fielddata.mobile.validation.Validator.ValidationResult;

public interface AttributeChangeListener {

	public void onAttributeChange(Attribute attribute);
	
	public void onValidationStatusChange(Attribute attribute, ValidationResult result);
}
