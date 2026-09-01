package io.milton.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.milton.s3.db.DynamoDBService;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import io.milton.s3.util.DateUtils;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@ExtendWith(MockitoExtension.class)
class DynamoDBManagerImplTest {

  @Mock private DynamoDBService service;

  private DynamoDBManagerImpl manager;

  @BeforeEach
  void setUp() {
    manager = new DynamoDBManagerImpl(service);
  }

  @Test
  void createsOnlyMissingTablesAndDeletesTables() {
    when(service.isTableExist("existing")).thenReturn(true);
    when(service.isTableExist("new")).thenReturn(false);
    when(service.createTable("new")).thenReturn(true);
    when(service.deleteTable("new")).thenReturn(true);

    assertTrue(manager.createTable("existing"));
    assertTrue(manager.createTable("new"));
    assertTrue(manager.deleteTable("new"));
    verify(service, never()).createTable("existing");
  }

  @Test
  void checksEntityExistenceByParentAndName() {
    Folder parent = new Folder("parent", null);
    when(service.scanItems(eq("table"), anyMap())).thenReturn(List.of(item(true, "child")));

    assertTrue(manager.isExistEntity("table", "child", parent));
    assertFalse(manager.isExistEntity("table", " ", parent));
    assertFalse(manager.isExistEntity("table", null, null));
  }

  @Test
  void storesEntities() {
    Folder folder = new Folder("folder", null);
    Map<String, AttributeValue> item = item(true, "folder");
    when(service.newItem(folder)).thenReturn(item);
    when(service.putItem("table", item)).thenReturn(PutItemResponse.builder().build());

    assertTrue(manager.putEntity("table", folder));
    assertThrows(NullPointerException.class, () -> manager.putEntity("table", null));
  }

  @Test
  void findsRootAndRejectsCorruptRootType() {
    when(service.scanItems(eq("empty"), anyMap())).thenReturn(List.of());
    when(service.scanItems(eq("folder"), anyMap())).thenReturn(List.of(item(true, "/")));
    when(service.scanItems(eq("file"), anyMap())).thenReturn(List.of(item(false, "bad")));

    assertNull(manager.findRootFolder("empty"));
    assertInstanceOf(Folder.class, manager.findRootFolder("folder"));
    assertThrows(MiltonAwsException.class, () -> manager.findRootFolder("file"));
  }

  @Test
  void findsEntitiesByIdentityAndParent() {
    Folder parent = new Folder("parent", null);
    File requested = new File("requested", parent);
    Map<String, AttributeValue> stored = item(false, "stored");
    stored.put(AttributeKey.UUID, string(requested.getId().toString()));
    when(service.getItem(eq("table"), anyMap())).thenReturn(stored);
    when(service.scanItems(eq("table"), anyMap())).thenReturn(List.of(stored));

    Entity byObject = manager.findEntityByUniqueId("table", requested);
    Entity byId = manager.findEntityByUniqueId("table", requested.getId().toString(), parent);
    List<Entity> children = manager.findEntityByParent("table", parent);
    List<Entity> files = manager.findEntityByParentAndType("table", parent, false);

    assertEquals(requested.getId(), byObject.getId());
    assertEquals(requested.getId(), byId.getId());
    assertEquals(1, children.size());
    assertEquals(1, files.size());
    assertNull(manager.findEntityByUniqueId("table", (Entity) null));
    assertNull(manager.findEntityByUniqueId("table", " ", parent));
    assertTrue(manager.findEntityByParent("table", null).isEmpty());
    assertTrue(manager.findEntityByParentAndType("table", null, true).isEmpty());
  }

  @Test
  void updatesEntityStateAfterMetadataUpdate() {
    Folder oldParent = new Folder("old", null);
    Folder newParent = new Folder("new", null);
    File file = new File("before", oldParent);
    when(service.updateItem(eq("table"), anyMap(), anyMap()))
        .thenReturn(UpdateItemResponse.builder().build());

    assertTrue(manager.updateEntityByUniqueId("table", file, newParent, "after", false));

    assertEquals("after", file.getName());
    assertSame(newParent, file.getParent());
    assertThrows(
        IllegalArgumentException.class,
        () -> manager.updateEntityByUniqueId("table", file, newParent, " ", true));
  }

  @Test
  void returnsFalseWhenUpdateHasNoResponse() {
    Folder parent = new Folder("parent", null);
    File file = new File("before", parent);
    when(service.updateItem(eq("table"), anyMap(), anyMap())).thenReturn(null);

    assertFalse(manager.updateEntityByUniqueId("table", file, parent, "after", true));
    assertEquals("before", file.getName());
  }

  @Test
  void deletesOnlyValidIds() {
    when(service.deleteItem(eq("table"), anyMap()))
        .thenReturn(DeleteItemResponse.builder().build());

    assertTrue(manager.deleteEntityByUniqueId("table", UUID.randomUUID().toString()));
    assertFalse(manager.deleteEntityByUniqueId("table", ""));
  }

  private static Map<String, AttributeValue> item(boolean directory, String name) {
    Date date = new Date(1_700_000_000_000L);
    Map<String, AttributeValue> item = new LinkedHashMap<>();
    item.put(AttributeKey.UUID, string(UUID.randomUUID().toString()));
    item.put(AttributeKey.ENTITY_NAME, string(name));
    item.put(AttributeKey.IS_DIRECTORY, number(directory ? 1 : 0));
    item.put(AttributeKey.FILE_SIZE, number(10));
    item.put(AttributeKey.CONTENT_TYPE, string("text/plain"));
    item.put(AttributeKey.CREATED_DATE, string(DateUtils.dateToString(date)));
    item.put(AttributeKey.MODIFIED_DATE, string(DateUtils.dateToString(date)));
    return item;
  }

  private static AttributeValue string(String value) {
    return AttributeValue.builder().s(value).build();
  }

  private static AttributeValue number(long value) {
    return AttributeValue.builder().n(Long.toString(value)).build();
  }
}
