package au.org.ala.fielddata.mobile.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MapDefaults implements Parcelable {
	
	public static class MapCentre {
		public Double x;
		public Double y;
	}
	public int zoom;
	public MapCentre center;
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(zoom);
		if (center != null) {
			
			dest.writeDouble(center.x);
			dest.writeDouble(center.y);
		}
		else {
			dest.writeDouble(Double.MAX_VALUE);
			dest.writeDouble(Double.MAX_VALUE);
		}
	}
	public static final Parcelable.Creator<MapDefaults> CREATOR = new Parcelable.Creator<MapDefaults>() {
		public MapDefaults createFromParcel(Parcel in) {
			return new MapDefaults(in);
		}

		public MapDefaults[] newArray(int size) {
			return new MapDefaults[size];
		}
	};
	
	private MapDefaults(Parcel in) {
		zoom = in.readInt();
		double x = in.readDouble();
		double y = in.readDouble();
		
		if (x != Double.MAX_VALUE && y != Double.MAX_VALUE) {
			center = new MapCentre();
			center.x = x;
			center.y = y;
		}
	}
	
}