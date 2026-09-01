/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.service;

import io.milton.s3.AmazonS3Manager;
import io.milton.s3.AmazonS3ManagerImpl;
import io.milton.s3.DynamoDBManager;
import io.milton.s3.DynamoDBManagerImpl;
import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Default coordinator for S3 object data and DynamoDB metadata. */
public final class AmazonStorageServiceImpl implements AmazonStorageService {

  private static final String KEY_SEPARATOR = "/";

  private final DynamoDBManager dynamoDBManager;
  private final AmazonS3Manager amazonS3Manager;

  public AmazonStorageServiceImpl(Region region) {
    this(new DynamoDBManagerImpl(region), new AmazonS3ManagerImpl(region));
  }

  public AmazonStorageServiceImpl(
      DynamoDBManager dynamoDBManager, AmazonS3Manager amazonS3Manager) {
    this.dynamoDBManager = Objects.requireNonNull(dynamoDBManager, "dynamoDBManager");
    this.amazonS3Manager = Objects.requireNonNull(amazonS3Manager, "amazonS3Manager");
  }

  @Override
  public Bucket createBucket(String bucketName) {
    boolean bucketExisted = amazonS3Manager.isRootBucket(bucketName);
    Bucket bucket = amazonS3Manager.createBucket(bucketName);
    if (dynamoDBManager.createTable(bucketName)) {
      return bucket;
    }
    if (!bucketExisted) {
      amazonS3Manager.deleteBucket(bucketName);
    }
    throw new MiltonAwsException("Unable to create metadata table " + bucketName);
  }

  @Override
  public void deleteBucket(String bucketName) {
    amazonS3Manager.deleteBucket(bucketName);
    if (!dynamoDBManager.deleteTable(bucketName)) {
      throw new MiltonAwsException("Unable to delete metadata table " + bucketName);
    }
  }

  @Override
  public Folder findRootFolder(String bucketName) {
    Folder rootFolder = dynamoDBManager.findRootFolder(bucketName);
    if (rootFolder != null) {
      return rootFolder;
    }
    Folder newRoot = new Folder("/", null);
    if (!dynamoDBManager.putEntity(bucketName, newRoot)) {
      throw new MiltonAwsException("Unable to create root folder in " + bucketName);
    }
    return newRoot;
  }

  @Override
  public Entity findEntityByUniqueId(String bucketName, Entity entity) {
    if (entity == null) {
      return null;
    }
    if (entity instanceof File && !amazonS3Manager.entityExists(bucketName, keyFor(entity))) {
      return null;
    }
    return dynamoDBManager.findEntityByUniqueId(bucketName, entity);
  }

  @Override
  public List<Entity> findEntityByParent(String bucketName, Folder parent) {
    if (parent == null) {
      return List.of();
    }

    Map<UUID, Entity> children = new LinkedHashMap<>();
    String prefix = parent.getId() + KEY_SEPARATOR;
    for (S3Object object : amazonS3Manager.findEntityByPrefixKey(bucketName, prefix)) {
      String key = object.key();
      if (!key.startsWith(prefix)) {
        continue;
      }
      String uniqueId = key.substring(prefix.length());
      if (uniqueId.isEmpty() || uniqueId.contains(KEY_SEPARATOR)) {
        continue;
      }
      Entity entity = dynamoDBManager.findEntityByUniqueId(bucketName, uniqueId, parent);
      if (entity instanceof File file) {
        file.setSize(object.size());
        children.put(file.getId(), file);
      }
    }

    for (Entity folder : dynamoDBManager.findEntityByParentAndType(bucketName, parent, true)) {
      children.putIfAbsent(folder.getId(), folder);
    }
    return List.copyOf(children.values());
  }

  @Override
  public boolean putEntity(String bucketName, Entity entity, InputStream inputStream) {
    if (entity == null) {
      return false;
    }

    String key = keyFor(entity);
    if (entity instanceof File file) {
      if (inputStream == null) {
        throw new IllegalArgumentException("inputStream is required for a file");
      }
      if (!amazonS3Manager.uploadEntity(
          bucketName, key, inputStream, file.getSize(), file.getContentType())) {
        return false;
      }
    }

    if (dynamoDBManager.putEntity(bucketName, entity)) {
      return true;
    }
    if (entity instanceof File) {
      amazonS3Manager.deleteEntity(bucketName, key);
    }
    return false;
  }

