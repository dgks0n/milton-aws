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
package io.milton.s3.db.mapper;

import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class DynamoDBEntityMapper {

	public static List<Entity> convertItemsToEntities(Folder parent, List<Map<String, 
			AttributeValue>> items) {
        if (items.isEmpty())
        	Collections.emptyList();
        
        List<Entity> childrens = new ArrayList<Entity>();
        for (Map<String, AttributeValue> item : items) {
        	Entity entity = convertItemToEntity(parent, item);
            childrens.add(entity);
        }
        
        return childrens;
    }
    
	public static Entity convertItemToEntity(Folder parent, Map<String, AttributeValue> item) {
    	
    	Date createdDate = DateUtils.dateFromString(item.get(AttributeKey.CREATED_DATE).getS());
        Date modifiedDate = DateUtils.dateFromString(item.get(AttributeKey.MODIFIED_DATE).getS());
        
        String uniqueId = item.get(AttributeKey.UUID).getS();
        String entityName = item.get(AttributeKey.ENTITY_NAME).getS();
        if (Integer.valueOf(item.get(AttributeKey.IS_DIRECTORY).getN()) == 1) {
			Folder folder = new Folder(UUID.fromString(uniqueId), entityName,
					createdDate, modifiedDate, parent);
            return folder;
        } else {
			File file = new File(UUID.fromString(uniqueId), entityName,
					createdDate, modifiedDate, parent);
            file.setContentType(item.get(AttributeKey.CONTENT_TYPE).getS());
            file.setSize(new Long(item.get(AttributeKey.FILE_SIZE).getN()));
            return file;
        }
    }
}
