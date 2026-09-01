package io.milton.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectAclResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class AmazonS3ManagerImplTest {

  private static final String BUCKET = "example-bucket";

  @Mock private S3Client client;

  private AmazonS3ManagerImpl manager;

  @BeforeEach
  void setUp() {
    manager = new AmazonS3ManagerImpl(client, Region.US_WEST_2);
  }

  @Test
  void checksBucketExistence() {
    when(client.headBucket(any(HeadBucketRequest.class)))
        .thenReturn(HeadBucketResponse.builder().build())
        .thenThrow(S3Exception.builder().statusCode(404).build())
        .thenThrow(S3Exception.builder().statusCode(403).message("denied").build());

    assertTrue(manager.isRootBucket(BUCKET));
    assertFalse(manager.isRootBucket(BUCKET));
    assertThrows(MiltonAwsException.class, () -> manager.isRootBucket(BUCKET));
  }

  @Test
  void createsRegionalBucketOnlyWhenMissing() {
    when(client.headBucket(any(HeadBucketRequest.class)))
        .thenThrow(S3Exception.builder().statusCode(404).build());
    when(client.createBucket(any(CreateBucketRequest.class)))
        .thenReturn(CreateBucketResponse.builder().build());

    Bucket bucket = manager.createBucket(BUCKET);

    ArgumentCaptor<CreateBucketRequest> request =
        ArgumentCaptor.forClass(CreateBucketRequest.class);
    verify(client).createBucket(request.capture());
    assertEquals(BUCKET, bucket.name());
    assertEquals(
        "us-west-2", request.getValue().createBucketConfiguration().locationConstraintAsString());
  }

  @Test
  void existingBucketIsNotCreatedAgain() {
    when(client.headBucket(any(HeadBucketRequest.class)))
        .thenReturn(HeadBucketResponse.builder().build());

    assertEquals(BUCKET, manager.createBucket(BUCKET).name());

    verify(client, never()).createBucket(any(CreateBucketRequest.class));
  }

  @Test
  void usEastOneOmitsLocationConstraint() {
    AmazonS3ManagerImpl eastManager = new AmazonS3ManagerImpl(client, Region.US_EAST_1);
    when(client.headBucket(any(HeadBucketRequest.class)))
        .thenThrow(S3Exception.builder().statusCode(404).build());
    when(client.createBucket(any(CreateBucketRequest.class)))
        .thenReturn(CreateBucketResponse.builder().build());

    eastManager.createBucket(BUCKET);

    ArgumentCaptor<CreateBucketRequest> request =
        ArgumentCaptor.forClass(CreateBucketRequest.class);
    verify(client).createBucket(request.capture());
    assertNull(request.getValue().createBucketConfiguration());
  }

  @Test
  void listsAndDeletesBucket() {
    Bucket bucket = Bucket.builder().name(BUCKET).build();
    when(client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(bucket).build());
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().build());
    when(client.deleteBucket(any(DeleteBucketRequest.class)))
        .thenReturn(DeleteBucketResponse.builder().build());

    assertEquals(List.of(bucket), manager.findBuckets());
    assertTrue(manager.deleteBucket(BUCKET));
  }

  @Test
  void uploadsFilesAndStreamsWithMetadata(@TempDir java.nio.file.Path directory) throws Exception {
    java.nio.file.Path source = directory.resolve("source.txt");
    java.nio.file.Files.writeString(source, "content");
    when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    assertTrue(manager.uploadEntity(BUCKET, "file", source.toFile()));
    assertTrue(
        manager.uploadEntity(
            BUCKET,
            "stream",
            new ByteArrayInputStream(new byte[] {1, 2}),
            2,
            "application/octet-stream"));

    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client, times(2)).putObject(request.capture(), any(RequestBody.class));
    assertEquals("application/octet-stream", request.getAllValues().get(1).contentType());
    assertThrows(
        IllegalArgumentException.class,
        () -> manager.uploadEntity(BUCKET, "invalid", InputStream.nullInputStream(), -1, null));
  }

  @Test
  void deletesObjectsInS3SizedBatches() {
    List<S3Object> objects =
        IntStream.range(0, 1_001)
            .mapToObj(index -> S3Object.builder().key("key-" + index).build())
            .toList();
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(ListObjectsV2Response.builder().contents(objects).build());
    when(client.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    assertTrue(manager.deleteEntities(BUCKET));

    verify(client, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void deletesAndCopiesSingleObject() {
    when(client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    assertTrue(manager.deleteEntity(BUCKET, "old/key"));
    assertTrue(manager.copyEntity(BUCKET, "old/key", null, "new/key"));

    ArgumentCaptor<CopyObjectRequest> request = ArgumentCaptor.forClass(CopyObjectRequest.class);
    verify(client).copyObject(request.capture());
    assertEquals(BUCKET, request.getValue().sourceBucket());
    assertEquals("old/key", request.getValue().sourceKey());
    assertEquals(BUCKET, request.getValue().destinationBucket());
    assertEquals("new/key", request.getValue().destinationKey());
  }

  @Test
  void managesPublicReadAcl() {
    Grant publicRead =
        Grant.builder()
            .permission(Permission.READ)
            .grantee(
                Grantee.builder().uri("http://acs.amazonaws.com/groups/global/AllUsers").build())
            .build();
    when(client.putObjectAcl(any(PutObjectAclRequest.class)))
        .thenReturn(PutObjectAclResponse.builder().build());
    when(client.getObjectAcl(any(GetObjectAclRequest.class)))
        .thenReturn(GetObjectAclResponse.builder().grants(publicRead).build())
        .thenReturn(GetObjectAclResponse.builder().build());

    assertTrue(manager.publicEntity(BUCKET, "key"));
    assertTrue(manager.isPublicEntity(BUCKET, "key"));
    assertFalse(manager.isPublicEntity(BUCKET, "key"));
  }

  @Test
  void downloadsToStreamAndFile(@TempDir java.nio.file.Path directory) {
    ResponseInputStream<GetObjectResponse> stream =
        new ResponseInputStream<>(
            GetObjectResponse.builder().build(),
            new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
    when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream);
    when(client.getObject(
            any(GetObjectRequest.class),
            org.mockito.ArgumentMatchers
                .<ResponseTransformer<GetObjectResponse, GetObjectResponse>>any()))
        .thenReturn(GetObjectResponse.builder().build());

    assertSame(stream, manager.downloadEntity(BUCKET, "key"));
    assertTrue(manager.downloadEntity(BUCKET, "key", directory.resolve("target").toFile()));
  }

  @Test
  void checksObjectExistence() {
    when(client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().build())
        .thenThrow(S3Exception.builder().statusCode(404).build());

    assertTrue(manager.entityExists(BUCKET, "key"));
    assertFalse(manager.entityExists(BUCKET, "missing"));
  }

  @Test
  void listsAllPagesAndPassesPrefix() {
    S3Object first = S3Object.builder().key("prefix/one").build();
    S3Object second = S3Object.builder().key("prefix/two").build();
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(
            ListObjectsV2Response.builder()
                .contents(first)
                .isTruncated(true)
                .nextContinuationToken("next")
                .build(),
            ListObjectsV2Response.builder().contents(second).build());

    assertEquals(List.of(first, second), manager.findEntityByPrefixKey(BUCKET, "prefix/"));

    ArgumentCaptor<ListObjectsV2Request> request =
        ArgumentCaptor.forClass(ListObjectsV2Request.class);
    verify(client, times(2)).listObjectsV2(request.capture());
    assertEquals("prefix/", request.getAllValues().get(0).prefix());
    assertEquals("next", request.getAllValues().get(1).continuationToken());
  }

  @Test
  void validatesInputsAndWrapsSdkFailures() {
    assertThrows(IllegalArgumentException.class, () -> manager.findEntityByBucket(" "));
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenThrow(S3Exception.builder().statusCode(500).message("failure").build());
    assertThrows(MiltonAwsException.class, () -> manager.findEntityByBucket(BUCKET));
  }
}
