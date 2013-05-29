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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.pref.Preferences;
import au.org.ala.fielddata.mobile.service.dto.DownloadSpeciesResponse;
import au.org.ala.fielddata.mobile.service.dto.DownloadSurveyResponse;
import au.org.ala.fielddata.mobile.service.dto.SyncRecordsResponse;
import au.org.ala.fielddata.mobile.service.dto.UserSurveyResponse;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

/** 
 * The FieldDataServiceClient provides the interface to the FieldData 
 * web services.
 */
public class FieldDataServiceClient extends WebServiceClient {

	private String syncUrl = "/survey/upload";

	private String surveyUrl = "/survey/list?ident=";
	
	private String speciesUrl = "/species/speciesForSurvey?ident=%s&surveyId=%d&first=%d&maxResults=%d";

	private String surveyDetails = "/survey/%d?ident=%s&surveysOnDevice=%s";
	
	private String pingUrl = "/survey/ping";
	
	private String downloadUrl = "/survey/download?uuid=";
	

	private String ident;
	
	/**
	 * Because the Survey web service also downloads species, this is 
	 * just a holder for the results of the download.
	 */
	public class SurveysAndSpecies {
		
		public SurveysAndSpecies(List<Survey> surveys, List<Species> species) {
			this.surveys = surveys;
			this.species = species;
		}
		
		public List<Survey> surveys;
		public List<Species> species;
	}

	public FieldDataServiceClient(Context ctx) {
		super(ctx);
		this.ident = new Preferences(ctx).getFieldDataSessionKey();
	}

	public void sync(List<Record> records) throws Exception {

		MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>();
		params.set("ident", ident);
		params.set("inFrame", "false"); // Setting this parameter prevents the
										// server from assuming a jsonp request.
	    params.set("syncData", records);

		String url = getServerUrl() + syncUrl;

		RestTemplate restTemplate = getRestTemplate();
		SyncRecordsResponse result = restTemplate.postForObject(url, params,
				SyncRecordsResponse.class);
		
	}

	public List<Survey> downloadSurveys() {

		String url = getServerUrl() + surveyUrl + ident;

		RestTemplate restTemplate = getRestTemplate();
		restTemplate.getMessageConverters().add(
				new StringHttpMessageConverter());
		UserSurveyResponse[] result = restTemplate.getForObject(url,
				UserSurveyResponse[].class);

		url = getServerUrl() + surveyDetails;
		List<Survey> surveys = new ArrayList<Survey>();
		List<Integer> speciesIdList = new ArrayList<Integer>();
		//List<Species> species = new ArrayList<Species>();
		for (UserSurveyResponse userSurvey : result) {
			String downloadedSurveys = jsonSurveyIds(surveys);
			DownloadSurveyResponse surveyResponse = restTemplate.getForObject(
					String.format(url, userSurvey.id, ident, downloadedSurveys),
					DownloadSurveyResponse.class);
			
			Survey survey = mapSurvey(surveyResponse);
			surveys.add(survey);

			List<Integer> speciesIds = surveyResponse.details.speciesIds;
			if (speciesIds != null) {
				speciesIdList.addAll(surveyResponse.details.speciesIds);
			}
			//species.addAll(downloadSpecies(survey));
		}
		System.out.println(speciesIdList);
		
		return surveys;
	}
	
	public List<Species> downloadSpecies(Survey survey, List<Integer> downloadedSurveys, int first, int maxResults) {
		
		String url = getServerUrl() + String.format(speciesUrl, ident, survey.server_id, first, maxResults);
		for (Integer id : downloadedSurveys) {
			url += "&surveysOnDevice="+id;
		}

		RestTemplate restTemplate = getRestTemplate();
		DownloadSpeciesResponse response = restTemplate.getForObject(url, DownloadSpeciesResponse.class);
		
		return response.list;
	}

	private Survey mapSurvey(DownloadSurveyResponse surveyResponse) {
		Survey survey = new Survey();

		survey.server_id = surveyResponse.details.id;
		survey.lastSync = System.currentTimeMillis();
		survey.name = surveyResponse.details.name;
		survey.attributes = surveyResponse.attributes;
		survey.recordProperties = surveyResponse.recordProperties;
		survey.map = surveyResponse.map;
		survey.description = surveyResponse.details.description;
		survey.speciesIds = surveyResponse.details.speciesIds;
		survey.imageUrl = surveyResponse.imageUrl;
		return survey;
	}
	
	private String jsonSurveyIds(List<Survey> surveys) {
		JSONArray ids = new JSONArray();
		for (Survey survey : surveys) {
			ids.put(survey.server_id);
		}
		return ids.toString();
	}
	
	public boolean ping(int timeoutInMillis) {
		
		InputStream in = null;
		HttpURLConnection conn = null;
		boolean canPing = true;
		try {
			URL url = new URL(getServerUrl() + pingUrl);
			conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(timeoutInMillis);
			//conn.setReadTimeout(timeoutInMillis);
			in = conn.getInputStream();
			while (in.read() != -1) {}
			
		} catch (Exception e) {
			canPing = false;
		} finally {
			try {
				close(in);
				close(conn);
			} catch (Exception e) {
				canPing = false;
			}
		}
		return canPing;	
	}

	/**
	 * Downloads the species profile in the background so it is cached
	 * when it is required to be used.
	 * @param uuid identifies the image to download.
	 */
	public void downloadSpeciesProfileImage(String uuid) {
		String url = getServerUrl()+downloadUrl+uuid;
		Log.d("FieldDataServiceClient", "downloading species: "+uuid);
		ImageLoader.getInstance().loadImage(url, null);
	}
	
	public void loadSurveyImage(ImageView imageView, String imageUrl) {
		String url = getServerUrl()+imageUrl;
		Log.d("FieldDataServiceClient", "loading survey image: "+url);
		ImageLoader.getInstance().displayImage(url, imageView, new SimpleImageLoadingListener() {
						
			public void onLoadingComplete(String arg0, View imageView, Bitmap arg2) {
				imageView.setVisibility(View.VISIBLE);
			}
		});
	}

}
