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
package io.milton.s3.db.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.milton.s3.db.DynamoDBService;
import io.milton.s3.db.DynamoDBServiceImpl;
import io.milton.s3.db.mapper.DynamoDBEntityMapper;
import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;
import io.milton.s3.model.IEntity;
import io.milton.s3.model.IFile;
import io.milton.s3.model.IFolder;
import io.milton.s3.util.AttributeKey;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

public class DynamoDBClient {

	private static final Logger LOG = LoggerFactory.getLogger(DynamoDBClient.class);
	
	/**
     * Amazon DynamoDB Storage Service
     */
    private final DynamoDBService dynamoDBService;
    
    /**
     * Default constructor with default region (US_WEST_2)
     * 
     * @param repository
     * 				- the table name
     */
    public DynamoDBClient(String repository) {
    	this(Region.getRegion(Regions.US_WEST_2), repository);
    }
    
    /**
     * Initialize Amazon DynamoDB environment for the given repository
     * 
     * @param region
     * 				- the region
     * @param repository
     * 				- the table name
     */
	public DynamoDBClient(Region region, String repository) {
		dynamoDBService = new DynamoDBServiceImpl(region, repository);
		if (!dynamoDBService.isTableExist()) {
            LOG.info("Creating table " + repository + " in Amazon DynamoDB...!!!");
            
			// Create table if it's not exist & describe the table for the given
			// table after created
            dynamoDBService.createTable();
            dynamoDBService.describeTable();
        }
	}
	
	/**
	 * The putEntity method stores an item in a table
	 * 
	 * @param entity
	 * @return
	 */
	public boolean putEntity(IEntity entity) {
		boolean isSuccess = false;
		if (entity == null)
			return isSuccess;
		
		Map<String, AttributeValue> newItem = dynamoDBService.newItem(entity);
		if (dynamoDBService.putItem(newItem) != null)
			isSuccess = true;
			
		return isSuccess;
	}
	
	/**
	 * The findRootFolder method retrieves an root item
	 * 
	 * @return
	 */
	public IFolder findRootFolder() {
		return dynamoDBService.getRootFolder();
	}
	
	/**
	 * The findEntityByUniqueId method retrieves an root item for the given
	 * unique UUID
	 * 
	 * @param entity
	 * @return
	 */
	public IEntity findEntityByUniqueId(IEntity entity) {
		if (entity == null)
			return null;
		
		HashMap<String, AttributeValue> primaryKey = new HashMap<String, AttributeValue> ();
		primaryKey.put(AttributeKey.UUID, new AttributeValue().withS(entity.getId().toString()));
		Entity dynamoEntity = DynamoDBEntityMapper.convertItemToEntity(entity.getParent(), 
				dynamoDBService.getItem(primaryKey));
		return dynamoEntity;
	}
	
	/**
	 * The findEntityByParent method enables you to retrieve multiple items
	 * from one table.
	 * 
	 * @param parent
	 * @return
	 */
	public List<Entity> findEntityByParent(Folder parent) {
		if (parent == null)
			return Collections.emptyList();
		
		Condition condition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue().withS(parent.getId().toString()));
        Map<String, Condition> conditions = new HashMap<String, Condition>();
        conditions.put(AttributeKey.PARENT_UUID, condition);
        List<Entity> children = dynamoDBService.getChildren(parent, conditions);
        return children;
	}
	
	/**
	 * Move or rename file to other folder
	 * 
	 * @param file
	 * 				- current entity want to move or rename
	 * @param newParent
	 * 				- parent of entity
	 * @param newEntityName
	 * 				- new name of entity
	 * @param isRenaming
	 * 				- TRUE is renaming file, otherwise FALSE
	 * @return TRUE/FALSE
	 */
	public boolean updateEntityByUniqueId(IFile file, IFolder newParent, String newEntityName, boolean isRenaming) {
		HashMap<String, AttributeValue> primaryKey = new HashMap<String, AttributeValue>();
        primaryKey.put(AttributeKey.UUID, new AttributeValue().withS(file.getId().toString()));
        
        Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();
        updateItems.put(AttributeKey.ENTITY_NAME, new AttributeValueUpdate()
        	.withAction(AttributeAction.PUT).withValue(new AttributeValue().withS(newEntityName)));
        
        if (!isRenaming) {
        	updateItems.put(AttributeKey.PARENT_UUID, new AttributeValueUpdate()
    			.withAction(AttributeAction.PUT).withValue(new AttributeValue()
    			.withS(newParent.getId().toString())));
        }
        
        UpdateItemResult updateStatus = dynamoDBService.updateItem(primaryKey, updateItems);
        if (updateStatus != null)
        	return true;
        
		return false;
	}
	
	/**
	 * The deleteEntityByUniqueId method deletes an item from a table.
	 * 
	 * @param uniqueId
	 * @return
	 */
	public boolean deleteEntityByUniqueId(String uniqueId) {
		boolean isSuccess = false;
		if (StringUtils.isEmpty(uniqueId))
			return isSuccess;
		
		HashMap<String, AttributeValue> primaryKey = new HashMap<String, AttributeValue> ();
		primaryKey.put(AttributeKey.UUID, new AttributeValue().withS(uniqueId));
		if (dynamoDBService.deleteItem(primaryKey) != null)
			isSuccess = true;
		
		return isSuccess;
	}
}
