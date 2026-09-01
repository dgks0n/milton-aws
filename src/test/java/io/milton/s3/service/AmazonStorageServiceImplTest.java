package io.milton.s3.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.milton.s3.AmazonS3Manager;
import io.milton.s3.DynamoDBManager;
import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class AmazonStorageServiceImplTest {

  private static final String BUCKET = "bucket";

  @Mock private DynamoDBManager dynamo;
  @Mock private AmazonS3Manager s3;

  private AmazonStorageServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AmazonStorageServiceImpl(dynamo, s3);
  }

  @Test
  void createsBucketAndMetadataTable() {
    Bucket bucket = Bucket.builder().name(BUCKET).build();
    when(s3.isRootBucket(BUCKET)).thenReturn(false);
    when(s3.createBucket(BUCKET)).thenReturn(bucket);
    when(dynamo.createTable(BUCKET)).thenReturn(true);

    assertSame(bucket, service.createBucket(BUCKET));
  }

  @Test
  void rollsBackNewBucketWhenMetadataCreationFails() {
    when(s3.isRootBucket(BUCKET)).thenReturn(false);
    when(s3.createBucket(BUCKET)).thenReturn(Bucket.builder().name(BUCKET).build());
    when(dynamo.createTable(BUCKET)).thenReturn(false);

    assertThrows(MiltonAwsException.class, () -> service.createBucket(BUCKET));
    verify(s3).deleteBucket(BUCKET);
  }

  @Test
  void neverDeletesPreexistingBucketDuringMetadataFailure() {
    when(s3.isRootBucket(BUCKET)).thenReturn(true);
    when(s3.createBucket(BUCKET)).thenReturn(Bucket.builder().name(BUCKET).build());
    when(dynamo.createTable(BUCKET)).thenReturn(false);

    assertThrows(MiltonAwsException.class, () -> service.createBucket(BUCKET));
    verify(s3, never()).deleteBucket(BUCKET);
  }

  @Test
  void deletesBothStores() {
    when(s3.deleteBucket(BUCKET)).thenReturn(true);
    when(dynamo.deleteTable(BUCKET)).thenReturn(true);

    service.deleteBucket(BUCKET);

    verify(s3).deleteBucket(BUCKET);
    verify(dynamo).deleteTable(BUCKET);
  }

  @Test
  void metadataDeleteFailureIsVisible() {
    when(s3.deleteBucket(BUCKET)).thenReturn(true);
    when(dynamo.deleteTable(BUCKET)).thenReturn(false);

    assertThrows(MiltonAwsException.class, () -> service.deleteBucket(BUCKET));
  }

  @Test
  void findsOrCreatesRootFolder() {
    Folder existing = new Folder("/", null);
    when(dynamo.findRootFolder("existing")).thenReturn(existing);
    when(dynamo.findRootFolder("new")).thenReturn(null);
    when(dynamo.putEntity(eq("new"), any(Folder.class))).thenReturn(true);

    assertSame(existing, service.findRootFolder("existing"));
    Folder created = service.findRootFolder("new");
    assertEquals("/", created.getName());
    assertNull(created.getParent());
  }

  @Test
  void rootCreationFailureIsVisible() {
    when(dynamo.findRootFolder(BUCKET)).thenReturn(null);
    when(dynamo.putEntity(eq(BUCKET), any(Folder.class))).thenReturn(false);

    assertThrows(MiltonAwsException.class, () -> service.findRootFolder(BUCKET));
  }

  @Test
  void findsFoldersFromMetadataAndFilesOnlyWhenObjectExists() {
    Folder folder = new Folder("folder", null);
    File file = new File("file", folder);
    when(dynamo.findEntityByUniqueId(BUCKET, folder)).thenReturn(folder);
    when(dynamo.findEntityByUniqueId(BUCKET, file)).thenReturn(file);
    when(s3.entityExists(BUCKET, folder.getId() + "/" + file.getId())).thenReturn(true, false);

    assertSame(folder, service.findEntityByUniqueId(BUCKET, folder));
    assertSame(file, service.findEntityByUniqueId(BUCKET, file));
    assertNull(service.findEntityByUniqueId(BUCKET, file));
    assertNull(service.findEntityByUniqueId(BUCKET, null));
  }

  @Test
  void combinesValidS3FilesWithDynamoFolders() {
    Folder parent = new Folder("parent", null);
    File file = new File("file", parent);
    Folder folder = new Folder("child", parent);
    String prefix = parent.getId() + "/";
    when(s3.findEntityByPrefixKey(BUCKET, prefix))
        .thenReturn(
            List.of(
                object(prefix + file.getId(), 25),
                object("wrong-prefix", 1),
                object(prefix + "nested/id", 1)));
    when(dynamo.findEntityByUniqueId(BUCKET, file.getId().toString(), parent)).thenReturn(file);
    when(dynamo.findEntityByParentAndType(BUCKET, parent, true)).thenReturn(List.of(folder));

    List<Entity> children = service.findEntityByParent(BUCKET, parent);

    assertEquals(List.of(file, folder), children);
    assertEquals(25L, file.getSize());
    assertTrue(service.findEntityByParent(BUCKET, null).isEmpty());
  }

  @Test
  void storesFoldersWithoutS3AndFilesInBothStores() {
    Folder parent = new Folder("parent", null);
    Folder folder = new Folder("folder", parent);
    File file = new File("file", parent);
    file.setSize(3);
    file.setContentType("text/plain");
    when(dynamo.putEntity(BUCKET, folder)).thenReturn(true);
    when(s3.uploadEntity(
            eq(BUCKET), anyString(), any(InputStream.class), anyLong(), eq("text/plain")))
        .thenReturn(true);
    when(dynamo.putEntity(BUCKET, file)).thenReturn(true);

    assertTrue(service.putEntity(BUCKET, folder, null));
    assertTrue(service.putEntity(BUCKET, file, new ByteArrayInputStream(new byte[] {1, 2, 3})));
    assertFalse(service.putEntity(BUCKET, null, null));
    verify(s3, never())
        .uploadEntity(eq(BUCKET), anyString(), eq((InputStream) null), anyLong(), any());
  }

  @Test
  void fileRequiresInputAndRollsBackFailedMetadataWrite() {
    Folder parent = new Folder("parent", null);
    File file = new File("file", parent);
    when(s3.uploadEntity(eq(BUCKET), anyString(), any(InputStream.class), eq(0L), eq(null)))
        .thenReturn(true);
    when(dynamo.putEntity(BUCKET, file)).thenReturn(false);

    assertThrows(IllegalArgumentException.class, () -> service.putEntity(BUCKET, file, null));
    assertFalse(service.putEntity(BUCKET, file, new ByteArrayInputStream(new byte[0])));
    verify(s3).deleteEntity(BUCKET, parent.getId() + "/" + file.getId());
  }

  @Test
  void copiesFileWithFreshIdentityAndMetadata() {
    Folder sourceParent = new Folder("source", null);
    Folder targetParent = new Folder("target", null);
    File source = new File("source.txt", sourceParent);
    source.setSize(12);
    source.setContentType("text/plain");
    when(s3.copyEntity(eq(BUCKET), anyString(), eq("target-bucket"), anyString())).thenReturn(true);
    when(dynamo.putEntity(eq("target-bucket"), any(File.class))).thenReturn(true);

    assertTrue(
        service.copyEntityByUniqueId(BUCKET, source, targetParent, "target-bucket", "copy.txt"));

    ArgumentCaptor<Entity> copy = ArgumentCaptor.forClass(Entity.class);
    verify(dynamo).putEntity(eq("target-bucket"), copy.capture());
    File copiedFile = assertInstanceOf(File.class, copy.getValue());
    assertNotEquals(source.getId(), copiedFile.getId());
    assertEquals("copy.txt", copiedFile.getName());
    assertEquals(12, copiedFile.getSize());
    assertEquals("text/plain", copiedFile.getContentType());
  }

  @Test
  void copiesFolderWithoutS3AndCleansFailedFileCopyMetadata() {
    Folder target = new Folder("target", null);
    Folder sourceFolder = new Folder("source", null);
    when(dynamo.putEntity(eq(BUCKET), any(Folder.class))).thenReturn(true);

    assertTrue(service.copyEntityByUniqueId(BUCKET, sourceFolder, target, null, "copied"));
    verify(s3, never()).copyEntity(anyString(), anyString(), anyString(), anyString());

    File sourceFile = new File("file", sourceFolder);
    when(s3.copyEntity(eq(BUCKET), anyString(), eq(BUCKET), anyString())).thenReturn(true);
    when(dynamo.putEntity(eq(BUCKET), any(File.class))).thenReturn(false);

    assertFalse(service.copyEntityByUniqueId(BUCKET, sourceFile, target, null, "copy"));
    verify(s3).deleteEntity(eq(BUCKET), anyString());
  }

  @Test
  void movesFileTransactionally() {
    Folder sourceParent = new Folder("source", null);
    Folder targetParent = new Folder("target", null);
    File file = new File("file", sourceParent);
    when(s3.copyEntity(eq(BUCKET), anyString(), eq(BUCKET), anyString())).thenReturn(true);
    when(dynamo.updateEntityByUniqueId(BUCKET, file, targetParent, "moved", false))
        .thenReturn(true);

    assertTrue(service.updateEntityByUniqueId(BUCKET, file, targetParent, "moved", false));

    InOrder order = Mockito.inOrder(s3, dynamo);
    order.verify(s3).copyEntity(eq(BUCKET), anyString(), eq(BUCKET), anyString());
    order.verify(dynamo).updateEntityByUniqueId(BUCKET, file, targetParent, "moved", false);
    order.verify(s3).deleteEntity(eq(BUCKET), anyString());
  }

  @Test
  void failedMoveMetadataDeletesNewObjectNotSource() {
    Folder sourceParent = new Folder("source", null);
    Folder targetParent = new Folder("target", null);
    File file = new File("file", sourceParent);
    String sourceKey = sourceParent.getId() + "/" + file.getId();
    String destinationKey = targetParent.getId() + "/" + file.getId();
    when(s3.copyEntity(BUCKET, sourceKey, BUCKET, destinationKey)).thenReturn(true);
    when(dynamo.updateEntityByUniqueId(BUCKET, file, targetParent, "moved", false))
        .thenReturn(false);

    assertFalse(service.updateEntityByUniqueId(BUCKET, file, targetParent, "moved", false));

    verify(s3).deleteEntity(BUCKET, destinationKey);
    verify(s3, never()).deleteEntity(BUCKET, sourceKey);
  }

  @Test
  void renameAndFolderMoveOnlyUpdateMetadata() {
    Folder parent = new Folder("parent", null);
    Folder folder = new Folder("folder", parent);
    when(dynamo.updateEntityByUniqueId(BUCKET, folder, parent, "renamed", true)).thenReturn(true);

    assertTrue(service.updateEntityByUniqueId(BUCKET, folder, parent, "renamed", true));
    verify(s3, never()).copyEntity(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void deletesMatchingObjectKeysThenMetadata() {
    String id = java.util.UUID.randomUUID().toString();
    when(s3.findEntityByBucket(BUCKET))
        .thenReturn(List.of(object("parent/" + id, 1), object("unrelated", 1)));
    when(s3.deleteEntity(BUCKET, "parent/" + id)).thenReturn(true);
    when(dynamo.deleteEntityByUniqueId(BUCKET, id)).thenReturn(true);

    assertTrue(service.deleteEntityByUniqueId(BUCKET, id));
    assertFalse(service.deleteEntityByUniqueId(BUCKET, " "));
    verify(s3, never()).deleteEntity(BUCKET, "unrelated");
  }

  @Test
  void recursivelyDeletesFolderContents() {
    Folder folder = new Folder("folder", null);
    File file = new File("file", folder);
    String key = folder.getId() + "/" + file.getId();
    when(s3.findEntityByPrefixKey(BUCKET, folder.getId() + "/"))
        .thenReturn(List.of(object(key, 1)));
    when(dynamo.findEntityByUniqueId(BUCKET, file.getId().toString(), folder)).thenReturn(file);
    when(dynamo.findEntityByParentAndType(BUCKET, folder, true)).thenReturn(List.of());
    when(s3.deleteEntity(BUCKET, key)).thenReturn(true);
    when(dynamo.deleteEntityByUniqueId(BUCKET, file.getId().toString())).thenReturn(true);
    when(dynamo.deleteEntityByUniqueId(BUCKET, folder.getId().toString())).thenReturn(true);

    assertTrue(service.deleteEntity(BUCKET, folder));

    InOrder order = Mockito.inOrder(s3, dynamo);
    order.verify(s3).deleteEntity(BUCKET, key);
    order.verify(dynamo).deleteEntityByUniqueId(BUCKET, file.getId().toString());
    order.verify(dynamo).deleteEntityByUniqueId(BUCKET, folder.getId().toString());
  }

  @Test
  void recursivelyCopiesFolderContents() {
    Folder source = new Folder("source", null);
    File child = new File("child.txt", source);
    child.setSize(4);
    String key = source.getId() + "/" + child.getId();
    when(dynamo.putEntity(eq(BUCKET), any(Folder.class))).thenReturn(true);
    when(s3.findEntityByPrefixKey(BUCKET, source.getId() + "/"))
        .thenReturn(List.of(object(key, 4)));
    when(dynamo.findEntityByUniqueId(BUCKET, child.getId().toString(), source)).thenReturn(child);
    when(dynamo.findEntityByParentAndType(BUCKET, source, true)).thenReturn(List.of());
    when(s3.copyEntity(eq(BUCKET), eq(key), eq(BUCKET), anyString())).thenReturn(true);
    when(dynamo.putEntity(eq(BUCKET), any(File.class))).thenReturn(true);

    assertTrue(
        service.copyEntityByUniqueId(BUCKET, source, new Folder("target", null), null, "copy"));

    verify(s3).copyEntity(eq(BUCKET), eq(key), eq(BUCKET), anyString());
    verify(dynamo).putEntity(eq(BUCKET), any(File.class));
  }

  @Test
  void delegatesDownloads(@TempDir java.nio.file.Path directory) {
    java.io.File destination = directory.resolve("file").toFile();
    ResponseInputStream<GetObjectResponse> stream =
        new ResponseInputStream<>(
            GetObjectResponse.builder().build(), InputStream.nullInputStream());
    when(s3.downloadEntity(BUCKET, "key", destination)).thenReturn(true);
    when(s3.downloadEntity(BUCKET, "key")).thenReturn(stream);

    assertTrue(service.downloadEntityByUniqueId(BUCKET, "key", destination));
    assertSame(stream, service.downloadEntityByUniqueId(BUCKET, "key"));
  }

  private static S3Object object(String key, long size) {
    return S3Object.builder().key(key).size(size).build();
  }
}
