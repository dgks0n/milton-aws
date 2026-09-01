/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.s3.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** Thread-safe conversion for the timestamp format stored in DynamoDB. */
public final class DateUtils {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z uuuu", Locale.ENGLISH);

  private DateUtils() {}

  /** Parses a stored timestamp. */
  public static Date dateFromString(String value) {
    Objects.requireNonNull(value, "value");
    try {
      return Date.from(ZonedDateTime.parse(value, FORMATTER).toInstant());
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid stored date: " + value, exception);
    }
  }

  /** Formats a timestamp for storage. */
  public static String dateToString(Date value) {
    Objects.requireNonNull(value, "value");
    return FORMATTER.format(value.toInstant().atZone(java.time.ZoneId.systemDefault()));
  }
}
