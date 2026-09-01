/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.db.mapper;

import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Converts DynamoDB attribute maps to domain entities. */
public final class DynamoDBEntityMapper {

  private DynamoDBEntityMapper() {}

  public static List<Entity> convertItemsToEntities(
      Folder parent, List<Map<String, AttributeValue>> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }
    List<Entity> entities = new ArrayList<>(items.size());
    for (Map<String, AttributeValue> item : items) {
      Entity entity = convertItemToEntity(parent, item);
      if (entity != null) {
        entities.add(entity);
      }
    }
    return List.copyOf(entities);
  }

  public static Entity convertItemToEntity(Folder parent, Map<String, AttributeValue> item) {
    if (item == null || item.isEmpty()) {
      return null;
    }

    UUID id = UUID.fromString(requiredString(item, AttributeKey.UUID));
    String name = requiredString(item, AttributeKey.ENTITY_NAME);
    Date createdDate = DateUtils.dateFromString(requiredString(item, AttributeKey.CREATED_DATE));
    Date modifiedDate = DateUtils.dateFromString(requiredString(item, AttributeKey.MODIFIED_DATE));
    boolean directory = "1".equals(requiredAttribute(item, AttributeKey.IS_DIRECTORY).n());

    if (directory) {
      return new Folder(id, name, createdDate, modifiedDate, parent);
    }

    File file = new File(id, name, createdDate, modifiedDate, parent);
    file.setSize(Long.parseLong(requiredAttribute(item, AttributeKey.FILE_SIZE).n()));
    String contentType = requiredString(item, AttributeKey.CONTENT_TYPE);
    file.setContentType(AttributeKey.NOT_EXIST.equals(contentType) ? null : contentType);
    return file;
  }

  private static String requiredString(Map<String, AttributeValue> item, String attributeName) {
    String value = requiredAttribute(item, attributeName).s();
    if (value == null) {
      throw new IllegalArgumentException("Attribute " + attributeName + " is not a string");
    }
    return value;
  }

  private static AttributeValue requiredAttribute(
      Map<String, AttributeValue> item, String attributeName) {
    AttributeValue value = item.get(attributeName);
    if (value == null) {
      throw new IllegalArgumentException("Missing attribute " + attributeName);
    }
    return value;
  }
}
