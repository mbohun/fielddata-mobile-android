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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.pm.PackageManager;
import android.location.Location;
import android.util.SparseArray;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeOption;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeType;
import au.org.ala.fielddata.mobile.validation.RecordValidator;
import au.org.ala.fielddata.mobile.validation.RecordValidator.RecordValidationResult;
import au.org.ala.fielddata.mobile.validation.Validator.ValidationResult;

public class SurveyViewModel {
	/** Defines the survey we are rendering in this Activity */
	private Survey survey;
	/** The attributes of the survey, split into pages */
	private List<List<Attribute>> attributes;

	/** The data collected for the Survey */
	private Record record;
	
	/** Validates our record */
	private RecordValidator validator;

	/** The currently selected Species - cached here to avoid database access */
	private Species species;

	private PackageManager packageManager;

	private SparseArray<AttributeChangeListener> listeners;
	
	private SparseArray<ValidationResult> validationStatus;
	
	private TempValue tempValue;
	
	private Attribute pointSourceAttribute;

    private boolean usePages;

	
	/**
	 * Compares two Attributes by their weight. Not null safe!
	 */
	static class WeightComparitor implements Comparator<Attribute> {

		public int compare(Attribute lhs, Attribute rhs) {
			return lhs.weight.compareTo(rhs.weight);
		}

	}
	
	public static class TempValue {
		Attribute attribute;
		String value;
		
		public TempValue(Attribute attribute, String value) {
			this.attribute = attribute;
			this.value = value;
		}
		
		public Attribute getAttribute() {
			return attribute;
		}
		
		public String getValue() {
			return value;
		}
	}

	public SurveyViewModel(Survey survey, Record record, PackageManager packageManager, boolean useHrAsPageBreak) {
		this.survey = survey;
		this.record = record;
		this.packageManager = packageManager;
		attributes = new ArrayList<List<Attribute>>();
		listeners = new SparseArray<AttributeChangeListener>();
		validationStatus = new SparseArray<ValidationResult>();
		validator = new RecordValidator();
        usePages = useHrAsPageBreak;
		sortAttributes();
	}

	public int getPageCount() {
		return attributes.size();
	}

	public List<Attribute> getPage(int pageNum) {
		return attributes.get(pageNum);
	}

	public void speciesSelected(Species species) {
		this.species = species;
		record.taxon_id = species.server_id;
		record.scientificName = species.scientificName;
		
		Attribute changed = survey.propertyByType(AttributeType.SPECIES_P);
		
		validate(changed);	
	}
	
	public void setLocation(Location location) {
		record.setLocation(location);
		
		Attribute changed = survey.propertyByType(AttributeType.POINT);
		fireAttributeChanged(changed);
		validate(changed);
		
		updateLocationSource(location);
	}
	
	/**
	 * This method exists to handle the case that the survey will only 
	 * accept Location data via GPS but for some reason GPS is unavailable
	 * to the required accuracy.
	 */
	public void disableLocationValidation() {
		Attribute location = survey.propertyByType(AttributeType.POINT);
		location.required = false;
		// Force a validation to remove any existing validation message
		// displayed on the location field.
		validate(location);
	}
	
	private void updateLocationSource(Location location) {
		if (location != null && pointSourceAttribute != null) {
			for (AttributeOption option : pointSourceAttribute.options) {
				String value = option.value == null ? "" : option.value;
				if (value.equalsIgnoreCase(location.getProvider())) {
					record.setValue(pointSourceAttribute, value);
					fireAttributeChanged(pointSourceAttribute);
					break;
				}
			}
		}
	}

	public Species getSelectedSpecies() {
		return species;
	}

	public Record getRecord() {
		return record;
	}

	public Survey getSurvey() {
		return survey;
	}

	public String getValue(Attribute attribute) {

		return record.getValue(attribute);
	}

	public void setValue(Attribute attribute, String value) {

		record.setValue(attribute, value);
		fireAttributeChanged(attribute);
		validate(attribute);	
	}
	
	public void setValue(Attribute attribute, Date value) {
		record.setValue(attribute, value);
		fireAttributeChanged(attribute);
		validate(attribute);
	}

	public void setAttributeChangeListener(AttributeChangeListener listener, Attribute attribute) {
		listeners.put(attribute.getServerId(), listener);
	}

	public void removeAttributeChangeListener(AttributeChangeListener listener) {
		listeners.delete(listeners.indexOfValue(listener));
	}

	private void fireAttributeChanged(Attribute attribute) {
		AttributeChangeListener listener = listeners.get(attribute.getServerId());
		if (listener != null) {
			listener.onAttributeChange(attribute);
		}
	}
	
	private void fireAttributeValidated(ValidationResult result) {
		AttributeChangeListener listener = listeners.get(result.getAttribute().getServerId());
		if (listener != null) {
			listener.onValidationStatusChange(result.getAttribute(), result);
		}
	}

