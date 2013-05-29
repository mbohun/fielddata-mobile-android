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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.AttributeChangeListener;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;

public class TextViewBinder extends AbsBinder implements TextWatcher, AttributeChangeListener {

	private SurveyViewModel model;
	private boolean updating;
	
	public TextViewBinder(Context ctx, TextView view, Attribute attribute, SurveyViewModel model) {
		super(attribute, view);
		this.model = model;
		updating = false;
		view.setText(model.getValue(attribute));
		view.addTextChangedListener(this);
		
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		try {
			updating = true;
			bind(s);
		}
		finally {
			updating = false;
		}
	}

	public void afterTextChanged(Editable s) {}
	
	public void onAttributeChange(Attribute attribute) {
		if (attribute.getServerId() != this.attribute.getServerId()) {
			return;
		}
		if (!updating) {	
			((TextView)view).setText(model.getValue(attribute));
		}
	}
	
	public void bind() {
		bind(nullSafeText());
	}
	
	private void bind(CharSequence text) {
		model.setValue(attribute, text.toString());
	}
	
	private String nullSafeText() {
		CharSequence text = ((TextView)view).getText();
		if (text == null) {
			return "";
		}
		return text.toString();
	}
	
	
}
