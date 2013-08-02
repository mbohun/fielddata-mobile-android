package au.org.ala.fieldcapture.mobile.test;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.List;

import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.AttributeValues;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.Survey;

/**
 * Tests the RecordDAO class.
 */
public class RecordDAOTest extends AndroidTestCase {

    private static final int TEXT_ATTRIBUTE_ID = 1;
    private static final int INTEGER_ATTRIBUTE_ID = 2;
    private static final int PHOTOPOINT_ATTRIBUTE_ID = 3;

    private RecordDAO recordDAO;
    private Survey simpleSurvey;
    private Survey photoPointSurvey;


    public void setUp() throws Exception {
        super.setUp();

        recordDAO = new RecordDAO(getContext());
        recordDAO.deleteAll(Record.class);
        simpleSurvey = createSimpleSurvey(1);
        photoPointSurvey = createPhotoPointSurvey(2);
    }

    @MediumTest
    public void testSaveSimpleRecord() {

        String textAttributeValue = "textAttributeValue";
        String integerAttributeValue = "3";

        Record record = new Record();

        record.created = System.currentTimeMillis();
        record.survey_id = simpleSurvey.server_id;
        record.setValue(simpleSurvey.getAttribute(TEXT_ATTRIBUTE_ID), textAttributeValue);
        record.setValue(simpleSurvey.getAttribute(INTEGER_ATTRIBUTE_ID), integerAttributeValue);

        recordDAO.save(record);


        List<Record> records = recordDAO.loadAll(Record.class);
        assertEquals(1, records.size());
        Record underTest = records.get(0);
        assertEquals(record.survey_id, underTest.survey_id);
        assertEquals(2, underTest.getAttributeValues().size());

        assertEquals(textAttributeValue, underTest.getValue(simpleSurvey.getAttribute(TEXT_ATTRIBUTE_ID)));
        assertEquals(integerAttributeValue, underTest.getValue(simpleSurvey.getAttribute(INTEGER_ATTRIBUTE_ID)));


    }

    @MediumTest
    public void testSavePhotoPoint() {

        String textAttributeValue = "textAttributeValue";
        String integerAttributeValue = "3";

        Record record = new Record();

        record.created = System.currentTimeMillis();
        record.survey_id = photoPointSurvey.server_id;
        record.setValue(photoPointSurvey.getAttribute(TEXT_ATTRIBUTE_ID), textAttributeValue);
        record.setValue(photoPointSurvey.getAttribute(INTEGER_ATTRIBUTE_ID), integerAttributeValue);

        AttributeValues photoPointValues = new AttributeValues(photoPointSurvey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID), record, 0);
        photoPointValues.add(photoPointSurvey.getAttribute(4), "-35");
        photoPointValues.add(photoPointSurvey.getAttribute(5), "-120");
        record.setValue(photoPointSurvey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID), photoPointValues);

        recordDAO.save(record);


        List<Record> records = recordDAO.loadAll(Record.class);
        assertEquals(1, records.size());
        Record underTest = records.get(0);
        assertEquals(record.survey_id, underTest.survey_id);
        assertEquals(3, underTest.getAttributeValues().size());

        assertEquals(textAttributeValue, underTest.getValue(photoPointSurvey.getAttribute(TEXT_ATTRIBUTE_ID)));
        assertEquals(integerAttributeValue, underTest.getValue(photoPointSurvey.getAttribute(INTEGER_ATTRIBUTE_ID)));

        // Check the photopoint.
        List<AttributeValues> values = underTest.getNestedValues(photoPointSurvey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID));
        assertEquals(1, values.size());
        AttributeValues photoPoint = values.get(0);
        assertEquals(2, photoPoint.getAttributeValues().size());
        assertEquals(0, photoPoint.getRow());
        assertEquals("-35", photoPoint.getAttributeValues().get(0).nullSafeValue());
        assertEquals("-120", photoPoint.getAttributeValues().get(1).nullSafeValue());

    }



    private Survey createSimpleSurvey(int id) {
        Survey survey = new Survey();
        survey.server_id = id;
        survey.setId(id);

        survey.addAttribute(Attribute.AttributeType.TEXT, "textAttribute", TEXT_ATTRIBUTE_ID);
        survey.addAttribute(Attribute.AttributeType.INTEGER, "integerAttribute", INTEGER_ATTRIBUTE_ID);

        return survey;
    }

    private Survey createPhotoPointSurvey(int id) {
        Survey survey = createSimpleSurvey(id);

        survey.addAttribute(Attribute.AttributeType.CENSUS_METHOD_ROW, "photoPointAttribute", PHOTOPOINT_ATTRIBUTE_ID);
        Attribute photoPointAttribute = survey.getAttribute(PHOTOPOINT_ATTRIBUTE_ID);

        photoPointAttribute.addNestedAttribute(4, Attribute.AttributeType.DECIMAL, "lat");
        photoPointAttribute.addNestedAttribute(5, Attribute.AttributeType.DECIMAL, "lon");


        return survey;
    }
}
