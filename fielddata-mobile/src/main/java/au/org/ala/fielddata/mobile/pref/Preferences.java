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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Manages access to the application preferences.
 */
public class Preferences {

	private static final String SURVEY_KEY = "SurveyPreference";
	private static final String SURVEY_NAME_KEY = "SurveyNamePreference";
	private static final String SESSION_KEY = "Session";
	private static final String AUTO_UPLOAD_PREFERENCE_KEY = "AutomaticUpload";
	private static final Boolean DEFAULT_AUTO_UPLOAD_PREFERENCE = Boolean.TRUE;
	private static final String WIFI_ONLY_PREFERENCE_KEY = "WifiOnly";
	private static final Boolean DEFAULT_WIFI_ONLY_PREFERENCE = Boolean.TRUE;
	private static final String ASKED_ABOUT_WIFI_PREFERENCE_KEY = "AskedAboutWifi";
	
	private Context ctx; 

	public Preferences(Context ctx) {
		this.ctx = ctx;
	}
	
	public void setCurrentSurvey(Integer id) {
		Editor preferences = preferencesEditor();
		preferences.putInt(SURVEY_KEY, id).commit();
	}
	
	public Integer getCurrentSurvey() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(SURVEY_KEY, -1);
	}
	
	public void setCurrentSurveyName(String name) {
		Editor preferences = preferencesEditor();
		preferences.putString(SURVEY_NAME_KEY, name).commit();
	
	}
	
	public String getCurrentSurveyName() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(SURVEY_NAME_KEY, "No survey");
	}
	
	private Editor preferencesEditor() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).edit();
	}

	public String getFieldDataServerUrl() {
		return getFieldDataServerUrl(true, false);
	}
	
	public String getFieldDataServerUrl(boolean includePortalPath, boolean secure) {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String hostName = prefs.getString("serverHostName", "");
		String context = prefs.getString("contextName", "");
		String path = "";
		if (includePortalPath) {
			path = prefs.getString("path", "");
		}
		
		StringBuilder url = new StringBuilder();
		if (secure) {
			url.append("https://");
		}
		else {
			url.append("http://");
		}
		url.append(hostName).append("/").append(context);
		
		if (path.length() > 0) {
			url.append("/").append(path);
		}
		
		return url.toString();
	}
	
	
	/**
	 * Returns the URL of the un-proxied field data server. (this should really
	 * be downloaded by the proxy, but it's not at this point).
	 */
	public String getUnproxiedFieldDataServerUrl() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String hostName = prefs.getString("serverHostName", "");
		String context = "bdrs-core";//prefs.getString("contextName", "");
		String path = prefs.getString("path", "");
		
		StringBuilder url = new StringBuilder();
		url.append("http://").append(hostName).append("/").append(context);
		
		if (path.length() > 0) {
			url.append("/").append(path);
		}
		
		return url.toString();
	}
	
	public String getReviewUrl() {
		return getUnproxiedFieldDataServerUrl()+"/review/sightings/advancedReview.htm?u=%s";
	}
	
	public String getFieldDataServerHostName() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String hostName = prefs.getString("serverHostName", "");
		return hostName;
	}
	
	public String getFieldDataContextName() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String contextName = prefs.getString("contextName", "");
		return contextName;
	}
	
	public String getFieldDataPath() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String portalName = prefs.getString("path", "");
		return portalName;
	}
	
	public void setFieldDataPath(String path) {
		Editor preferences = preferencesEditor();
		preferences.putString("path", path).commit();
	}
	
	public String getFieldDataPortalName() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ctx);
		String portalName = prefs.getString("portalName", "");
		return portalName;
	}
	
	public void setFieldDataPortalName(String name) {
		Editor preferences = preferencesEditor();
		preferences.putString("portalName", name).commit();
	}
	
	public String getFieldDataSessionKey() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(SESSION_KEY, null);
	}
	
	public void setFieldDataSessionKey(String sessionKey) {
		Editor preferences = preferencesEditor();
		preferences.putString(SESSION_KEY, sessionKey).commit();
	
	}
	
	public boolean getUploadAutomatically() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				AUTO_UPLOAD_PREFERENCE_KEY, DEFAULT_AUTO_UPLOAD_PREFERENCE);
	}
	
	public boolean getUploadOverWifiOnly() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				WIFI_ONLY_PREFERENCE_KEY, DEFAULT_WIFI_ONLY_PREFERENCE);
	}
	
	public void setUploadOverWifiOnly(boolean wifiOnly) {
		Editor preferences = preferencesEditor();
		preferences.putBoolean(WIFI_ONLY_PREFERENCE_KEY, wifiOnly).commit();
	}
	
	public boolean getAskedAboutWifi() {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				ASKED_ABOUT_WIFI_PREFERENCE_KEY, false);
	}
	
	public void setAskedAboutWifi() {
		Editor preferences = preferencesEditor();
		preferences.putBoolean(ASKED_ABOUT_WIFI_PREFERENCE_KEY, true).commit();
		
	}
	
}
