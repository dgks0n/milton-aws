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

public class AttributeKey {
	
	public static final String NOT_EXIST = "NONE";
	
	public static final String UUID = "UniqueId";
	public static final String ENTITY_NAME = "EntityName";
	public static final String PARENT_UUID = "ParentId";
	public static final String IS_DIRECTORY = "IsDirectory";
	public static final String FILE_SIZE = "FileSize";
	public static final String CONTENT_TYPE = "ContentType";
	public static final String CREATED_DATE = "CreatedDate";
	public static final String MODIFIED_DATE = "ModifiedDate";
}
