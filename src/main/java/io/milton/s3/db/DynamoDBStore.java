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

import io.milton.s3.model.BaseEntity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.DynamoDBTable;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

public class DynamoDBStore {

	private static final Logger LOG = LoggerFactory.getLogger(DynamoDBStore.class);
	
	/**
	 * Constructor
	 */
	public DynamoDBStore() {}
	
	/**
	 * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
	 */
	private AmazonDynamoDBClient dynamoDBClient;
	
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
	public void openDynamoDB() throws Exception {
		dynamoDBClient = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
		
		// Set default region
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDBClient.setRegion(usWest2);
	}
	
	/**
	 * Create the table if it already does not exist for the given name
	 * 
	 * @param tableName - the table name
	 */
	public void createTable(String tableName) {
		List<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition().withAttributeName(DynamoDBTable.UUID).withAttributeType(ScalarAttributeType.S));
		
		List<KeySchemaElement> keySchemaElement = new ArrayList<KeySchemaElement>();
		keySchemaElement.add(new KeySchemaElement().withAttributeName(DynamoDBTable.UUID).withKeyType(KeyType.HASH));
		
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
			.withReadCapacityUnits(10L)
			.withWriteCapacityUnits(10L);
		
		CreateTableRequest createTableRequest = new CreateTableRequest()
	        .withTableName(tableName)
	        .withAttributeDefinitions(attributeDefinitions)
	        .withKeySchema(keySchemaElement)
	        .withProvisionedThroughput(provisionedThroughput);
		
