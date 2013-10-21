package mobi.jenkinsci.alm.assembla.client.test;

import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import mobi.jenkinsci.alm.assembla.client.CalendarSerializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class TestCalendarSerializer {

  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private static Date DATE;
  static {
    try {
      DATE = SIMPLE_DATE_FORMAT.parse("2013-03-30T10:00:32-0000");
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
  private CalendarSerializer serializer;
  private static final JsonElement DATE_WITHOUT_TZ_JSON = new JsonPrimitive("2013-03-30T10:00:32");
  private static final JsonElement DATE_ENDING_WITH_Z_JSON = new JsonPrimitive("2013-03-30T10:00:32Z");
  private static final JsonElement DATE_WITH_COLON_ON_TZ_JSON = new JsonPrimitive("2013-03-30T10:00:32+00:00");

  @Before
  public void setUp() throws Exception {
    serializer = new CalendarSerializer();
  }

  @Test
  public void testDateWithoutTzAreParsedCorrectly() {
    assertDeserialisedDateEquals(DATE, DATE_WITHOUT_TZ_JSON);
  }

  private void assertDeserialisedDateEquals(Date expectedDate, JsonElement jsonDate) {
    Calendar calendar = serializer.deserialize(jsonDate, null, null);
    assertNotNull(calendar);
    Assert.assertEquals(expectedDate, calendar.getTime());
  }

  @Test
  public void testDateEndingWithZAreParsedCorrectly() {
    assertDeserialisedDateEquals(DATE, DATE_ENDING_WITH_Z_JSON);
  }

  @Test
  public void testDateWithColonOnTzAreParsedCorrectly() {
    assertDeserialisedDateEquals(DATE, DATE_WITH_COLON_ON_TZ_JSON);
  }
}
