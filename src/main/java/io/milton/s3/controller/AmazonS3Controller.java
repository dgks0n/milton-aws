/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3.controller;

import io.milton.annotations.ChildrenOf;
import io.milton.annotations.ContentLength;
import io.milton.annotations.ContentType;
import io.milton.annotations.Copy;
import io.milton.annotations.CreatedDate;
import io.milton.annotations.Delete;
import io.milton.annotations.DisplayName;
import io.milton.annotations.Get;
import io.milton.annotations.MakeCollection;
import io.milton.annotations.ModifiedDate;
import io.milton.annotations.Move;
import io.milton.annotations.Name;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.annotations.UniqueId;
import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.service.AmazonStorageService;
import io.milton.s3.service.AmazonStorageServiceImpl;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

/** Milton annotation controller exposing an AWS-backed WebDAV resource tree. */
@ResourceController
public final class AmazonS3Controller {

  public static final String DEFAULT_BUCKET_NAME = "milton-s3-demo";
  public static final String DEFAULT_REGION = "us-west-2";

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Controller.class);

  private final AmazonStorageService amazonStorageService;
  private final String bucketName;

  /** Creates a controller from system properties or environment variables. */
  public AmazonS3Controller() {
    this(
        new AmazonStorageServiceImpl(
            Region.of(config("milton.aws.region", "AWS_REGION", DEFAULT_REGION))),
        config("milton.aws.bucket", "MILTON_AWS_BUCKET", DEFAULT_BUCKET_NAME));
  }

  /** Creates an injectable controller suitable for applications and tests. */
  public AmazonS3Controller(AmazonStorageService amazonStorageService, String bucketName) {
    this.amazonStorageService =
        Objects.requireNonNull(amazonStorageService, "amazonStorageService");
    if (bucketName == null || bucketName.isBlank()) {
      throw new IllegalArgumentException("bucketName must not be blank");
    }
    this.bucketName = bucketName;
    amazonStorageService.createBucket(bucketName);
  }

  @Root
  public Folder getRootFolder() {
    return amazonStorageService.findRootFolder(bucketName);
  }

  @ChildrenOf
  public AmazonS3Controller getChildren(AmazonS3Controller rootFolder) {
    return this;
  }

  @ChildrenOf
  public List<Entity> getChildren(Folder parent) {
    return parent == null ? List.of() : amazonStorageService.findEntityByParent(bucketName, parent);
  }

  @MakeCollection
  public Folder createFolder(Folder parent, String folderName) {
    Objects.requireNonNull(parent, "parent");
    Folder folder = parent.addFolder(folderName);
    if (!amazonStorageService.putEntity(bucketName, folder, null)) {
      throw operationFailed("create folder", folderName);
    }
    return folder;
  }

  @Name
  public String getResourceName(Entity entity) {
    return Objects.requireNonNull(entity, "entity").getName();
  }

  @DisplayName
  public String getDisplayName(Entity entity) {
    return getResourceName(entity);
  }

  @PutChild
  public File createFile(
      Folder parent,
      String newName,
      InputStream inputStream,
      Long contentLength,
      String contentType) {
    Objects.requireNonNull(parent, "parent");
    Objects.requireNonNull(inputStream, "inputStream");
    if (contentLength == null || contentLength < 0) {
      throw new IllegalArgumentException("contentLength must not be negative");
    }

    File file = parent.addFile(newName);
    file.setSize(contentLength);
    file.setContentType(
        contentType == null || contentType.isBlank() ? defaultContentType(newName) : contentType);
    if (!amazonStorageService.putEntity(bucketName, file, inputStream)) {
      throw operationFailed("create file", newName);
    }
    return file;
  }

  @Move
  public void renameOrMoveEntity(Entity entity, Folder newParent, String newName) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(newParent, "newParent");
    boolean renaming = Objects.equals(entity.getParent(), newParent);
    if (!amazonStorageService.updateEntityByUniqueId(
        bucketName, entity, newParent, newName, renaming)) {
      throw operationFailed("rename or move entity", entity.getName());
    }
  }

  @Copy
  public void copyFile(Entity entity, Folder newParent, String newName) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(newParent, "newParent");
    if (!amazonStorageService.copyEntityByUniqueId(bucketName, entity, newParent, null, newName)) {
      throw operationFailed("copy entity", entity.getName());
    }
  }

  @ContentLength
  public Long getContentLength() {
    return 0L;
  }

  @ContentLength
  public Long getContentLength(Entity entity) {
    return entity instanceof File file ? file.getSize() : 0L;
  }

  @ContentType
  public String getContentType(Entity entity) {
    return entity instanceof File file ? file.getContentType() : "";
  }

  @CreatedDate
  public Date getCreatedDate(Entity entity) {
    return Objects.requireNonNull(entity, "entity").getCreatedDate();
  }

  @ModifiedDate
  public Date getModifiedDate(Entity entity) {
    return Objects.requireNonNull(entity, "entity").getModifiedDate();
  }

  @UniqueId
  public String getUniqueId(Entity entity) {
    return Objects.requireNonNull(entity, "entity").getId().toString();
  }

  @Get
  public InputStream downloadFile(File file) {
    Objects.requireNonNull(file, "file");
    if (file.getParent() == null) {
      throw new IllegalArgumentException("file must have a parent");
    }
    String keyName = file.getParent().getId() + "/" + file.getId();
    InputStream inputStream = amazonStorageService.downloadEntityByUniqueId(bucketName, keyName);
    if (inputStream == null) {
      throw operationFailed("download file", file.getName());
    }
    return inputStream;
  }

  @Delete
  public void deleteFileOrFolder(Entity entity) {
    Objects.requireNonNull(entity, "entity");
    if (!amazonStorageService.deleteEntity(bucketName, entity)) {
      throw operationFailed("delete entity", entity.getName());
    }
  }

  private MiltonAwsException operationFailed(String operation, String entityName) {
    String message = "Unable to " + operation + " '" + entityName + "' in bucket " + bucketName;
    LOGGER.warn(message);
    return new MiltonAwsException(message);
  }

  private static String defaultContentType(String fileName) {
    String detected = URLConnection.guessContentTypeFromName(fileName);
    return detected == null ? "application/octet-stream" : detected;
  }

  private static String config(String propertyName, String environmentName, String defaultValue) {
    String property = System.getProperty(propertyName);
    if (property != null && !property.isBlank()) {
      return property;
    }
    String environment = System.getenv(environmentName);
    return environment == null || environment.isBlank() ? defaultValue : environment;
  }
}
