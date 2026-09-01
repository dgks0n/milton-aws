/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.service;

import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;
import java.io.InputStream;
import java.util.List;
import software.amazon.awssdk.services.s3.model.Bucket;

/** Coordinates S3 object storage with DynamoDB metadata. */
public interface AmazonStorageService {

  Bucket createBucket(String bucketName);

  void deleteBucket(String bucketName);

  Folder findRootFolder(String bucketName);

  Entity findEntityByUniqueId(String bucketName, Entity entity);

  List<Entity> findEntityByParent(String bucketName, Folder parent);

  boolean putEntity(String bucketName, Entity entity, InputStream inputStream);

  boolean copyEntityByUniqueId(
      String bucketName, Entity entity, Folder newParent, String newBucketName, String newName);

  boolean updateEntityByUniqueId(
      String bucketName,
      Entity entity,
      Folder newParent,
      String newEntityName,
      boolean isRenamingAction);

  boolean deleteEntityByUniqueId(String bucketName, String uniqueId);

  boolean deleteEntity(String bucketName, Entity entity);

  boolean downloadEntityByUniqueId(String bucketName, String keyName, java.io.File destinationFile);

  InputStream downloadEntityByUniqueId(String bucketName, String keyName);
}
