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
package io.milton.s3.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

public class DynamoDBManager {

	private static final Logger LOG = LoggerFactory.getLogger(DynamoDBManager.class);
	
	/**
	 * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
	 */
	private static AmazonDynamoDBClient dynamoDBClient;
	
	/**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
	public static void openDynamoDB() throws Exception {
		dynamoDBClient = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
		
		// Set default region
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDBClient.setRegion(usWest2);
	}
	
	public static AmazonDynamoDBClient getDynamoDB() {
		return dynamoDBClient;
	}
	
	/**
	 * Waiting for the table to become active
	 * 
	 * @param tableName - The table name
	 */
	public static void waitingForTableAvailable(String tableName) {
		LOG.info("Waiting for " + tableName + " to become active...");
		
		long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
        	try {
        		Thread.sleep(1000 * 20);
        	} catch (Exception ex) {
        		LOG.warn(ex.getMessage());
        	}
        	
        	try {
                DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = getDynamoDB().describeTable(describeTableRequest).getTable();
                
                // Display current status of table
                String tableStatus = tableDescription.getTableStatus();
                LOG.info("Current state for " + tableName + ": " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                	return;
                }
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) {
                	throw ase;
                }
            }
        }
        
        throw new RuntimeException("Table " + tableName + " never went active");
	}
	
	/**
	 * Create the table if it already does not exist for the given name
	 * 
	 * @param tableName - the table name
	 */
	public static void createTable(String tableName) {
		List<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("name").withAttributeType(ScalarAttributeType.S));
		
		List<KeySchemaElement> keySchemaElement = new ArrayList<KeySchemaElement>();
		keySchemaElement.add(new KeySchemaElement().withAttributeName("name").withKeyType(KeyType.HASH));
		
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
			DynamoDBManager.waitingForTableAvailable(tableName);
		} catch (ResourceInUseException rie) {
			LOG.warn("Table " + tableName + " already exists");
		}
	}
	
	/**
	 * Describe the table for the given table
	 * 
	 * @param tableName - the table name
	 */
	public static void describeTable(String tableName) {
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
        
        LOG.info("Table description: " + tableDescription);
    }
	
	/**
	 * Put given item into the table
	 * 
	 * @param item
	 */
	public static void putItem(String tableName, Map<String, AttributeValue> item) {
		try {
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            
            LOG.info("Result: " + putItemResult);
        } catch (Exception ex) {
            LOG.error("Failed to put given item into the " + tableName, ex);
        }
	}
}
