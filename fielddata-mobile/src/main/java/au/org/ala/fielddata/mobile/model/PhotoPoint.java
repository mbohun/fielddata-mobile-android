package au.org.ala.fielddata.mobile.model;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by god08d on 19/06/13.
 */
public class PhotoPoint extends WayPoint {

    public Uri photo;

    public PhotoPoint(Location location, String markerId) {
        super(location, markerId);

    }
    public PhotoPoint(Parcel in) {
        super(in);
        photo = in.readParcelable(WayPoint.class.getClassLoader());
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(photo, 0);
    }

    public static final Parcelable.Creator<PhotoPoint> CREATOR = new Parcelable.Creator<PhotoPoint>() {
        public PhotoPoint createFromParcel(Parcel in) {
            return new PhotoPoint(in);
        }
        public PhotoPoint[] newArray(int size) {
            return new PhotoPoint[size];
        }
    };
}
