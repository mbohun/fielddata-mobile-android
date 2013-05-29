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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeOption;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;

/**
 * The SpinnerBinder is responsible for keeping the state of a Spinner
 * widget in sync with the attribute value stored in the SurveyViewModel.
 */
public class SpinnerBinder extends AbsBinder implements OnItemSelectedListener, OnTouchListener {

	private SurveyViewModel model;
	private boolean updating;
	private boolean bindEnabled;
	

	public SpinnerBinder(Context ctx, Spinner view, Attribute attribute, SurveyViewModel model) {
		super(attribute, view);
		this.model = model;
		updating = false;
		bindEnabled = false;
		
		update();
		view.setOnItemSelectedListener(this);
		view.setOnTouchListener(this);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (bindEnabled) {
			
			bind();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
		if (bindEnabled) {
			bind("");
		}
	}
	
	
	/**
	 * The reason this is necessary is spinners fire onItemSelected events
	 * automatically when they are first layed out (including after an 
	 * orientation change for example).
	 * We don't actually want to trigger validation unless the user has
	 * interacted with the Spinner (or the user has pressed Save).
	 */
	public boolean onTouch(View v, MotionEvent event) {
		bindEnabled = true;
		return false;
	}

	public void onAttributeChange(Attribute attribute) {
		if (attribute.getServerId() != this.attribute.getServerId()) {
			return;
		}
		update();
	}

	private void update() {
		try {
			updating = true;
			final Spinner spinner = (Spinner)view;
			String value = model.getValue(attribute);
			
			if (value != null) {
				SpinnerAdapter adapter = (SpinnerAdapter)spinner.getAdapter();
				for (int i=0; i<adapter.getCount(); i++) {
					Object tmpValue = adapter.getItem(i);
					
					if (tmpValue instanceof AttributeOption) {
						AttributeOption option = (AttributeOption)tmpValue;
						if (value.equals(option.value)) {
							final int selectedIndex = i;
							spinner.post(new Runnable() {
								public void run() {
									spinner.setSelection(selectedIndex);
							    }
							});
								
							break;
						}
					}
					// CategorizedSpinners use Strings as they need to store
					// header labels also.
					else if (tmpValue instanceof String) {
						String option = (String)tmpValue;
						if (value.equals(option)) {
							final int selectedIndex = i;
							spinner.post(new Runnable() {
								public void run() {
									spinner.setSelection(selectedIndex);
							    }
							});
								
							break;
						}
					}
					
				}
			}
		}
		finally {
			updating = false;
		}
	}
	
	public void bind() {
		if (!updating) {
			
			bindEnabled = true;
			bind(nullSafeText());
		}
	}
	
	private void bind(String value) {
		model.setValue(attribute, value);
	}

	private String nullSafeText() {
		Object value =  ((Spinner)view).getSelectedItem();
		if (value == null) {
			return "";
		}
		return value.toString();
	}

}
