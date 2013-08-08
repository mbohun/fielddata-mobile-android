package au.org.ala.fieldcapture.mobile.test;

import android.location.Location;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.AttributeValues;
import au.org.ala.fielddata.mobile.model.PhotoPoint;
import au.org.ala.fielddata.mobile.model.PhotopointMapper;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Survey;

/**
 * Tests the PhotoPointMapper class.
 */
public class PhotoPointMapperTest extends AndroidTestCase {

    private static final int TEXT_ATTRIBUTE_ID = 1;
    private static final int INTEGER_ATTRIBUTE_ID = 2;
    private static final int PHOTOPOINT_ATTRIBUTE_ID = 3;

    private Survey survey;
    private PhotopointMapper mapper;

    public void setUp() throws Exception {
        super.setUp();

        survey = createPhotoPointSurvey(1);
        mapper = new PhotopointMapper(survey);
    }

    @MediumTest
    public void testSinglePhotoPointMapping() {

        Record record = createRecord();
        List<PhotoPoint> photoPoints = new ArrayList<PhotoPoint>();
        String file1 = "file:///tmp/1.jpg";
        photoPoints.add(createPhotoPoint(32.0, -150.0, file1));


        mapper.map(record, photoPoints, survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));

        List<AttributeValues> values = record.getNestedValues(survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));

        Assert.assertEquals(1, values.size());

        checkMappedAttributes(0, file1, "32.0", "-150.0", values);

        List<PhotoPoint> mappedPhotoPoints = mapper.map(record, survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));
        Assert.assertEquals(1, mappedPhotoPoints.size());

        checkMappedPhotoPoint(0, file1, 32.0, -150.0, mappedPhotoPoints);
    }

    @MediumTest
    public void testMultiplePhotoPointMapping() {

        Record record = createRecord();
        List<PhotoPoint> photoPoints = new ArrayList<PhotoPoint>();
        String file1 = "file:///tmp/1.jpg";
        photoPoints.add(createPhotoPoint(32.0, -150.0, file1));
        photoPoints.add(createPhotoPoint(33.0, -151.0, "file:///tmp/2.jpg"));
        photoPoints.add(createPhotoPoint(34.0, -152.0, "file:///tmp/3.jpg"));

        mapper.map(record, photoPoints, survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));

        List<AttributeValues> values = record.getNestedValues(survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));

        Assert.assertEquals(3, values.size());

        checkMappedAttributes(0, file1, "32.0", "-150.0", values);
        checkMappedAttributes(1, "file:///tmp/2.jpg", "33.0", "-151.0", values);
        checkMappedAttributes(2, "file:///tmp/3.jpg", "34.0", "-152.0", values);


        List<PhotoPoint> mappedPhotoPoints = mapper.map(record, survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));
        Assert.assertEquals(3, mappedPhotoPoints.size());

        checkMappedPhotoPoint(0, file1, 32.0, -150.0, mappedPhotoPoints);
        checkMappedPhotoPoint(1, "file:///tmp/2.jpg", 33.0, -151.0, mappedPhotoPoints);
        checkMappedPhotoPoint(2, "file:///tmp/3.jpg", 34.0, -152.0, mappedPhotoPoints);

    }

    private void checkMappedAttributes(int index, String expectedFile, String expectedLat, String expectedLon, List<AttributeValues> values) {
        AttributeValues photoPoint = values.get(index);
        Assert.assertEquals(index, photoPoint.getRow());
        List<Record.AttributeValue> details = photoPoint.getAttributeValues();

        String lat = valueByAttributeName(PhotopointMapper.LATITUDE, details);
        Assert.assertEquals(expectedLat, lat);
        String lon = valueByAttributeName(PhotopointMapper.LONGITUDE, details);
        Assert.assertEquals(expectedLon, lon);

        String photoUri = valueByAttributeName("photo", details);
        Assert.assertEquals(expectedFile, photoUri);
    }


    private void checkMappedPhotoPoint(int index, String expectedFile, double expectedLat, double expectedLon, List<PhotoPoint> photoPoints) {
        PhotoPoint photoPoint1 = photoPoints.get(index);
        Assert.assertEquals(expectedLat, photoPoint1.coordinate().latitude);
        Assert.assertEquals(expectedLon, photoPoint1.coordinate().longitude);
        Assert.assertEquals(expectedFile, photoPoint1.photo.toString());
    }

    private String valueByAttributeName(String name, List<Record.AttributeValue> values) {
        for (Record.AttributeValue value : values) {
            Attribute attribute = survey.getAttribute(value.attribute_id);
            if (attribute.name.equals(name)) {
                return value.nullSafeValue();
            }
        }
        return null;
    }


    private Survey createPhotoPointSurvey(int id) {
        Survey survey = createSimpleSurvey(id);

        survey.addAttribute(Attribute.AttributeType.CENSUS_METHOD_ROW, "photoPointAttribute", PHOTOPOINT_ATTRIBUTE_ID);
        Attribute photoPointAttribute = survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID);

        photoPointAttribute.addNestedAttribute(4, Attribute.AttributeType.DECIMAL, PhotopointMapper.LATITUDE);
        photoPointAttribute.addNestedAttribute(5, Attribute.AttributeType.DECIMAL, PhotopointMapper.LONGITUDE);
        photoPointAttribute.addNestedAttribute(6, Attribute.AttributeType.DECIMAL, PhotopointMapper.BEARING);
        photoPointAttribute.addNestedAttribute(7, Attribute.AttributeType.DATE, PhotopointMapper.DATE);
        photoPointAttribute.addNestedAttribute(8, Attribute.AttributeType.IMAGE, "photo");

        return survey;
    }

    private Survey createSimpleSurvey(int id) {
        Survey survey = new Survey();
        survey.server_id = id;
        survey.setId(id);

        survey.addAttribute(Attribute.AttributeType.TEXT, "textAttribute", TEXT_ATTRIBUTE_ID);
        survey.addAttribute(Attribute.AttributeType.INTEGER, "integerAttribute", INTEGER_ATTRIBUTE_ID);

        return survey;
    }

    private Record createRecord() {
        Record record = new Record();

        record.created = System.currentTimeMillis();
        record.survey_id = survey.server_id;
        record.setValue(survey.getAttribute(TEXT_ATTRIBUTE_ID), "TextAttributeValue");
        record.setValue(survey.getAttribute(INTEGER_ATTRIBUTE_ID), "1");
        return record;
    }

    private PhotoPoint createPhotoPoint(double lat, double lon, String fileName) {
        Location location = new Location("Test");
        location.setLatitude(lat);
        location.setLongitude(lon);

        PhotoPoint photoPoint = new PhotoPoint(location);
        photoPoint.photo = Uri.parse(fileName);

        return photoPoint;

    }
}
