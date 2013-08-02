package au.org.ala.fielddata.mobile.model;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class WayPoints implements Parcelable {
	
	private List<PhotoPoint> photoPoints;
	private List<WayPoint> verticies;
	private boolean polygonClosed;
    private int photoPointAttribute;
	
	public WayPoints(int photoPointAttribute) {
		photoPoints = new ArrayList<PhotoPoint>();
		verticies = new ArrayList<WayPoint>();
		polygonClosed = false;
        this.photoPointAttribute = photoPointAttribute;
	}
	
	public WayPoints(Parcel in) {
		photoPoints = new ArrayList<PhotoPoint>();
		in.readTypedList(photoPoints, PhotoPoint.CREATOR);
		verticies = new ArrayList<WayPoint>();
		in.readTypedList(verticies, WayPoint.CREATOR);
		boolean[] wrapper = new boolean[1];
		in.readBooleanArray(wrapper);
		polygonClosed = wrapper[0];
        photoPointAttribute = in.readInt();
		
	}

    public void setPhotoPointAttribute(int photoPointAttribute) {
        this.photoPointAttribute = photoPointAttribute;
    }
	
	public String verticiesToWKT() {
		if (verticies.size() == 0) {
			return "";
		}
		else if (verticies.size() == 1) {
			return "POINT ("+verticiesToText()+")";
		}
		else if (polygonClosed) {
			return "MULTIPOLYGON (("+verticiesToText()+"))";
		}
		else {
			return "MULTILINESTRING ("+verticiesToText()+")";
		}
	}
	
	private String verticiesToText() {
		if (verticies.isEmpty()) {
			return "";
		}
		StringBuilder coordinates = new StringBuilder();
		for (int i=0; i<verticies.size()-1; i++) {
			WayPoint coordinate = verticies.get(i);
			coordinates.append(coordinate.toWKT());
			coordinates.append(",");
		}
		coordinates.append(verticies.get(verticies.size()-1).toWKT());
		return coordinates.toString();
	}
	
	public boolean isClosed() {
		return polygonClosed;
	}
	
	public void setClosed(boolean close) {
		polygonClosed = close;
	}

    public int getPhotoPointAttribute() {
        return photoPointAttribute;
    }
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(photoPoints);
		dest.writeTypedList(verticies);
		dest.writeBooleanArray(new boolean[]{polygonClosed});
        dest.writeInt(photoPointAttribute);
	}
	public int describeContents() {
		return 0;
	}
	
	public List<LatLng> verticies() {
		List<LatLng> coordinates = new ArrayList<LatLng>(verticies.size());
		for (WayPoint wayPoint : verticies) {
			LatLng latlng = wayPoint.coordinate();
			if (latlng != null) {
				coordinates.add(latlng);
			}
		}
		if (polygonClosed && verticies.size() >= 3) {
			coordinates.add(verticies.get(0).coordinate());
		}
		return coordinates;
	}
	
	public List<WayPoint> getVerticies() {
		return verticies;
	}
	
	public List<PhotoPoint> getPhotoPoints() {
		return photoPoints;
	}
	
	public WayPoint findById(String markerId) {
		for (WayPoint wayPoint : verticies) {
			if (markerId.equals(wayPoint.markerId)) {
				return wayPoint;
			}
		}

		return null;
	}

    public PhotoPoint findPhotoPointById(String markerId) {
        for (PhotoPoint wayPoint : photoPoints) {
            if (markerId.equals(wayPoint.markerId)) {
                return wayPoint;
            }
        }
        return null;
    }
	
	public void addVertex(WayPoint vertex) {
		verticies.add(vertex);
	}
	
	public void addPhotoPoint(PhotoPoint photoPoint) {
		photoPoints.add(photoPoint);
	}
	public static final Parcelable.Creator<WayPoints> CREATOR = new Parcelable.Creator<WayPoints>() {
		public WayPoints createFromParcel(Parcel in) {
			return new WayPoints(in);
		}
		public WayPoints[] newArray(int size) {
			return new WayPoints[size];
		}
	};

    public Location getLocation() {
        // TODO fix me
        if (verticies != null && verticies.size() > 0) {
            return verticies.get(0).location;
        }
        return null;
    }

    public void verticiesFromWkt(String locationWkt) {
        if (locationWkt == null) {
            return;
        }

        if (locationWkt.startsWith("POINT")) {
            setClosed(false);
        }
        else if (locationWkt.startsWith("MULTIPOLYGON")) {
            setClosed(true);
        }
        else if (locationWkt.startsWith("MULTILINESTRING")) {
            setClosed(false);
        }
        else {
            throw new IllegalArgumentException("Cannot create WayPoints from badly formed WKT : "+locationWkt);
        }
        // Because we only support a small subset of geometries, we can do the parsing in
        // a simplistic manner.
        decode(locationWkt.substring(locationWkt.lastIndexOf("("), locationWkt.indexOf(")")));
    }

    private void decode(String wktCoordinates) {
        String[] coordinates = wktCoordinates.split(",");
        for (String coordinate : coordinates) {
            addVertex(new WayPoint(coordinate));
        }
    }


}