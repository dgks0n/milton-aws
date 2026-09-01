package io.milton.s3.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.service.AmazonStorageService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmazonS3ControllerTest {

  private static final String BUCKET = "test-bucket";

  @Mock private AmazonStorageService storage;

  private AmazonS3Controller controller;

  @BeforeEach
  void setUp() {
    controller = new AmazonS3Controller(storage, BUCKET);
  }

  @Test
  void constructorInitializesBucketAndValidatesConfiguration() {
    verify(storage).createBucket(BUCKET);
    assertThrows(IllegalArgumentException.class, () -> new AmazonS3Controller(storage, " "));
    assertThrows(NullPointerException.class, () -> new AmazonS3Controller(null, BUCKET));
  }

  @Test
  void exposesRootAndChildren() {
    Folder root = new Folder("/", null);
    Folder child = new Folder("child", root);
    when(storage.findRootFolder(BUCKET)).thenReturn(root);
    when(storage.findEntityByParent(BUCKET, root)).thenReturn(List.of(child));

    assertSame(root, controller.getRootFolder());
    assertSame(controller, controller.getChildren(controller));
    assertEquals(List.of(child), controller.getChildren(root));
    assertTrue(controller.getChildren((Folder) null).isEmpty());
  }

  @Test
  void createsFolderOrReportsFailure() {
    Folder root = new Folder("/", null);
    when(storage.putEntity(eq(BUCKET), any(Folder.class), eq(null))).thenReturn(true, false);

    Folder folder = controller.createFolder(root, "docs");

    assertEquals("docs", folder.getName());
    assertSame(root, folder.getParent());
    assertThrows(MiltonAwsException.class, () -> controller.createFolder(root, "failed"));
  }

  @Test
  void createsFileWithExplicitOrDetectedContentType() {
    Folder root = new Folder("/", null);
    when(storage.putEntity(eq(BUCKET), any(File.class), any(InputStream.class))).thenReturn(true);

    File explicit =
        controller.createFile(
            root, "data.bin", new ByteArrayInputStream(new byte[3]), 3L, "custom/type");
    File detected =
        controller.createFile(root, "readme.txt", new ByteArrayInputStream(new byte[0]), 0L, null);
    File fallback =
        controller.createFile(
            root,
            "unknown.extension-not-registered",
            new ByteArrayInputStream(new byte[0]),
            0L,
            " ");

    assertEquals(3L, explicit.getSize());
    assertEquals("custom/type", explicit.getContentType());
    assertEquals("text/plain", detected.getContentType());
    assertEquals("application/octet-stream", fallback.getContentType());
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.createFile(root, "bad", InputStream.nullInputStream(), -1L, null));
  }

  @Test
  void reportsFileCreationFailure() {
    Folder root = new Folder("/", null);
    when(storage.putEntity(eq(BUCKET), any(File.class), any(InputStream.class))).thenReturn(false);

    assertThrows(
        MiltonAwsException.class,
        () ->
            controller.createFile(
                root, "bad.txt", InputStream.nullInputStream(), 0L, "text/plain"));
  }

  @Test
  void distinguishesRenameFromMove() {
    Folder original = new Folder("original", null);
    Folder target = new Folder("target", null);
    File file = new File("file", original);
    when(storage.updateEntityByUniqueId(BUCKET, file, original, "renamed", true)).thenReturn(true);
    when(storage.updateEntityByUniqueId(BUCKET, file, target, "moved", false)).thenReturn(true);

    controller.renameOrMoveEntity(file, original, "renamed");
    controller.renameOrMoveEntity(file, target, "moved");

    verify(storage).updateEntityByUniqueId(BUCKET, file, original, "renamed", true);
    verify(storage).updateEntityByUniqueId(BUCKET, file, target, "moved", false);
  }

  @Test
  void reportsMoveAndCopyFailures() {
    Folder source = new Folder("source", null);
    Folder target = new Folder("target", null);
    File file = new File("file", source);
    when(storage.updateEntityByUniqueId(BUCKET, file, target, "moved", false)).thenReturn(false);
    when(storage.copyEntityByUniqueId(BUCKET, file, target, null, "copied")).thenReturn(false);

    assertThrows(
        MiltonAwsException.class, () -> controller.renameOrMoveEntity(file, target, "moved"));
    assertThrows(MiltonAwsException.class, () -> controller.copyFile(file, target, "copied"));
  }

  @Test
  void copiesEntity() {
    Folder source = new Folder("source", null);
    Folder target = new Folder("target", null);
    File file = new File("file", source);
    when(storage.copyEntityByUniqueId(BUCKET, file, target, null, "copied")).thenReturn(true);

    controller.copyFile(file, target, "copied");

    verify(storage).copyEntityByUniqueId(BUCKET, file, target, null, "copied");
  }

  @Test
  void exposesMiltonMetadata() {
    Folder root = new Folder("/", null);
    File file = new File("report.pdf", root);
    file.setSize(99);
    file.setContentType("application/pdf");

    assertEquals("report.pdf", controller.getResourceName(file));
    assertEquals("report.pdf", controller.getDisplayName(file));
    assertEquals(0L, controller.getContentLength());
    assertEquals(0L, controller.getContentLength(root));
    assertEquals(99L, controller.getContentLength(file));
    assertEquals("", controller.getContentType(root));
    assertEquals("application/pdf", controller.getContentType(file));
    assertEquals(file.getCreatedDate(), controller.getCreatedDate(file));
    assertEquals(file.getModifiedDate(), controller.getModifiedDate(file));
    assertEquals(file.getId().toString(), controller.getUniqueId(file));
  }

  @Test
  void downloadsFileAndReportsMissingStream() {
    Folder root = new Folder("/", null);
    File file = new File("file", root);
    InputStream stream = InputStream.nullInputStream();
    String key = root.getId() + "/" + file.getId();
    when(storage.downloadEntityByUniqueId(BUCKET, key)).thenReturn(stream).thenReturn(null);

    assertSame(stream, controller.downloadFile(file));
    assertThrows(MiltonAwsException.class, () -> controller.downloadFile(file));
    assertThrows(
        IllegalArgumentException.class, () -> controller.downloadFile(new File("orphan", null)));
  }

  @Test
  void deletesEntityOrReportsFailure() {
    Entity file = new File("file", new Folder("/", null));
    when(storage.deleteEntity(BUCKET, file)).thenReturn(true, false);

    controller.deleteFileOrFolder(file);
    assertThrows(MiltonAwsException.class, () -> controller.deleteFileOrFolder(file));
    verify(storage, never()).deleteEntityByUniqueId(BUCKET, "unrelated");
  }
}
