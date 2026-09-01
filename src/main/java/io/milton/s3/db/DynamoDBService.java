/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.db;

import io.milton.s3.model.Entity;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/** Low-level DynamoDB operations used by the metadata manager. */
public interface DynamoDBService {

  boolean createTable(String tableName);

  boolean deleteTable(String tableName);

  boolean isTableExist(String tableName);

  Map<String, AttributeValue> newItem(Entity entity);

  PutItemResponse putItem(String tableName, Map<String, AttributeValue> item);

  Map<String, AttributeValue> getItem(String tableName, Map<String, AttributeValue> primaryKey);

  List<Map<String, AttributeValue>> scanItems(String tableName, Map<String, Condition> conditions);

  UpdateItemResponse updateItem(
      String tableName,
      Map<String, AttributeValue> primaryKey,
      Map<String, AttributeValueUpdate> updateItems);

  DeleteItemResponse deleteItem(String tableName, Map<String, AttributeValue> primaryKey);
}