  @Override
  public boolean copyEntityByUniqueId(
      String bucketName, Entity entity, Folder newParent, String newBucketName, String newName) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(newParent, "newParent");
    requireText(newName, "newName");
    String targetBucket =
        newBucketName == null || newBucketName.isBlank() ? bucketName : newBucketName;
    Entity copy = copyOf(entity, newParent, newName);

    if (entity instanceof File
        && !amazonS3Manager.copyEntity(bucketName, keyFor(entity), targetBucket, keyFor(copy))) {
      return false;
    }
    if (dynamoDBManager.putEntity(targetBucket, copy)) {
      if (entity instanceof Folder sourceFolder && copy instanceof Folder copiedFolder) {
        for (Entity child : findEntityByParent(bucketName, sourceFolder)) {
          if (!copyEntityByUniqueId(
              bucketName, child, copiedFolder, targetBucket, child.getName())) {
            deleteEntity(targetBucket, copiedFolder);
            return false;
          }
        }
      }
      return true;
    }
    if (copy instanceof File) {
      amazonS3Manager.deleteEntity(targetBucket, keyFor(copy));
    }
    return false;
  }

  @Override
  public boolean updateEntityByUniqueId(
      String bucketName,
      Entity entity,
      Folder newParent,
      String newEntityName,
      boolean isRenamingAction) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(newParent, "newParent");
    requireText(newEntityName, "newEntityName");

    String sourceKey = keyFor(entity);
    String destinationKey = newParent.getId() + KEY_SEPARATOR + entity.getId();
    boolean movedObject = !isRenamingAction && entity instanceof File;
    if (movedObject
        && !amazonS3Manager.copyEntity(bucketName, sourceKey, bucketName, destinationKey)) {
      return false;
    }

    boolean metadataUpdated =
        dynamoDBManager.updateEntityByUniqueId(
            bucketName, entity, newParent, newEntityName, isRenamingAction);
    if (!metadataUpdated) {
      if (movedObject) {
        amazonS3Manager.deleteEntity(bucketName, destinationKey);
      }
      return false;
    }
    if (movedObject) {
      amazonS3Manager.deleteEntity(bucketName, sourceKey);
    }
    return true;
  }

  @Override
  public boolean deleteEntityByUniqueId(String bucketName, String uniqueId) {
    if (uniqueId == null || uniqueId.isBlank()) {
      return false;
    }
    String suffix = KEY_SEPARATOR + uniqueId;
    for (S3Object object : amazonS3Manager.findEntityByBucket(bucketName)) {
      if ((object.key().equals(uniqueId) || object.key().endsWith(suffix))
          && !amazonS3Manager.deleteEntity(bucketName, object.key())) {
        return false;
      }
    }
    return dynamoDBManager.deleteEntityByUniqueId(bucketName, uniqueId);
  }

  @Override
  public boolean deleteEntity(String bucketName, Entity entity) {
    Objects.requireNonNull(entity, "entity");
    if (entity instanceof Folder folder) {
      for (Entity child : findEntityByParent(bucketName, folder)) {
        if (!deleteEntity(bucketName, child)) {
          return false;
        }
      }
    } else if (!amazonS3Manager.deleteEntity(bucketName, keyFor(entity))) {
      return false;
    }
    return dynamoDBManager.deleteEntityByUniqueId(bucketName, entity.getId().toString());
  }

  @Override
  public boolean downloadEntityByUniqueId(
      String bucketName, String keyName, java.io.File destinationFile) {
    return amazonS3Manager.downloadEntity(bucketName, keyName, destinationFile);
  }

  @Override
  public InputStream downloadEntityByUniqueId(String bucketName, String keyName) {
    return amazonS3Manager.downloadEntity(bucketName, keyName);
  }

  static String keyFor(Entity entity) {
    Objects.requireNonNull(entity, "entity");
    return entity.getParent() == null
        ? entity.getId().toString()
        : entity.getParent().getId() + KEY_SEPARATOR + entity.getId();
  }

  private static Entity copyOf(Entity entity, Folder newParent, String newName) {
    if (entity instanceof File source) {
      File copy = new File(newName, newParent);
      copy.setSize(source.getSize());
      copy.setContentType(source.getContentType());
      return copy;
    }
    return new Folder(newName, newParent);
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }
}
