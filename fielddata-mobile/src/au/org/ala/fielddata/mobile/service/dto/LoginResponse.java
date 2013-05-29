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

package au.org.ala.fielddata.mobile.service.dto;

import au.org.ala.fielddata.mobile.model.User;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

	public class Portal {
		public String name;
		public String path;
	}
	
	public Portal portal;
	
	public String ident;
	@SerializedName("portal_id")
	public Integer portalId;

	public User user;

	public String toString() {

		StringBuilder loginResponse = new StringBuilder();
		loginResponse.append("Ident: ").append(ident).append(", portal: ").append(portalId);
		loginResponse.append(", user: "+user.toString());
		
		return loginResponse.toString();
	}
	
}
