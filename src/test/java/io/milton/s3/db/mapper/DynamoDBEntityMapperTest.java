package io.milton.s3.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class DynamoDBEntityMapperTest {

  @Test
  void mapsFolderAndFileItems() {
    Folder parent = new Folder("parent", null);
    Map<String, AttributeValue> folderItem = item(true, "docs");
    Map<String, AttributeValue> fileItem = item(false, "report.pdf");

    List<Entity> entities =
        DynamoDBEntityMapper.convertItemsToEntities(parent, List.of(folderItem, fileItem));

    assertEquals(2, entities.size());
    assertInstanceOf(Folder.class, entities.get(0));
    File file = assertInstanceOf(File.class, entities.get(1));
    assertEquals(parent, file.getParent());
    assertEquals(123L, file.getSize());
    assertEquals("application/pdf", file.getContentType());
  }

  @Test
  void mapsMissingContentTypeSentinelToNull() {
    Map<String, AttributeValue> item = item(false, "file");
    item.put(AttributeKey.CONTENT_TYPE, string(AttributeKey.NOT_EXIST));

    File file = assertInstanceOf(File.class, DynamoDBEntityMapper.convertItemToEntity(null, item));

    assertNull(file.getContentType());
  }

  @Test
  void emptyInputsProduceEmptyResults() {
    assertTrue(DynamoDBEntityMapper.convertItemsToEntities(null, null).isEmpty());
    assertTrue(DynamoDBEntityMapper.convertItemsToEntities(null, List.of()).isEmpty());
    assertNull(DynamoDBEntityMapper.convertItemToEntity(null, null));
    assertNull(DynamoDBEntityMapper.convertItemToEntity(null, Map.of()));
  }

  @Test
  void malformedItemsFailFast() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DynamoDBEntityMapper.convertItemToEntity(
                null, Map.of(AttributeKey.UUID, string(UUID.randomUUID().toString()))));

    Map<String, AttributeValue> wrongType = item(true, "folder");
    wrongType.put(AttributeKey.ENTITY_NAME, AttributeValue.builder().n("1").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> DynamoDBEntityMapper.convertItemToEntity(null, wrongType));
  }

  private static Map<String, AttributeValue> item(boolean directory, String name) {
    Date date = new Date(1_700_000_000_000L);
    Map<String, AttributeValue> item = new LinkedHashMap<>();
    item.put(AttributeKey.UUID, string(UUID.randomUUID().toString()));
    item.put(AttributeKey.ENTITY_NAME, string(name));
    item.put(AttributeKey.IS_DIRECTORY, number(directory ? 1 : 0));
    item.put(AttributeKey.FILE_SIZE, number(123));
    item.put(AttributeKey.CONTENT_TYPE, string("application/pdf"));
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