		try {
			CreateTableResult createdTableDescription = dynamoDBClient.createTable(createTableRequest);
			LOG.info("Created table description: " + createdTableDescription);
			
			// Wait for it to become active
			waitingForTableAvailable(tableName);
		} catch (ResourceInUseException rie) {
			LOG.warn("Table " + tableName + " already exists");
		}
	}
	
	/**
	 * Describe the table for the given table
	 * 
	 * @param tableName - the table name
	 */
	public void describeTable(String tableName) {
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
        
        LOG.info("Table description: " + tableDescription);
    }
	
	/**
	 * Remove a existing table for the given table name
	 * 
	 * @param tableName
	 */
	public void deleteTable(String tableName) {
	    DeleteTableRequest deleteTableRequest = new DeleteTableRequest().withTableName(tableName);
	    DeleteTableResult deleteTableResult = dynamoDBClient.deleteTable(deleteTableRequest);
	    
	    waitingForTableDeleted(tableName);
	    LOG.info("Delete result: " + deleteTableResult);
	}
	/**
	 * Put given item into the table
	 * 
	 * @param item
	 */
	public PutItemResult putItem(String tableName, Map<String, AttributeValue> item) {
		try {
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            
            LOG.info("Putted item result: " + putItemResult);
            return putItemResult;
        } catch (Exception ex) {
            LOG.error("Failed to put given item into the " + tableName, ex);
        }
		return null;
	}
	
	/**
	 * Parser new item for the given informations (UniqueId, Name of item, Parent Id, Blob Id,...)
	 * 
	 * @param uniqueId
	 * @param itemName
	 * @param parentId
	 * @param blobId
	 * @param fileSize
	 * @param createdDate
	 * @param modifiedDate
	 * 
	 * @return a map of item
	 * @throws ParseException 
	 */
	public Map<String, AttributeValue> newItem(String uniqueId, String itemName, String parentId, String blobId, 
	        int fileSize, String contentType, Date createdDate, Date modifiedDate) {
		
	    Map<String, AttributeValue> newItem = new HashMap<String, AttributeValue>();
	    newItem.put(DynamoDBTable.UUID, new AttributeValue().withS(uniqueId));
	    newItem.put(DynamoDBTable.ENTITY_NAME, new AttributeValue().withS(itemName));
	    newItem.put(DynamoDBTable.PARENT_UUID, new AttributeValue().withS(parentId));
	    newItem.put(DynamoDBTable.BLOB_ID, new AttributeValue().withS(blobId));
	    newItem.put(DynamoDBTable.FILE_SIZE, new AttributeValue().withN(Integer.toString(fileSize)));
	    newItem.put(DynamoDBTable.CONTENT_TYPE, new AttributeValue().withS(contentType));
	    newItem.put(DynamoDBTable.CREATED_DATE, new AttributeValue().withS(createdDate.toString()));
	    newItem.put(DynamoDBTable.MODIFIED_DATE, new AttributeValue().withS(modifiedDate.toString()));
        return newItem;
	}
	
	/**
	 * Get all entities for the given conditions
	 * 
	 * @param parent
	 * @param tableName
	 * @param fieldFilder
	 * @param condition
	 * 
	 * @return a list of entities
	 * @throws ParseException 
	 */
	public List<BaseEntity> getChildren(Folder parent, String tableName, String fieldFilder, Condition condition) {
	    List<Map<String, AttributeValue>> items = getItem(tableName, fieldFilder, condition);
	    if (items.isEmpty())
            Collections.emptyList();
        
	    final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT);
        List<BaseEntity> childrens = new ArrayList<BaseEntity>();
        for (Map<String, AttributeValue> item : items) {
            String blobId = item.get(DynamoDBTable.BLOB_ID).getS();
            String entityName = item.get(DynamoDBTable.ENTITY_NAME).getS();
            
            Date createdDate = null;
            Date modifiedDate = null;
            try {
            	createdDate = dateFormat.parse(item.get(DynamoDBTable.CREATED_DATE).getS());
                modifiedDate = dateFormat.parse(item.get(DynamoDBTable.MODIFIED_DATE).getS());
			} catch (ParseException pe) {
				LOG.warn(pe.getMessage());
			}
            
            if (StringUtils.isEmpty(blobId)) {
                Folder folder = new Folder(entityName, parent);
                folder.setCreatedDate(createdDate);
                folder.setModifiedDate(modifiedDate);
                childrens.add(folder);
            } else {
                File file = new File(entityName, parent);
                file.setCreatedDate(createdDate);
                file.setModifiedDate(modifiedDate);
                file.setBytes(blobId.getBytes());
                file.setContentType(item.get(DynamoDBTable.CONTENT_TYPE).getS());
                file.setSize(new Long(item.get(DynamoDBTable.FILE_SIZE).getN()));
                childrens.add(file);
            }
        }
        
        return childrens;
	}
	
	/**
	 * Return TRUE if Root folder already created, otherwise FALSE
	 * 
	 * @param tableName
	 * @param fieldFilter
	 * @param condition
	 * @return TRUE/FALSE
	 */
	public boolean isRootFolderCreated(String tableName, String fieldFilter, Condition condition) {
	    List<Map<String, AttributeValue>> items = getItem(tableName, fieldFilter, condition);
	    if (items.isEmpty())
	        return false;
	    return true;
	}
	
	/**
	 * Waiting for the table to become active
	 * 
	 * @param tableName - The table name
	 */
	private void waitingForTableAvailable(String tableName) {
		LOG.info("Waiting for table " + tableName + " to become active...");
		
		long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
            
            // Display current status of table
            String tableStatus = tableDescription.getTableStatus();
            LOG.info("Current state for table " + tableName + ": " + tableStatus);
            if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                return;
            }
            
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception ex) {
                LOG.warn(ex.getMessage());
            }
        }
        
        throw new RuntimeException("Table " + tableName + " never went active");
	}
	
	/**
	 * Waiting for table to be deleted
	 * 
	 * @param tableName
	 */
	private void waitingForTableDeleted(String tableName) {
	    LOG.info("Waiting for table " + tableName + " while status deleting...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
                String tableStatus = tableDescription.getTableStatus();
                LOG.info("Current state for table " + tableName + ": " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                    return;
                }
            } catch (ResourceNotFoundException rne) {
                LOG.warn("Table " + tableName + " is not found. It was deleted.");
                return;
            }
            
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception ex) {
                LOG.warn(ex.getMessage());
            }
        }
        throw new RuntimeException("Table " + tableName + " was never deleted");
	}
	
	private List<Map<String, AttributeValue>> getItem(String tableName, String fieldFilter, Condition condition) {
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        scanFilter.put(fieldFilter, condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDBClient.scan(scanRequest);
        
        int count = scanResult.getCount();
        if (count <= 0)
        	return Collections.emptyList();
        
		LOG.info("Successful by getting items from " + tableName
				+ " based on filter field: " + fieldFilter + "; Returning " + count + " of items");
        return scanResult.getItems();
    }

}
