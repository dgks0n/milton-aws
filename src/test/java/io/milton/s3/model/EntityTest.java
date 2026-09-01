package io.milton.s3.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityTest {

  @Test
  void folderCreatesTypedChildren() {
    Folder root = new Folder("/", null);

    Folder folder = root.addFolder("docs");
    File file = root.addFile("readme.txt");

    assertTrue(root.isDirectory());
    assertTrue(folder.isDirectory());
    assertFalse(file.isDirectory());
    assertEquals(root, folder.getParent());
    assertEquals(root, file.getParent());
  }

  @Test
  void fileStoresValidatedMetadata() {
    File file = new File("report.pdf", null);

    file.setSize(42L);
    file.setContentType("application/pdf");

    assertEquals(42L, file.getSize());
    assertEquals("application/pdf", file.getContentType());
    assertTrue(file.toString().contains("size=42"));
    assertThrows(IllegalArgumentException.class, () -> file.setSize(-1));
  }

  @Test
  void constructorAndNameRejectInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new Folder(" ", null));
    Folder folder = new Folder("valid", null);
    assertThrows(IllegalArgumentException.class, () -> folder.setName(null));
  }

  @Test
  void datesAreDefensivelyCopied() {
    Date created = new Date(1_000L);
    Date modified = new Date(2_000L);
    Folder folder = new Folder(UUID.randomUUID(), "docs", created, modified, null);

    created.setTime(9_000L);
    modified.setTime(9_000L);
    Date exposedCreated = folder.getCreatedDate();
    exposedCreated.setTime(8_000L);

    assertEquals(1_000L, folder.getCreatedDate().getTime());
    assertEquals(2_000L, folder.getModifiedDate().getTime());

    folder.setModifiedDate(new Date(3_000L));
    assertEquals(3_000L, folder.getModifiedDate().getTime());
    assertThrows(NullPointerException.class, () -> folder.setModifiedDate(null));
  }

  @Test
  void identityUsesOnlyStableUuid() {
    UUID id = UUID.randomUUID();
    Date now = new Date();
    Folder first = new Folder(id, "first", now, now, null);
    Folder second = new Folder(id, "second", now, now, null);
    Folder other = new Folder("first", null);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, other);
    assertNotEquals(first, "not an entity");
  }

  @Test
  void mutationsUpdateEntityWithoutRecursiveStringOutput() {
    Folder root = new Folder("/", null);
    Folder child = new Folder("before", root);
    Date before = child.getModifiedDate();

    child.setName("after");
    child.setParent(new Folder("other", null));

    assertEquals("after", child.getName());
    assertNotNull(child.getParent());
    assertFalse(child.getModifiedDate().before(before));
    assertTrue(child.toString().contains("parentId="));
  }
}
