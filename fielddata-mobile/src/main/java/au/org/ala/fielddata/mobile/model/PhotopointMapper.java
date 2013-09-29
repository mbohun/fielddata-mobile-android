package au.org.ala.fielddata.mobile.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Maps a WayPoint to a nested record attribute structure. (as the BDRS has no PhotoPoint data type)
 */
public class PhotopointMapper {

    // A photopoint expects attributes to be named according to a particular naming convention.
    public static final String LATITUDE = "ppLat";
    public static final String LONGITUDE = "ppLong";
    public static final String BEARING = "bearing";
    public static final String DATE = "date";


    private Survey survey;
    public PhotopointMapper(Survey survey) {
        this.survey = survey;
    }

    public void map(Record record, List<PhotoPoint> photoPoints, Attribute photoPointAttribute) {

        if (photoPoints == null || photoPoints.size() == 0) {
            return;
        }

        for (PhotoPoint photoPoint : photoPoints) {
            AttributeValues values = new AttributeValues(photoPointAttribute, record, 0);
            map(photoPointAttribute.getNestedAttributes(), values, photoPoint);
            record.setValue(photoPointAttribute, values);
        }

    }

    private void map(List<Attribute> nestedAttributes, AttributeValues values, PhotoPoint photoPoint) {
        for (Attribute attribute : nestedAttributes) {
            if (attribute.getType().supportsNestedValues()) {
                AttributeValues nestedValues = new AttributeValues(attribute, null, 0);
                values.add(attribute, nestedValues);
                map(attribute.getNestedAttributes(), nestedValues, photoPoint);
            }
            else {
                LatLng coordinate = photoPoint.coordinate();
                if (LATITUDE.equals(attribute.name)) {
                    values.add(attribute, Double.toString(coordinate.latitude));
                }
                else if (LONGITUDE.equals(attribute.name)) {
                    values.add(attribute, Double.toString(coordinate.longitude));
                }
                else if (BEARING.equals(attribute.name)) {
                    // Not implemented...
                }
                else if (DATE.equals(attribute.name)) {
                    // Not implemented...
                }
                else if (Attribute.AttributeType.IMAGE == attribute.getType()) {
                    values.add(attribute, photoPoint.photo);
                }
            }

        }
    }

    public List<PhotoPoint> map(Record record, Attribute photoPointAttribute) {

        List<AttributeValues> photoPointAttributes = record.getNestedValues(photoPointAttribute);
        if (photoPointAttributes == null) {
            return null;
        }

        List<PhotoPoint> photoPoints = new ArrayList<PhotoPoint>(photoPointAttributes.size());

        for (AttributeValues photoPointValues : photoPointAttributes) {
            PhotoPoint photoPoint = new PhotoPoint(new Location(""));
            mapPhotoPoint(photoPoint, Arrays.asList(new AttributeValues[]{photoPointValues}));
            photoPoints.add(photoPoint);
        }
        return photoPoints;

    }

    private void mapPhotoPoint(PhotoPoint photoPoint, List<AttributeValues> valuesList) {

        if (valuesList == null) {
            return;
        }
        for (AttributeValues values : valuesList) {
            for (Record.AttributeValue value : values.getAttributeValues()) {
                Attribute attribute = survey.getAttribute(value.attribute_id);
                if (attribute.getType().supportsNestedValues()) {
                    mapPhotoPoint(photoPoint, value.getNestedValues());
                }
                else {
                    if (LATITUDE.equals(attribute.name)) {
                        photoPoint.location.setLatitude(Double.parseDouble(value.nullSafeValue()));
                    }
                    else if (LONGITUDE.equals(attribute.name)) {
                        photoPoint.location.setLongitude(Double.parseDouble(value.nullSafeValue()));
                    }
                    else if (BEARING.equals(attribute.name)) {

                    }
                    else if (DATE.equals(attribute.name)) {

                    }
                    else if (Attribute.AttributeType.IMAGE == attribute.getType()) {
                        photoPoint.photo = value.getUri();
                    }
                }
            }
        }
    }
}
