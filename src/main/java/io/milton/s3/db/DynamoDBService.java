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
package io.milton.s3.db;

import io.milton.s3.model.Entity;
import io.milton.s3.model.IEntity;
import io.milton.s3.model.IFolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

public interface DynamoDBService {

    void createTable();
    
    void waitForTableAvailable();
    
    TableDescription describeTable();
    
    void deleteTable();
    
    void waitForTableDeleted();
    
    boolean isTableExist();
    
    Map<String, AttributeValue> newItem(IEntity entity);
    
    PutItemResult putItem(Map<String, AttributeValue> item);
    
    Map<String, AttributeValue> getItem(HashMap<String, AttributeValue> primaryKey);
    
    List<Map<String, AttributeValue>> getItems(HashMap<String, AttributeValue> primaryKey);
    
    List<Map<String, AttributeValue>> getItems(Map<String, Condition> conditions);
    
    UpdateItemResult updateItem(HashMap<String, AttributeValue> primaryKey, Map<String, AttributeValueUpdate> updateItems);
    
    DeleteItemResult deleteItem(HashMap<String, AttributeValue> primaryKey);
    
    List<Entity> getChildren(IFolder parent, HashMap<String, AttributeValue> primaryKey);
    
    List<Entity> getChildren(IFolder parent, Map<String, Condition> conditions);
    
    boolean isRootFolderCreated(IFolder rootFolder);
}
