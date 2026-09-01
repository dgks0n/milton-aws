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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/** AWS SDK v2 implementation of {@link AmazonS3Manager}. */
public final class AmazonS3ManagerImpl implements AmazonS3Manager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3ManagerImpl.class);
  private static final String ALL_USERS_URI = "http://acs.amazonaws.com/groups/global/AllUsers";
  private static final int MAX_DELETE_BATCH_SIZE = 1_000;

  private final S3Client s3Client;
  private final Region region;

  /** Creates a client using the AWS default credentials provider chain. */
  public AmazonS3ManagerImpl(Region region) {
    this(
        S3Client.builder()
            .region(Objects.requireNonNull(region, "region"))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build(),
        region);
  }

  /** Creates a manager around an externally configured client. */
  public AmazonS3ManagerImpl(S3Client s3Client, Region region) {
    this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
    this.region = Objects.requireNonNull(region, "region");
  }

  @Override
  public boolean isRootBucket(String bucketName) {
    requireText(bucketName, "bucketName");
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      return true;
    } catch (NoSuchBucketException exception) {
      return false;
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return false;
      }
      throw failure("check bucket " + bucketName, exception);
    } catch (SdkException exception) {
      throw failure("check bucket " + bucketName, exception);
    }
  }

  @Override
  public Bucket createBucket(String bucketName) {
    requireText(bucketName, "bucketName");
    if (!isRootBucket(bucketName)) {
      CreateBucketRequest.Builder request = CreateBucketRequest.builder().bucket(bucketName);
      if (!Region.US_EAST_1.equals(region)) {
        request.createBucketConfiguration(
            CreateBucketConfiguration.builder()
                .locationConstraint(BucketLocationConstraint.fromValue(region.id()))
                .build());
      }
      try {
        s3Client.createBucket(request.build());
      } catch (SdkException exception) {
        throw failure("create bucket " + bucketName, exception);
      }
    }
    return Bucket.builder().name(bucketName).build();
  }

  @Override
  public boolean deleteBucket(String bucketName) {
    requireText(bucketName, "bucketName");
    deleteEntities(bucketName);
    try {
      s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
      return true;
    } catch (SdkException exception) {
      throw failure("delete bucket " + bucketName, exception);
    }
  }

  @Override
  public List<Bucket> findBuckets() {
    try {
      return List.copyOf(s3Client.listBuckets().buckets());
    } catch (SdkException exception) {
      throw failure("list buckets", exception);
    }
  }

  @Override
  public boolean uploadEntity(String bucketName, String keyName, java.io.File file) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    Objects.requireNonNull(file, "file");
    try {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(bucketName).key(keyName).build(),
          RequestBody.fromFile(file));
      return true;
    } catch (SdkException exception) {
      throw failure("upload object " + keyName, exception);
    }
  }

  @Override
  public boolean uploadEntity(
      String bucketName,
      String keyName,
      InputStream inputStream,
      long contentLength,
      String contentType) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    Objects.requireNonNull(inputStream, "inputStream");
    if (contentLength < 0) {
      throw new IllegalArgumentException("contentLength must not be negative");
    }

    PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucketName).key(keyName);
    if (contentType != null && !contentType.isBlank()) {
      request.contentType(contentType);
    }
    try {
      s3Client.putObject(request.build(), RequestBody.fromInputStream(inputStream, contentLength));
      return true;
    } catch (SdkException exception) {
      throw failure("upload object " + keyName, exception);
    }
  }

  @Override
  public boolean deleteEntity(String bucketName, String keyName) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(keyName).build());
      return true;
    } catch (SdkException exception) {
      throw failure("delete object " + keyName, exception);
    }
  }

  @Override
  public boolean deleteEntities(String bucketName) {
    List<S3Object> objects = findEntityByBucket(bucketName);
    for (int start = 0; start < objects.size(); start += MAX_DELETE_BATCH_SIZE) {
      int end = Math.min(start + MAX_DELETE_BATCH_SIZE, objects.size());
      List<ObjectIdentifier> identifiers =
          objects.subList(start, end).stream()
              .map(object -> ObjectIdentifier.builder().key(object.key()).build())
              .toList();
      try {
        s3Client.deleteObjects(
            DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(identifiers).quiet(true).build())
                .build());
      } catch (SdkException exception) {
        throw failure("delete objects from bucket " + bucketName, exception);
      }
    }
    return true;
  }

  @Override
  public boolean publicEntity(String bucketName, String keyName) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    try {
      s3Client.putObjectAcl(
          PutObjectAclRequest.builder()
              .bucket(bucketName)
              .key(keyName)
              .acl(ObjectCannedACL.PUBLIC_READ)
              .build());
      return true;
    } catch (SdkException exception) {
      throw failure("make object public " + keyName, exception);
    }
  }

  @Override
  public boolean copyEntity(
      String sourceBucketName,
      String sourceKeyName,
      String destinationBucketName,
      String destinationKeyName) {
    requireText(sourceBucketName, "sourceBucketName");
    requireText(sourceKeyName, "sourceKeyName");
    requireText(destinationKeyName, "destinationKeyName");
    String targetBucket =
        destinationBucketName == null || destinationBucketName.isBlank()
            ? sourceBucketName
            : destinationBucketName;
    try {
      s3Client.copyObject(
          CopyObjectRequest.builder()
              .sourceBucket(sourceBucketName)
              .sourceKey(sourceKeyName)
              .destinationBucket(targetBucket)
              .destinationKey(destinationKeyName)
              .build());
      return true;
    } catch (SdkException exception) {
      throw failure("copy object " + sourceKeyName, exception);
    }
  }

  @Override
  public boolean isPublicEntity(String bucketName, String keyName) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    try {
      return s3Client
          .getObjectAcl(GetObjectAclRequest.builder().bucket(bucketName).key(keyName).build())
          .grants()
          .stream()
          .anyMatch(
              grant ->
                  Permission.READ.equals(grant.permission())
                      && ALL_USERS_URI.equals(grant.grantee().uri()));
    } catch (SdkException exception) {
      throw failure("read object ACL " + keyName, exception);
    }
  }

  @Override
  public boolean downloadEntity(String bucketName, String keyName, java.io.File destinationFile) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    Objects.requireNonNull(destinationFile, "destinationFile");
    try {
      s3Client.getObject(
          GetObjectRequest.builder().bucket(bucketName).key(keyName).build(),
          ResponseTransformer.toFile(destinationFile.toPath()));
      return true;
    } catch (SdkException exception) {
      throw failure("download object " + keyName, exception);
    }
  }

  @Override
  public ResponseInputStream<GetObjectResponse> downloadEntity(String bucketName, String keyName) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    try {
      return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(keyName).build());
    } catch (SdkException exception) {
      throw failure("download object " + keyName, exception);
    }
  }

  @Override
  public boolean entityExists(String bucketName, String keyName) {
    requireText(bucketName, "bucketName");
    requireText(keyName, "keyName");
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(keyName).build());
      return true;
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return false;
      }
      throw failure("check object " + keyName, exception);
    } catch (SdkException exception) {
      throw failure("check object " + keyName, exception);
    }
  }

  @Override
  public List<S3Object> findEntityByBucket(String bucketName) {
    return listObjects(bucketName, null);
  }

  @Override
  public List<S3Object> findEntityByPrefixKey(String bucketName, String prefixKey) {
    requireText(prefixKey, "prefixKey");
    return listObjects(bucketName, prefixKey);
  }

  private List<S3Object> listObjects(String bucketName, String prefix) {
    requireText(bucketName, "bucketName");
    List<S3Object> objects = new ArrayList<>();
    String continuationToken = null;
    try {
      boolean truncated;
      do {
        ListObjectsV2Request.Builder request =
            ListObjectsV2Request.builder().bucket(bucketName).continuationToken(continuationToken);
        if (prefix != null) {
          request.prefix(prefix);
        }
        ListObjectsV2Response response = s3Client.listObjectsV2(request.build());
        objects.addAll(response.contents());
        continuationToken = response.nextContinuationToken();
        truncated = Boolean.TRUE.equals(response.isTruncated());
      } while (truncated);
      return List.copyOf(objects);
    } catch (SdkException exception) {
      throw failure("list objects in bucket " + bucketName, exception);
    }
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  private static MiltonAwsException failure(String operation, SdkException cause) {
    LOGGER.warn("Unable to {}", operation, cause);
    return new MiltonAwsException("Unable to " + operation, cause);
  }
}
