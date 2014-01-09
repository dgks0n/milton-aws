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

import io.milton.s3.db.mapper.DynamoDBEntityMapper;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;


public class DynamoDBServiceImpl implements DynamoDBService {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBServiceImpl.class);
    
    /**
     * Important: Be sure to fill in your AWS access credentials in the
     * AwsCredentials.properties file before you try to run this class.
     * 
     * http://aws.amazon.com/security-credentials
     */
    private final AmazonDynamoDBClient dynamoDBClient;
    
    private final String repository;
    
    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider
     * @see com.amazonaws.regions.Region
     */
    public DynamoDBServiceImpl(Region region, String repository) {
        this.repository = repository;
        
        LOG.info("Initialize Amazon DynamoDB environment...!!!");
        dynamoDBClient = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
        dynamoDBClient.setRegion(region);
    }
    
    @Override
    public void createTable() {
        List<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(AttributeKey.UUID)
        		.withAttributeType(ScalarAttributeType.S));
        
        List<KeySchemaElement> keySchemaElement = new ArrayList<KeySchemaElement>();
        keySchemaElement.add(new KeySchemaElement().withAttributeName(AttributeKey.UUID)
        		.withKeyType(KeyType.HASH));
        
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
            .withReadCapacityUnits(10L)
            .withWriteCapacityUnits(10L);
        
        CreateTableRequest createTableRequest = new CreateTableRequest()
            .withTableName(repository)
            .withAttributeDefinitions(attributeDefinitions)
            .withKeySchema(keySchemaElement)
            .withProvisionedThroughput(provisionedThroughput);
        
        try {
            CreateTableResult createdTableDescription = dynamoDBClient.createTable(createTableRequest);
            LOG.info("Created table description: " + createdTableDescription);
            
            // Wait for it to become active
            waitForTableAvailable();
        } catch (ResourceInUseException rie) {
            LOG.warn("Table " + repository + " already exists");
        }
    }

    @Override
    public void waitForTableAvailable() {
        LOG.info("Waiting for table " + repository + " to become active...");
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(repository);
            TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
            
            // Display current status of table
            String tableStatus = tableDescription.getTableStatus();
            LOG.info("Current state for table " + repository + ": " + tableStatus);
            if (tableStatus.equals(TableStatus.ACTIVE.toString()))
                return;
            
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception ex) {
                LOG.warn(ex.getMessage());
            }
        }
        
        throw new RuntimeException("Table " + repository + " never went active");
    }

    @Override
    public TableDescription describeTable() {
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(repository);
        TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
        if (tableDescription != null) {
        	LOG.info("Table description for " + repository + ": " + tableDescription);
        }
        
        return tableDescription;
    }

    @Override
    public void deleteTable() {
        DeleteTableRequest deleteTableRequest = new DeleteTableRequest().withTableName(repository);
        DeleteTableResult deleteTableResult = dynamoDBClient.deleteTable(deleteTableRequest);
        if (deleteTableRequest != null) {
        	LOG.info("Delete description for " + repository + ": " + deleteTableResult);
        }
        
        waitForTableDeleted();
    }

    @Override
    public void waitForTableDeleted() {
        LOG.info("Waiting for table " + repository + " while status deleting...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(repository);
                TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest)
                		.getTable();
                String tableStatus = tableDescription.getTableStatus();
                LOG.info("Current state for table " + repository + ": " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString()))
                    return;
            } catch (ResourceNotFoundException rne) {
                LOG.warn("Table " + repository + " is not found. It was deleted.");
                return;
            }
            
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception ex) {
                LOG.warn(ex.getMessage());
            }
        }
        throw new RuntimeException("Table " + repository + " was never deleted");
    }

    @Override
    public boolean isTableExist() {
        boolean isTableExist = false;
        if (describeTable() != null)
            isTableExist = true;
        
        return isTableExist;
    }
    
    @Override
    public Map<String, AttributeValue> newItem(Entity entity) {
        Map<String, AttributeValue> newItem = new HashMap<String, AttributeValue>();
        newItem.put(AttributeKey.UUID, new AttributeValue().withS(entity.getId().toString()));
        newItem.put(AttributeKey.ENTITY_NAME, new AttributeValue().withS(entity.getName()));
        
        // Get folder parent UUID
        String parentUniqueId = AttributeKey.NOT_EXIST;
        Folder folder = entity.getParent();
        if (folder != null) {
        	parentUniqueId = folder.getId().toString();
        }
        
        int fileSize = 0;
        String contentType = AttributeKey.NOT_EXIST;
        if (entity instanceof File) {
            fileSize = (int) ((File) entity).getSize();
            contentType = ((File) entity).getContentType();
        }
        
        newItem.put(AttributeKey.PARENT_UUID, new AttributeValue().withS(parentUniqueId));
		newItem.put(AttributeKey.IS_DIRECTORY, new AttributeValue()
				.withN(Integer.toString(entity.isDirectory() ? 1 : 0)));
        newItem.put(AttributeKey.FILE_SIZE, new AttributeValue().withN(Integer.toString(fileSize)));
        newItem.put(AttributeKey.CONTENT_TYPE, new AttributeValue().withS(contentType));
		newItem.put(AttributeKey.CREATED_DATE, new AttributeValue()
				.withS(DateUtils.dateToString(entity.getCreatedDate())));
		newItem.put(AttributeKey.MODIFIED_DATE, new AttributeValue()
				.withS(DateUtils.dateToString(entity.getModifiedDate())));
        return newItem;
    }

    /**
     * Put given item into the table. If the item exists, it replaces the entire
     * item. Instead of replacing the entire item, if you want to update only
     * specific attributes, you can use the updateItem method.
     * 
     * @param item
     */
    @Override
    public PutItemResult putItem(Map<String, AttributeValue> item) {
        try {
            PutItemRequest putItemRequest = new PutItemRequest(repository, item);
            PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            
			LOG.info("Putted item " + item.toString() + " into " + repository
					+ "; Putted status: " + putItemResult);
            return putItemResult;
        } catch (Exception ex) {
            LOG.error("Failed to put given item into the " + repository, ex);
        }
        return null;
    }
    
    @Override
    public Map<String, AttributeValue> getItem(HashMap<String, AttributeValue> primaryKey) {
    	try {
    		GetItemRequest getItemRequest = new GetItemRequest().withTableName(repository)
    				.withKey(primaryKey);
            GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
            Map<String, AttributeValue> item = getItemResult.getItem();
            if (item == null || item.isEmpty()) {
				LOG.warn("Could not find any item for the given UUID: "
						+ primaryKey + " from " + repository);
            	return Collections.emptyMap();
            }
            
			LOG.info("Getting result from " + repository + ": " + item
					+ "; Returning " + item.size() + " items");
            return item;
		} catch (Exception ex) {
			LOG.error("Failed to get item into the " + repository, ex);
		}
    	
        return Collections.emptyMap();
    }
    
    @Override
    public List<Map<String, AttributeValue>> getItem(Map<String, Condition> conditions) {
        ScanRequest scanRequest = new ScanRequest(repository).withScanFilter(conditions);
        ScanResult scanResult = dynamoDBClient.scan(scanRequest);
        int count = scanResult.getCount();
        if (count == 0)
            return Collections.emptyList();
        
		LOG.info("Successful by getting items from " + repository
				+ " based on conditions: " + conditions.toString() + "; Returning " + count + " of items");
        return scanResult.getItems();
    }
    
    @Override
    public UpdateItemResult updateItem(HashMap<String, AttributeValue> primaryKey, Map<String, 
    		AttributeValueUpdate> updateItems) {
        Map<String, AttributeValueUpdate> attributeValueUpdates = new HashMap<String, AttributeValueUpdate>();
        attributeValueUpdates.putAll(updateItems);
        
        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
            .withTableName(repository)
            .withKey(primaryKey).withReturnValues(ReturnValue.UPDATED_NEW)
            .withAttributeUpdates(updateItems);
        
        UpdateItemResult updateItemResult = dynamoDBClient.updateItem(updateItemRequest);
		LOG.info("Successful by updating item from " + repository
				+ "; Updated result: " + updateItemResult); 
        return updateItemResult;
    }

    @Override
    public DeleteItemResult deleteItem(HashMap<String, AttributeValue> primaryKey) {
        DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
            .withTableName(repository)
            .withKey(primaryKey);
            
        DeleteItemResult deleteItemResult = dynamoDBClient.deleteItem(deleteItemRequest);
        LOG.info("Successful by deleting item in " + repository);
        return deleteItemResult;
    }
    
    @Override
    public Map<String, AttributeValue> getItem(List<Map<String, Condition>> conditions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Entity> getChildren(Folder parent, Map<String, Condition> conditions) {
    	List<Map<String, AttributeValue>> items = getItem(conditions);
        return DynamoDBEntityMapper.convertItemsToEntities(parent, items);
    }
    
    @Override
	public Folder getRootFolder() {
        Condition condition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withS(AttributeKey.NOT_EXIST));
            
        Map<String, Condition> conditions = new HashMap<String, Condition>();
        conditions.put(AttributeKey.PARENT_UUID, condition);
            
        List<Entity> entities = getChildren(null, conditions);
        if (entities == null || entities.isEmpty()) {
            LOG.warn("Could not find item for the given "
                    + conditions.toString() + " from " + repository);
            return null;
        }
        
        return (Folder) entities.get(0);
	}
    
}
