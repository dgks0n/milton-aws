package io.milton.s3.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.milton.s3.MiltonAwsException;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@ExtendWith(MockitoExtension.class)
class DynamoDBServiceImplTest {

  @Mock private DynamoDbClient client;
  @Mock private DynamoDbWaiter waiter;
  @Mock private WaiterResponse<DescribeTableResponse> waiterResponse;

  private DynamoDBServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new DynamoDBServiceImpl(client);
  }

  @Test
  void createsOnDemandTableAndWaitsForAvailability() {
    when(client.createTable(any(CreateTableRequest.class)))
        .thenReturn(CreateTableResponse.builder().build());
    when(client.waiter()).thenReturn(waiter);
    when(waiter.waitUntilTableExists(any(DescribeTableRequest.class))).thenReturn(waiterResponse);
    when(waiterResponse.matched())
        .thenReturn(ResponseOrException.response(DescribeTableResponse.builder().build()));

    assertTrue(service.createTable("metadata"));

    verify(waiter).waitUntilTableExists(any(DescribeTableRequest.class));
    verify(waiter).close();
  }

  @Test
  void existingTableCountsAsSuccessfulCreate() {
    when(client.createTable(any(CreateTableRequest.class)))
        .thenThrow(ResourceInUseException.builder().message("exists").build());

    assertTrue(service.createTable("metadata"));
  }

  @Test
  void createFailureIsWrapped() {
    when(client.createTable(any(CreateTableRequest.class)))
        .thenThrow(DynamoDbException.builder().message("denied").build());

    assertThrows(MiltonAwsException.class, () -> service.createTable("metadata"));
  }

  @Test
  void deletesTableAndWaitsForRemoval() {
    when(client.deleteTable(any(DeleteTableRequest.class)))
        .thenReturn(DeleteTableResponse.builder().build());
    when(client.waiter()).thenReturn(waiter);
    when(waiter.waitUntilTableNotExists(any(DescribeTableRequest.class)))
        .thenReturn(waiterResponse);
    when(waiterResponse.matched())
        .thenReturn(ResponseOrException.response(DescribeTableResponse.builder().build()));

    assertTrue(service.deleteTable("metadata"));

    verify(waiter).waitUntilTableNotExists(any(DescribeTableRequest.class));
    verify(waiter).close();
  }

  @Test
  void missingTableCountsAsSuccessfulDelete() {
    when(client.deleteTable(any(DeleteTableRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("missing").build());

    assertTrue(service.deleteTable("metadata"));
  }

  @Test
  void waiterFailureIsVisible() {
    when(client.createTable(any(CreateTableRequest.class)))
        .thenReturn(CreateTableResponse.builder().build());
    when(client.waiter()).thenReturn(waiter);
    when(waiter.waitUntilTableExists(any(DescribeTableRequest.class))).thenReturn(waiterResponse);
    when(waiterResponse.matched())
        .thenReturn(ResponseOrException.exception(new IllegalStateException("timed out")));

    assertThrows(MiltonAwsException.class, () -> service.createTable("metadata"));
  }

  @Test
  void checksTableExistenceAndWrapsUnexpectedFailures() {
    when(client.describeTable(any(DescribeTableRequest.class)))
        .thenReturn(DescribeTableResponse.builder().build())
        .thenThrow(ResourceNotFoundException.builder().message("missing").build())
        .thenThrow(DynamoDbException.builder().message("denied").build());

    assertTrue(service.isTableExist("metadata"));
    assertFalse(service.isTableExist("metadata"));
    assertThrows(MiltonAwsException.class, () -> service.isTableExist("metadata"));
  }

  @Test
  void mapsDomainEntityToDynamoAttributesWithoutNarrowingFileSize() {
    Folder parent = new Folder("parent", null);
    File file = new File("large.bin", parent);
    file.setSize((long) Integer.MAX_VALUE + 100L);

    Map<String, AttributeValue> item = service.newItem(file);

    assertEquals(file.getId().toString(), item.get(AttributeKey.UUID).s());
    assertEquals(parent.getId().toString(), item.get(AttributeKey.PARENT_UUID).s());
    assertEquals("0", item.get(AttributeKey.IS_DIRECTORY).n());
    assertEquals(Long.toString(file.getSize()), item.get(AttributeKey.FILE_SIZE).n());
    assertEquals(AttributeKey.NOT_EXIST, item.get(AttributeKey.CONTENT_TYPE).s());
    assertThrows(NullPointerException.class, () -> service.newItem(null));
  }

  @Test
  void mapsFolderWithRootSentinel() {
    Map<String, AttributeValue> item = service.newItem(new Folder("/", null));

    assertEquals(AttributeKey.NOT_EXIST, item.get(AttributeKey.PARENT_UUID).s());
    assertEquals("1", item.get(AttributeKey.IS_DIRECTORY).n());
  }

  @Test
  void putsAndGetsItemsConsistently() {
    Map<String, AttributeValue> item = Map.of("id", string("1"));
    when(client.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
    when(client.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(item).build())
        .thenReturn(GetItemResponse.builder().build());

    assertEquals(PutItemResponse.builder().build(), service.putItem("table", item));
    assertEquals(item, service.getItem("table", item));
    assertTrue(service.getItem("table", item).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> service.putItem("table", Map.of()));
    assertThrows(IllegalArgumentException.class, () -> service.getItem("table", Map.of()));
  }

  @Test
  void scansEveryPage() {
    Map<String, AttributeValue> first = Map.of("id", string("1"));
    Map<String, AttributeValue> second = Map.of("id", string("2"));
    when(client.scan(any(ScanRequest.class)))
        .thenReturn(
            ScanResponse.builder().items(List.of(first)).lastEvaluatedKey(first).build(),
            ScanResponse.builder().items(List.of(second)).build());

    List<Map<String, AttributeValue>> results =
        service.scanItems("table", Map.of("name", Condition.builder().build()));

    assertEquals(List.of(first, second), results);
    assertThrows(IllegalArgumentException.class, () -> service.scanItems("table", Map.of()));
  }

  @Test
  void updatesAndDeletesItems() {
    Map<String, AttributeValue> key = Map.of("id", string("1"));
    Map<String, AttributeValueUpdate> updates =
        Map.of("name", AttributeValueUpdate.builder().value(string("new")).build());
    when(client.updateItem(any(UpdateItemRequest.class)))
        .thenReturn(UpdateItemResponse.builder().build());
    when(client.deleteItem(any(DeleteItemRequest.class)))
        .thenReturn(DeleteItemResponse.builder().build());

    assertEquals(UpdateItemResponse.builder().build(), service.updateItem("table", key, updates));
    assertEquals(DeleteItemResponse.builder().build(), service.deleteItem("table", key));
    assertThrows(
        IllegalArgumentException.class, () -> service.updateItem("table", Map.of(), updates));
    assertThrows(IllegalArgumentException.class, () -> service.updateItem("table", key, Map.of()));
    assertThrows(IllegalArgumentException.class, () -> service.deleteItem("table", Map.of()));
  }

  @Test
  void validatesNamesAndWrapsDataOperationFailures() {
    assertThrows(IllegalArgumentException.class, () -> service.createTable(" "));
    when(client.getItem(any(GetItemRequest.class)))
        .thenThrow(DynamoDbException.builder().message("failure").build());

    assertThrows(
        MiltonAwsException.class, () -> service.getItem("table", Map.of("id", string("1"))));
  }

  private static AttributeValue string(String value) {
    return AttributeValue.builder().s(value).build();
  }
}
