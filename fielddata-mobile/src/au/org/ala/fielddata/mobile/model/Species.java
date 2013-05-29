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

import android.os.Parcel;
import android.os.Parcelable;

public class Species extends Persistent implements Parcelable {

	public String scientificName;
	public String commonName;
	private int taxonGroupId;
	private String lsid;
	private String profileImageUUID;
	
	public static class ProfileElement {
		String content;
		String type;
	}
	
	public Species() {
		
	}
	
	public Species(String scientificName, String commonName, int taxonGroupId) {
		this.scientificName = scientificName;
		this.commonName = commonName;
		this.taxonGroupId = taxonGroupId;
		
	}
	
	public String getImageFileName() {
		return profileImageUUID;
	}
	
	public void setImageFileName(String fileName) {
		this.profileImageUUID = fileName;
	}

	public int getTaxonGroupId() {
		return taxonGroupId;
	}
	
	public void setTaxonGroupId(int taxonGroupId) {
		this.taxonGroupId = taxonGroupId;
	}
	
	public String getLsid() {
		return lsid;
	}
	
	public void setLsid(String lsid) {
		this.lsid = lsid;
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		int tmpId = -1;
		if (getId() != null) {
			tmpId  = getId();
		}
		dest.writeInt(tmpId);
		dest.writeInt(server_id);
		dest.writeString(scientificName);
		dest.writeString(commonName);
		dest.writeInt(taxonGroupId);
		dest.writeString(lsid);
	}

	public static final Parcelable.Creator<Species> CREATOR = new Parcelable.Creator<Species>() {
		public Species createFromParcel(Parcel in) {
			return new Species(in);
		}

		public Species[] newArray(int size) {
			return new Species[size];
		}
	};
	
	private Species(Parcel in) {
		setId(in.readInt());
		server_id = in.readInt();
		scientificName = in.readString();
		commonName = in.readString();
		taxonGroupId = in.readInt();
		lsid = in.readString();
	}

}
