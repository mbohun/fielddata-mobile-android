package au.org.ala.fielddata.mobile.model;

import java.util.List;

/**
 * Maps a WayPoint to a nested record attribute structure. (as the BDRS has no PhotoPoint data type)
 */
public class PhotopointMapper {

    private Survey survey;
    public PhotopointMapper(Survey survey) {
        this.survey = survey;
    }

    public void map(Record record, List<PhotoPoint> photoPoints, Attribute photoPointAttribute) {

        if (photoPoints == null || photoPoints.size() == 0) {
            return;
        }
        AttributeValues values = new AttributeValues(photoPointAttribute, record, 0);

        for (PhotoPoint photoPoint : photoPoints) {
            map(photoPointAttribute.getNestedAttributes(), values, photoPoint);
        }
        record.setValue(photoPointAttribute, values);
    }

    private void map(List<Attribute> nestedAttributes, AttributeValues values, PhotoPoint photoPoint) {
        for (Attribute attribute : nestedAttributes) {
            if (attribute.getType().supportsNestedValues()) {
                AttributeValues nestedValues = new AttributeValues(attribute, null, 0);
                values.add(attribute, nestedValues);
                map(attribute.getNestedAttributes(), nestedValues, photoPoint);
            }
            else {
                if ("lat".equals(attribute.name)) {
                    values.add(attribute, Double.toString(photoPoint.coordinate().latitude));
                }
                else if ("lon".equals(attribute.name)) {
                    values.add(attribute, Double.toString(photoPoint.coordinate().latitude));
                }
                else if ("bearing".equals(attribute.name)) {

                }
                else if ("date".equals(attribute.name)) {

                }
                else if (Attribute.AttributeType.IMAGE == attribute.getType()) {
                    values.add(attribute, photoPoint.photo);
                }
            }

        }
    }


}
