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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

public interface DynamoDBService {

    /**
     * Adds a new table to your account
     * 
     * The table name must be unique among those associated with the AWS Account
     * issuing the request, and the AWS Region that receives the request (e.g.
     * us-east-1 ). The CreateTable operation triggers an asynchronous workflow
     * to begin creating the table. Amazon DynamoDB immediately returns the
     * state of the table ( CREATING ) until the table is in the ACTIVE state.
     * Once the table is in the ACTIVE state, you can perform data plane
     * operations.
     * 
     * @param repository
     *            - The name of the table
     */
    void createTable(String repository);

    /**
     * Deletes a table and all of its items
     * 
     * If the table is in the ACTIVE state, you can delete it. If a table is in
     * CREATING or UPDATING states then Amazon DynamoDB returns a
     * ResourceInUseException . If the specified table does not exist, Amazon
     * DynamoDB returns a ResourceNotFoundException.
     * 
     * @param repository
     *            - The name of the table
     */
    void deleteTable(String repository);

    boolean isTableExist(String repository);

    Map<String, AttributeValue> newItem(Entity entity);

    PutItemResult putItem(String repository, Map<String, AttributeValue> item);

    /**
     * Retrieves a set of Attributes for an item that matches the primary key.
     * The GetItem operation provides an eventually-consistent read by default.
     * If eventually-consistent reads are not acceptable for your application,
     * use ConsistentRead . Although this operation might take longer than a
     * standard read, it always returns the last updated value.
     * 
     * @param repository
     *            - The name of the table
     * @param primaryKey
     *            - The primary key of the item
     * @return The response from the GetItem service method, as returned by
     *         AmazonDynamoDB
     */
    Map<String, AttributeValue> getItem(String repository,
            HashMap<String, AttributeValue> primaryKey);

    List<Map<String, AttributeValue>> getItem(String repository,
            Map<String, Condition> conditions);

    /**
     * Edits an existing item's attributes. You can perform a conditional update
     * (insert a new attribute name-value pair if it doesn't exist, or replace
     * an existing name-value pair if it has certain expected attribute values).
     * 
     * @param repository
     *            - The name of the table
     * @param primaryKey
     *            - The primary key of the item
     * @param updateItems
     *            - The new expected attribute values
     * @return
     */
    UpdateItemResult updateItem(String repository,
            HashMap<String, AttributeValue> primaryKey,
            Map<String, AttributeValueUpdate> updateItems);

    /**
     * Deletes a single item in a table by primary key
     * 
     * You can perform a conditional delete operation that deletes the item if
     * it exists, or if it has an expected attribute value.
     * 
     * @param repository
     *              - The name of the table
     * @param primaryKey
     *              - The primary key of the item
     * @return
     */
    DeleteItemResult deleteItem(String repository,
            HashMap<String, AttributeValue> primaryKey);
}
