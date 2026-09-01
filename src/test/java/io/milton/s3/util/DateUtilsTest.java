package io.milton.s3.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

  @Test
  void roundTripsStoredTimestamp() {
    Date original = new Date(1_700_000_000_000L);

    Date parsed = DateUtils.dateFromString(DateUtils.dateToString(original));

    assertEquals(original, parsed);
  }

  @Test
  void rejectsNullAndMalformedDates() {
    assertThrows(NullPointerException.class, () -> DateUtils.dateToString(null));
    assertThrows(NullPointerException.class, () -> DateUtils.dateFromString(null));
    assertThrows(IllegalArgumentException.class, () -> DateUtils.dateFromString("not-a-date"));
  }
}
