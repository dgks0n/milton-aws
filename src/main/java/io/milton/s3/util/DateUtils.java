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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	public static final DateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
	
	/**
	 * New date for the given string based on format date
	 * 
	 * @param dateString
	 * @return
	 */
	public static Date dateFromString(String dateString) {
    	Date date = null;
    	try {
    		date = dateFormat.parse(dateString);
		} catch (ParseException pe) {}
    	return date;
    }
	
	/**
	 * Convert a date to string for the give date
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToString(Date date) {
		return dateFormat.format(date);
	}
}
