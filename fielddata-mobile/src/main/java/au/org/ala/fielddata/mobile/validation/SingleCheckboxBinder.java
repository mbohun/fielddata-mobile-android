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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;

public class SingleCheckboxBinder extends AbsBinder implements OnCheckedChangeListener {

	private SurveyViewModel model;
	
	public SingleCheckboxBinder(Context ctx, CheckBox view, Attribute attribute, SurveyViewModel model) {
		super(attribute, view);
		this.model = model;
		view.setChecked(Boolean.valueOf(model.getValue(attribute)));
		view.setOnCheckedChangeListener(this);
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		model.setValue(attribute, Boolean.toString(isChecked));
	}
	
	public void onAttributeChange(Attribute attribute) {
		
		/*
		if (attribute.getServerId() != this.attribute.getServerId()) {
			return;
		}*/
		
	}
	
	public void bind() {
		bind(nullSafeText());
	}
	
	private void bind(String text) {
		model.setValue(attribute, text.toString());
	}
	
	private String nullSafeText() {
		CharSequence text = Boolean.toString(((CheckBox)view).isChecked());
		if (text == null) {
			return "false";
		}
		return text.toString();
	}


	
	
	
}
