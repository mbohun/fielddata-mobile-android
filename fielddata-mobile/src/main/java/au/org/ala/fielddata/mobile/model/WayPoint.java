package au.org.ala.fielddata.mobile.model;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class WayPoint implements Parcelable {
	public Location location;

	public String markerId;
	
	public WayPoint(String coordinates) {
        markerId = null;
        fromWkt(coordinates);
    }
	public WayPoint(Location location, String markerId) {
        this.location = location;
        this.markerId = markerId;

    }
    public WayPoint(Parcel in) {
        location = in.readParcelable(WayPoint.class.getClassLoader());
        markerId = in.readString();
    }
	
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(location, 0);
		dest.writeString(markerId);
	}
	public LatLng coordinate() {
		if (location == null) {
			return null;
		}
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
	
	public String toWKT() {
		StringBuilder coordinates = new StringBuilder();
		coordinates.append(location.getLongitude());
		coordinates.append(" ");
		coordinates.append(location.getLatitude());
		return coordinates.toString();
	}

    public void fromWkt(String coordinates) {
        String[] latLng = coordinates.split(" ");
        location = new Location("Saved Value");
        location.setLongitude(Double.parseDouble(latLng[0]));
        location.setLatitude(Double.parseDouble(latLng[1]));

    }
	
	public static final Parcelable.Creator<WayPoint> CREATOR = new Parcelable.Creator<WayPoint>() {
		public WayPoint createFromParcel(Parcel in) {
			return new WayPoint(in);
		}
		public WayPoint[] newArray(int size) {
			return new WayPoint[size];
		}
	};
	
	
}