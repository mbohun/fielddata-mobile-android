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

package au.org.ala.fielddata.mobile.pref;

import java.util.List;

import android.os.Build;
import android.os.Bundle;
import au.org.ala.fielddata.mobile.nrmplus.R;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class EditPreferences extends SherlockPreferenceActivity {

	@SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        	addPreferencesFromResource(R.xml.network_preferences);
        	addPreferencesFromResource(R.xml.preference1);
        	
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
    	loadHeadersFromResource(R.xml.preference_headers, target);
    }
}
