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
package au.org.ala.fielddata.mobile.service;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import android.content.Context;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.dto.LoginResponse;


public class LoginService extends WebServiceClient {

	
	private String loginUrl = "/survey/login";
	
	
	public LoginService(Context ctx) {
		super(ctx);
	}
	
	public LoginResponse login(String username, String password, String portalName) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		
		params.set("portalName", portalName);
		params.set("username", username);
		params.set("password", password);
		
		String url = getServerUrl() + loginUrl;

        LoginResponse result = null;
        try {
            result = getRestTemplate().postForObject(url, params, LoginResponse.class);
        }
        catch (NullPointerException e) {
            // We are getting SSL connection reset errors - at least in the internal network
            // which manifests itself as a NPE.  A retry is usually all that is required to fix it.
            result = getRestTemplate().postForObject(url, params, LoginResponse.class);
        }
		Preferences prefs = new Preferences(ctx);
		prefs.setFieldDataSessionKey(result.ident);
		prefs.setFieldDataPath(result.portal.path);
		prefs.setFieldDataPortalName(result.portal.name);
		
		return result;
	}

}
