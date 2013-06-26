package au.org.ala.fielddata.mobile.model;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for nested Record attribute values as defined by a Census method typed attribute.
 */
public class AttributeValues {

    int row;
    Integer recordId;
    int attributeId;

    List<Record.AttributeValue> values = new ArrayList();

    public AttributeValues(Attribute attribute, Record record, int row) {
        this.attributeId = attribute.server_id;
        this.recordId = record.getId();
        this.row = row;
    }

    public AttributeValues(Integer attributeId, Record record, int row) {
        this.attributeId = attributeId;
        this.recordId = record.getId();
        this.row = row;
    }



    public int getRow() {
        return row;
    }

    public int getAttributeId() {
        return attributeId;
    }

    public List<Record.AttributeValue> getAttributeValues() {
        return values;
    }

    public void add(Attribute attribute, String value) {
        Record.AttributeValue attributeValue = new Record.AttributeValue(attribute.server_id);
        attributeValue.setValue(value);
        values.add(attributeValue);
    }


    public void add(Attribute attribute, Uri value) {

        Record.AttributeValue attributeValue = new Record.AttributeValue(attribute.server_id);
        attributeValue.setUri(value);
        values.add(attributeValue);
    }

    public void add(Attribute attribute, AttributeValues attributeValues) {
        Record.AttributeValue attributeValue = new Record.AttributeValue(attribute.server_id);
        attributeValue.addNestedValues(attributeValues);
        values.add(attributeValue);
    }
}
