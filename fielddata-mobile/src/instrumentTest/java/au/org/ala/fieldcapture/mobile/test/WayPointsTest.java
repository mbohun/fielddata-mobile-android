package au.org.ala.fieldcapture.mobile.test;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import java.util.List;

import au.org.ala.fielddata.mobile.model.WayPoint;
import au.org.ala.fielddata.mobile.model.WayPoints;

/**
 * Tests the WayPoints class.
 */
public class WayPointsTest extends AndroidTestCase {

    private static final int PHOTO_POINT_ATTRIBUTE = 1;

    public void setUp() throws Exception {
        super.setUp();
    }

    @MediumTest
    public void testWktConversion() {

        double[][] verticies = {{1,2}, {3,4}, {5,6}, {7,8}};

        WayPoints wayPoints = new WayPoints(PHOTO_POINT_ATTRIBUTE);

        for (double[] vertex : verticies) {
            addVertex(vertex, wayPoints);
        }
        wayPoints.setClosed(false);

        String wkt = wayPoints.verticiesToWKT();
        Assert.assertEquals("MULTILINESTRING ((2.0 1.0, 4.0 3.0, 6.0 5.0, 8.0 7.0))", wkt);

        wayPoints.verticiesFromWkt(wkt);
        List<LatLng> latLngList = wayPoints.verticies();
        checkCoordinates(verticies, latLngList);

        wayPoints.setClosed(true);

        wkt = wayPoints.verticiesToWKT();
        Assert.assertEquals("MULTIPOLYGON (((2.0 1.0, 4.0 3.0, 6.0 5.0, 8.0 7.0)))", wkt);

        wayPoints.verticiesFromWkt(wkt);
        latLngList = wayPoints.verticies();
        // There is an extra vertex added in for polygons to display a closed shape on the screen.
        checkCoordinates(verticies, latLngList.subList(0, latLngList.size()-1));


    }

    private void checkCoordinates(double[][] expected, List<LatLng> actual) {
        Assert.assertEquals(expected.length, actual.size());
        int i=0;
        for (LatLng latLng : actual) {
            Assert.assertEquals(expected[i][0], latLng.latitude, 0.001f);
            Assert.assertEquals(expected[i][1], latLng.longitude, 0.001f);
            i++;
        }

    }

    private void addVertex(double[] latlng, WayPoints wayPoints) {
        Location loc = new Location("");
        loc.setLatitude(latlng[0]);
        loc.setLongitude(latlng[1]);

        wayPoints.addVertex(new WayPoint(loc, ""));
    }

}
