/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3;

import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;
import java.util.List;

/** Metadata operations backed by DynamoDB. */
public interface DynamoDBManager {

  boolean createTable(String tableName);

  boolean deleteTable(String tableName);

  boolean isExistEntity(String tableName, String entityName, Folder parent);

  boolean putEntity(String tableName, Entity entity);

  Folder findRootFolder(String tableName);

  Entity findEntityByUniqueId(String tableName, Entity entity);

  Entity findEntityByUniqueId(String tableName, String uniqueId, Folder parent);

  List<Entity> findEntityByParent(String tableName, Folder parent);

  List<Entity> findEntityByParentAndType(String tableName, Folder parent, boolean isDirectory);

  boolean updateEntityByUniqueId(
      String tableName,
      Entity entity,
      Folder newParent,
      String newEntityName,
      boolean isRenamingAction);

  boolean deleteEntityByUniqueId(String tableName, String uniqueId);
}
