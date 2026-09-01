/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.db;

import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

/** AWS SDK v2 implementation of {@link DynamoDBService}. */
public final class DynamoDBServiceImpl implements DynamoDBService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBServiceImpl.class);

  private final DynamoDbClient dynamoDbClient;

  /** Creates a client using the AWS default credentials provider chain. */
  public DynamoDBServiceImpl(Region region) {
    this(
        DynamoDbClient.builder()
            .region(Objects.requireNonNull(region, "region"))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build());
  }

  /** Creates a service around an externally configured client. */
  public DynamoDBServiceImpl(DynamoDbClient dynamoDbClient) {
    this.dynamoDbClient = Objects.requireNonNull(dynamoDbClient, "dynamoDbClient");
  }

  @Override
  public boolean createTable(String tableName) {
    requireText(tableName, "tableName");
    CreateTableRequest request =
        CreateTableRequest.builder()
            .tableName(tableName)
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName(AttributeKey.UUID)
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName(AttributeKey.UUID)
                    .keyType(KeyType.HASH)
                    .build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
    try {
      dynamoDbClient.createTable(request);
    } catch (ResourceInUseException exception) {
      return true;
    } catch (RuntimeException exception) {
      throw failure("create table " + tableName, exception);
    }

    try (DynamoDbWaiter waiter = dynamoDbClient.waiter()) {
      assertWaiterSucceeded(
          waiter.waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build()),
          "wait for table " + tableName);
    }
    return true;
  }

  @Override
  public boolean deleteTable(String tableName) {
    requireText(tableName, "tableName");
    try {
      dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    } catch (ResourceNotFoundException exception) {
      return true;
    } catch (RuntimeException exception) {
      throw failure("delete table " + tableName, exception);
    }

    try (DynamoDbWaiter waiter = dynamoDbClient.waiter()) {
      assertWaiterSucceeded(
          waiter.waitUntilTableNotExists(
              DescribeTableRequest.builder().tableName(tableName).build()),
          "wait for table deletion " + tableName);
    }
    return true;
  }

  @Override
  public boolean isTableExist(String tableName) {
    requireText(tableName, "tableName");
    try {
      dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
      return true;
    } catch (ResourceNotFoundException exception) {
      return false;
    } catch (RuntimeException exception) {
      throw failure("describe table " + tableName, exception);
    }
  }

  @Override
  public Map<String, AttributeValue> newItem(Entity entity) {
    Objects.requireNonNull(entity, "entity");
    Map<String, AttributeValue> item = new LinkedHashMap<>();
    item.put(AttributeKey.UUID, stringValue(entity.getId().toString()));
    item.put(AttributeKey.ENTITY_NAME, stringValue(entity.getName()));
    item.put(
        AttributeKey.PARENT_UUID,
        stringValue(
            entity.getParent() == null
                ? AttributeKey.NOT_EXIST
                : entity.getParent().getId().toString()));
    item.put(AttributeKey.IS_DIRECTORY, numberValue(entity.isDirectory() ? 1 : 0));

    long fileSize = 0;
    String contentType = AttributeKey.NOT_EXIST;
    if (entity instanceof File file) {
      fileSize = file.getSize();
      if (file.getContentType() != null && !file.getContentType().isBlank()) {
        contentType = file.getContentType();
      }
    }
    item.put(AttributeKey.FILE_SIZE, numberValue(fileSize));
    item.put(AttributeKey.CONTENT_TYPE, stringValue(contentType));
    item.put(
        AttributeKey.CREATED_DATE, stringValue(DateUtils.dateToString(entity.getCreatedDate())));
    item.put(
        AttributeKey.MODIFIED_DATE, stringValue(DateUtils.dateToString(entity.getModifiedDate())));
    return Map.copyOf(item);
  }

  @Override
  public PutItemResponse putItem(String tableName, Map<String, AttributeValue> item) {
    requireText(tableName, "tableName");
    if (item == null || item.isEmpty()) {
      throw new IllegalArgumentException("item must not be empty");
    }
    try {
      return dynamoDbClient.putItem(
          PutItemRequest.builder().tableName(tableName).item(item).build());
    } catch (RuntimeException exception) {
      throw failure("put item in table " + tableName, exception);
    }
  }

  @Override
  public Map<String, AttributeValue> getItem(
      String tableName, Map<String, AttributeValue> primaryKey) {
    requireText(tableName, "tableName");
    if (primaryKey == null || primaryKey.isEmpty()) {
      throw new IllegalArgumentException("primaryKey must not be empty");
    }
    try {
      Map<String, AttributeValue> item =
          dynamoDbClient
              .getItem(
                  GetItemRequest.builder()
                      .tableName(tableName)
                      .key(primaryKey)
                      .consistentRead(true)
                      .build())
              .item();
      return item == null ? Map.of() : Map.copyOf(item);
    } catch (RuntimeException exception) {
      throw failure("get item from table " + tableName, exception);
    }
  }

  @Override
  public List<Map<String, AttributeValue>> scanItems(
      String tableName, Map<String, Condition> conditions) {
    requireText(tableName, "tableName");
    if (conditions == null || conditions.isEmpty()) {
      throw new IllegalArgumentException("conditions must not be empty");
    }

    List<Map<String, AttributeValue>> items = new ArrayList<>();
    Map<String, AttributeValue> startKey = null;
    try {
      do {
        ScanRequest request =
            ScanRequest.builder()
                .tableName(tableName)
                .scanFilter(conditions)
                .exclusiveStartKey(startKey)
                .build();
        ScanResponse response = dynamoDbClient.scan(request);
        items.addAll(response.items());
        startKey = response.lastEvaluatedKey();
      } while (startKey != null && !startKey.isEmpty());
      return List.copyOf(items);
    } catch (RuntimeException exception) {
      throw failure("scan table " + tableName, exception);
    }
  }

  @Override
  public UpdateItemResponse updateItem(
      String tableName,
      Map<String, AttributeValue> primaryKey,
      Map<String, AttributeValueUpdate> updateItems) {
    requireText(tableName, "tableName");
    if (primaryKey == null || primaryKey.isEmpty()) {
      throw new IllegalArgumentException("primaryKey must not be empty");
    }
    if (updateItems == null || updateItems.isEmpty()) {
      throw new IllegalArgumentException("updateItems must not be empty");
    }
    try {
      return dynamoDbClient.updateItem(
          UpdateItemRequest.builder()
              .tableName(tableName)
              .key(primaryKey)
              .attributeUpdates(updateItems)
              .returnValues(ReturnValue.UPDATED_NEW)
              .build());
    } catch (RuntimeException exception) {
      throw failure("update item in table " + tableName, exception);
    }
  }

  @Override
  public DeleteItemResponse deleteItem(String tableName, Map<String, AttributeValue> primaryKey) {
    requireText(tableName, "tableName");
    if (primaryKey == null || primaryKey.isEmpty()) {
      throw new IllegalArgumentException("primaryKey must not be empty");
    }
    try {
      return dynamoDbClient.deleteItem(
          DeleteItemRequest.builder().tableName(tableName).key(primaryKey).build());
    } catch (RuntimeException exception) {
      throw failure("delete item from table " + tableName, exception);
    }
  }

  private static void assertWaiterSucceeded(
      WaiterResponse<DescribeTableResponse> response, String operation) {
    if (response.matched().response().isPresent()) {
      return;
    }
    Throwable cause = response.matched().exception().orElse(null);
    throw new MiltonAwsException("Unable to " + operation, cause);
  }

  private static AttributeValue stringValue(String value) {
    return AttributeValue.builder().s(value).build();
  }

  private static AttributeValue numberValue(long value) {
    return AttributeValue.builder().n(Long.toString(value)).build();
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  private static MiltonAwsException failure(String operation, RuntimeException cause) {
    LOGGER.warn("Unable to {}", operation, cause);
    return new MiltonAwsException("Unable to " + operation, cause);
  }
}