	private void sortAttributes() {
		List<Attribute> allAttributes = survey.allAttributes();
		Collections.sort(allAttributes, new WeightComparitor());

		List<Attribute> filteredAttributes = new ArrayList<Attribute>(allAttributes.size());

		for (Attribute attribute : allAttributes) {
			if (supports(attribute)) {
				
				if (attribute.name.equals("Treatment_Method")) {
					attribute.setType(AttributeType.CATEGORIZED_MULTI_SELECT);
				}
				else if (attribute.name.equals("Location_Precision")) {
					attribute.setType(AttributeType.POINT_SOURCE);
					pointSourceAttribute = attribute;
				}
				
				filteredAttributes.add(attribute);
			}

			if (usePages && attribute.getType() == AttributeType.HTML_HORIZONTAL_RULE) {
				if (filteredAttributes.size() > 0) {
					attributes.add(filteredAttributes);
					filteredAttributes = new ArrayList<Attribute>(allAttributes.size());
				}
			}
		}
		if (filteredAttributes.size() > 0) {
			attributes.add(filteredAttributes);
		}
	}

	private boolean supports(Attribute attribute) {
		AttributeType type = attribute.getType();
		if (type == null) {
			return false;
		}
		if (attribute.isModeratorAttribute()) {
			return false;
		}
		if ("possible_species".equals(attribute.name)) {
			return false;
		}
 		switch (type) {
		case HTML:
		case HTML_COMMENT:
		case HTML_HORIZONTAL_RULE:
		case HTML_NO_VALIDATION:
			return false;
		case IMAGE:
			return deviceHasCamera();
		
		case LOCATION:
			return false; // We dont' support locations yet, but if we did we'd have to 
			// check if there were any defined for the survey or user.
		case ACCURACY:
			return false; // Accuracy is recorded as a part of the POINT property.
        case TIME:
            return false;  // Time is automatically recorded but we don't yet allow it's explicit selection.
		}
		return true;
	}

	private boolean deviceHasCamera() {
		return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
		packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
	}

	
	public RecordValidationResult validate() {
		List<Attribute> allAttributes = new ArrayList<Attribute>();
		for (List<Attribute> page : attributes) {
			allAttributes.addAll(page);
		}
		
		RecordValidationResult result = validator.validateAll(allAttributes, record);
		record.setValid(result.valid());
		if (!result.valid()) {
			for (ValidationResult attr : result.invalidAttributes()) {
				fireAttributeValidated(attr);
			}
			
		}
		return result;
	}
	
	private void validate(Attribute attribute) {
		ValidationResult result = validator.validateRecordAttribute(attribute, record);
		validationStatus.put(attribute.getServerId(), result);
		fireAttributeValidated(result);
	}
	
	public ValidationResult validationStatus(Attribute attribute) {
		ValidationResult result = validationStatus.get(attribute.getServerId());
		return result != null ? result : new ValidationResult(attribute);
	}

	public int pageOf(Attribute firstInvalid) {
		int firstInvalidPage = -1;
		int pageNum = 0;
		for (List<Attribute> page : attributes) {
			for (Attribute attribute : page) {
				if (firstInvalid.equals(attribute)) {
					firstInvalidPage = pageNum;
					break;
				}
			}
			if (firstInvalidPage > 0) {
				break;
			}
			pageNum++;
		}
		return firstInvalidPage;
	}
	
	public AttributeChangeListener getAttributeListener(Attribute attribute) {
		return listeners.get(attribute.getServerId());
	}

	/**
	 * Temporary storage for the URI we are going to save a photo as. 
	 * This value needs to be persisted in the event the CollectSurveyData
	 * activity is killed while the camera activity is in the foreground. 
	 *
	 * @param attribute the target attribute for the temp value.
     * @param value the value to store temporarily.
	 */
	public void setTempValue(Attribute attribute, String value) {
		tempValue = new TempValue(attribute, value);
	}

	public TempValue clearTempValue() {
		TempValue tmp = tempValue;
		tempValue = null;
		return tmp;
	}
	
	public TempValue getTempValue() {
		return tempValue;
	}
	
	public void persistTempValue() {
		
		if (tempValue != null) {
			setValue(tempValue.getAttribute(), tempValue.getValue());
		}
		tempValue = null;
	}

	public Location getLocation() {
		return record.getLocation();
	}
	
//	public void setWayPoints(WayPoints wayPoints) {
//		// Gotta do me some magic here.
//		String location = wayPoints.verticiesToWKT();
//		List<WayPoint> photopointList = wayPoints.getPhotoPoints();
//        PhotopointMapper mapper = new PhotopointMapper(survey);
//        mapper.map(record, photopointList.get(0), survey.getAttribute(wayPoints.getPhotoPointAttribute()));
//	}

}
