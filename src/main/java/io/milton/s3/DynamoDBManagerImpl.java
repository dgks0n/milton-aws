/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3;

import io.milton.s3.db.DynamoDBService;
import io.milton.s3.db.DynamoDBServiceImpl;
import io.milton.s3.db.mapper.DynamoDBEntityMapper;
import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

/** Default {@link DynamoDBManager} implementation. */
public final class DynamoDBManagerImpl implements DynamoDBManager {

  private final DynamoDBService dynamoDBService;

  public DynamoDBManagerImpl(Region region) {
    this(new DynamoDBServiceImpl(region));
  }

  public DynamoDBManagerImpl(DynamoDBService dynamoDBService) {
    this.dynamoDBService = Objects.requireNonNull(dynamoDBService, "dynamoDBService");
  }

  @Override
  public boolean createTable(String tableName) {
    return dynamoDBService.isTableExist(tableName) || dynamoDBService.createTable(tableName);
  }

  @Override
  public boolean deleteTable(String tableName) {
    return dynamoDBService.deleteTable(tableName);
  }

  @Override
  public boolean isExistEntity(String tableName, String entityName, Folder parent) {
    if (entityName == null || entityName.isBlank()) {
      return false;
    }
    Map<String, Condition> conditions =
        Map.of(
            AttributeKey.PARENT_UUID,
            equalsString(parent == null ? AttributeKey.NOT_EXIST : parent.getId().toString()),
            AttributeKey.ENTITY_NAME,
            equalsString(entityName));
    return !dynamoDBService.scanItems(tableName, conditions).isEmpty();
  }

  @Override
  public boolean putEntity(String tableName, Entity entity) {
    Objects.requireNonNull(entity, "entity");
    return dynamoDBService.putItem(tableName, dynamoDBService.newItem(entity)) != null;
  }

  @Override
  public Folder findRootFolder(String tableName) {
    List<Entity> entities =
        DynamoDBEntityMapper.convertItemsToEntities(
            null,
            dynamoDBService.scanItems(
                tableName, Map.of(AttributeKey.PARENT_UUID, equalsString(AttributeKey.NOT_EXIST))));
    if (entities.isEmpty()) {
      return null;
    }
    Entity root = entities.get(0);
    if (!(root instanceof Folder folder)) {
      throw new MiltonAwsException("Stored root entity is not a folder");
    }
    return folder;
  }

  @Override
  public Entity findEntityByUniqueId(String tableName, Entity entity) {
    if (entity == null) {
      return null;
    }
    return findEntityByUniqueId(tableName, entity.getId().toString(), entity.getParent());
  }

  @Override
  public Entity findEntityByUniqueId(String tableName, String uniqueId, Folder parent) {
    if (uniqueId == null || uniqueId.isBlank()) {
      return null;
    }
    return DynamoDBEntityMapper.convertItemToEntity(
        parent,
        dynamoDBService.getItem(
            tableName, Map.of(AttributeKey.UUID, AttributeValue.builder().s(uniqueId).build())));
  }

  @Override
  public List<Entity> findEntityByParent(String tableName, Folder parent) {
    if (parent == null) {
      return List.of();
    }
    return DynamoDBEntityMapper.convertItemsToEntities(
        parent,
        dynamoDBService.scanItems(
            tableName, Map.of(AttributeKey.PARENT_UUID, equalsString(parent.getId().toString()))));
  }

  @Override
  public List<Entity> findEntityByParentAndType(
      String tableName, Folder parent, boolean isDirectory) {
    if (parent == null) {
      return List.of();
    }
    Map<String, Condition> conditions =
        Map.of(
            AttributeKey.PARENT_UUID,
            equalsString(parent.getId().toString()),
            AttributeKey.IS_DIRECTORY,
            equalsNumber(isDirectory ? 1 : 0));
    return DynamoDBEntityMapper.convertItemsToEntities(
        parent, dynamoDBService.scanItems(tableName, conditions));
  }

  @Override
  public boolean updateEntityByUniqueId(
      String tableName,
      Entity entity,
      Folder newParent,
      String newEntityName,
      boolean isRenamingAction) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(newParent, "newParent");
    if (newEntityName == null || newEntityName.isBlank()) {
      throw new IllegalArgumentException("newEntityName must not be blank");
    }

    Map<String, AttributeValueUpdate> updates = new java.util.LinkedHashMap<>();
    updates.put(AttributeKey.ENTITY_NAME, putString(newEntityName));
    updates.put(AttributeKey.MODIFIED_DATE, putString(DateUtils.dateToString(new Date())));
    if (!isRenamingAction) {
      updates.put(AttributeKey.PARENT_UUID, putString(newParent.getId().toString()));
    }

    boolean updated =
        dynamoDBService.updateItem(
                tableName,
                Map.of(
                    AttributeKey.UUID,
                    AttributeValue.builder().s(entity.getId().toString()).build()),
                updates)
            != null;
    if (updated) {
      entity.setName(newEntityName);
      if (!isRenamingAction) {
        entity.setParent(newParent);
      }
    }
    return updated;
  }

  @Override
  public boolean deleteEntityByUniqueId(String tableName, String uniqueId) {
    if (uniqueId == null || uniqueId.isBlank()) {
      return false;
    }
    return dynamoDBService.deleteItem(
            tableName, Map.of(AttributeKey.UUID, AttributeValue.builder().s(uniqueId).build()))
        != null;
  }

  private static Condition equalsString(String value) {
    return Condition.builder()
        .comparisonOperator(ComparisonOperator.EQ)
        .attributeValueList(AttributeValue.builder().s(value).build())
        .build();
  }

  private static Condition equalsNumber(int value) {
    return Condition.builder()
        .comparisonOperator(ComparisonOperator.EQ)
        .attributeValueList(AttributeValue.builder().n(Integer.toString(value)).build())
        .build();
  }

  private static AttributeValueUpdate putString(String value) {
    return AttributeValueUpdate.builder()
        .action(AttributeAction.PUT)
        .value(AttributeValue.builder().s(value).build())
        .build();
  }
}
