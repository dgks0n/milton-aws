/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3;

import java.io.InputStream;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Low-level Amazon S3 operations used by the storage service. */
public interface AmazonS3Manager {

  boolean isRootBucket(String bucketName);

  Bucket createBucket(String bucketName);

  boolean deleteBucket(String bucketName);

  List<Bucket> findBuckets();

  boolean uploadEntity(String bucketName, String keyName, java.io.File file);

  boolean uploadEntity(
      String bucketName,
      String keyName,
      InputStream inputStream,
      long contentLength,
      String contentType);

  boolean deleteEntity(String bucketName, String keyName);

  boolean deleteEntities(String bucketName);

  boolean publicEntity(String bucketName, String keyName);

  boolean copyEntity(
      String sourceBucketName,
      String sourceKeyName,
      String destinationBucketName,
      String destinationKeyName);

  boolean isPublicEntity(String bucketName, String keyName);

  boolean downloadEntity(String bucketName, String keyName, java.io.File destinationFile);

  ResponseInputStream<GetObjectResponse> downloadEntity(String bucketName, String keyName);

  boolean entityExists(String bucketName, String keyName);

  List<S3Object> findEntityByBucket(String bucketName);

  List<S3Object> findEntityByPrefixKey(String bucketName, String prefixKey);
}
