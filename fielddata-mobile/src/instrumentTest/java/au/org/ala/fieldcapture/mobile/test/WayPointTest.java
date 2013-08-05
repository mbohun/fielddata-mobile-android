package au.org.ala.fieldcapture.mobile.test;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import java.util.List;

import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.AttributeValues;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Survey;
import au.org.ala.fielddata.mobile.model.WayPoint;

/**
 * Tests the WayPoint class.
 */
public class WayPointTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    @MediumTest
    public void testWktConversion() {

        double lat = 32.0;
        double lon = 154.0;
        Location location = new Location("Test");
        location.setLatitude(lat);
        location.setLongitude(lon);

        WayPoint wayPoint = new WayPoint(location, null);

        String wkt = wayPoint.toWKT();

        Assert.assertEquals("154.0 32.0", wkt);

        WayPoint wayPoint2 = new WayPoint(wkt);
        LatLng latLng = wayPoint2.coordinate();

        Assert.assertEquals(lat, latLng.latitude);
        Assert.assertEquals(lon, latLng.longitude);

    }

}
