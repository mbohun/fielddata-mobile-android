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


public class User extends Persistent {

	public String firstName;
	public String lastName;
	public String emailAddress;

	public String toString() {
		StringBuilder user = new StringBuilder();
		user.append("Name: ").append(firstName).append(lastName).append(", email: ").append(emailAddress);
		return user.toString();
	}
	
}
