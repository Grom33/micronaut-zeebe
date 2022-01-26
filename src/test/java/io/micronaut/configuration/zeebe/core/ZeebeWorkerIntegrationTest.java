package io.micronaut.configuration.zeebe.core;

import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.mock.SyncWorker;
import io.micronaut.configuration.zeebe.core.mock.TestData;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.micronaut.configuration.zeebe.core.mock.SyncWorker.RESULT_DTO;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZeebeWorkerIntegrationTest extends ZeebeStarter implements TestPropertyProvider {

    @Inject
    ZeebeClusterConnectionManager connectionManager;

    @Inject
    List<TestData> workerContainers;

    @Inject
    SyncWorker syncWorker;

    @BeforeEach
    void beforeEach() {
        workerContainers.forEach(TestData::drainTestData);
    }

    @Test
    void syncWorkerMultiplyX2Test() {
        deploySchema("bpmn/sync-test-worker-multiply-x-2.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerMultiply", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void syncWorkerMultiplyX2OptionalTest() {
        deploySchema("bpmn/sync-test-worker-multiply-x-2Optional.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerMultiplyOptional", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void syncWorkerVoidTest() {
        deploySchema("bpmn/sync-test-worker-multiply-void.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerMultiplyVoid", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        int testData = (int) syncWorker.getTestData(SyncWorker.SYNC_TEST_WORKER_VOID);
        assertEquals(5, testData);
    }

    @Test
    void syncWorkerInputDataTest() {
        final DeploymentEvent deploymentEvent = deploySchema("bpmn/sync-test-worker-test-input-data.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerTestInputData", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        Map<String, Object> testData = (Map<String, Object>) syncWorker.getTestData(SyncWorker.SYNC_TEST_WORKER_TEST_INPUT_DATA);

        assertTrue(StringUtils.isNotEmpty((String) testData.get("elementId")));
        assertEquals(processInstanceResult.getProcessInstanceKey(), testData.get("processInstanceKey"));
        assertEquals(3, testData.get("retries"));
        assertEquals(4, ((Map) testData.get("variables")).size());
        assertNotNull(testData.get("elementInstanceKey"));
        assertEquals(deploymentEvent.getProcesses().get(0).getBpmnProcessId(), testData.get("bpmnProcessId"));
        assertNotNull(testData.get("deadline"));
        assertNotNull(testData.get("key"));
        assertEquals(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey(), testData.get("processDefinitionKey"));
        assertEquals(deploymentEvent.getProcesses().get(0).getVersion(), testData.get("processDefinitionVersion"));

        Dto dto = (Dto) testData.get("dto");
        assertEquals("test-value1", dto.getValue1());
        assertEquals("test-value2", dto.getValue2());

        Dto dto2 = (Dto) testData.get("dto2");
        assertEquals("root-value1", dto2.getValue1());
        assertEquals("root-value2", dto2.getValue2());
    }

    @Test
    void syncWorkerReturnDtoTest() {
        deploySchema("bpmn/sync-test-worker-return-dto.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerReturnDto", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        Map<String, Object> map = (Map<String, Object>) variablesAsMap.get(RESULT_DTO);
        assertEquals("root-value1", map.get("value1"));
        assertEquals("root-value2", map.get("value2"));
    }

    @Test
    void asyncWorkerMultiplyX2Test() {
        deploySchema("bpmn/async-test-worker-multiply-x-2.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("asyncTestWorkerMultiply", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void asyncWorkerMultiplyX2OptionalTest() {
        deploySchema("bpmn/async-test-worker-multiply-x-2Optional.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("asyncTestWorkerMultiplyOptional", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void asyncWorkerVoidTest() {
        deploySchema("bpmn/async-test-worker-multiply-void.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("asyncTestWorkerMultiplyVoid", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        assertEquals(5, (int) variablesAsMap.get("outResult"));
    }

    @Test
    void reactiveWorkerMultiplyX2Test() {
        deploySchema("bpmn/reactive-test-worker-multiply-x-2.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("reactiveTestWorkerMultiply", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void reactiveWorkerMultiplyX2OptionalTest() {
        deploySchema("bpmn/reactive-test-worker-multiply-x-2Optional.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/syncMultplyX2Payload.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("reactiveTestWorkerMultiplyOptional", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertTrue(Objects.nonNull(variablesAsMap.get("result")));
        assertEquals(10, variablesAsMap.get("result"));
    }

    @Test
    void reactiveWorkerReturnDtoTest() {
        deploySchema("bpmn/reactive-test-worker-return-dto.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("reactiveTestWorkerReturnDto", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        Map<String, Object> map = (Map<String, Object>) variablesAsMap.get(RESULT_DTO);
        assertEquals("root-value1", map.get("value1"));
        assertEquals("root-value2", map.get("value2"));
    }

    @Test
    void reactiveWorkerReturnFlowableTest() {
        deploySchema("bpmn/reactive-test-worker-return-flowable.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("reactiveTestWorkerReturnFlowable", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        List<String> lst = (List<String>) variablesAsMap.get("resultList");
        assertEquals(2, lst.size());
    }

    @Test
    void reactiveWorkerThrowTest() {
        deploySchema("bpmn/sync-test-worker-throw.bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        final ProcessInstanceResult processInstanceResult = startProcessInSyncMode("syncTestWorkerThrow", payload);
        Map<String, Object> variablesAsMap = processInstanceResult.getVariablesAsMap();
        assertNotNull(variablesAsMap);
        String result = (String) variablesAsMap.get("throwResult");
        assertEquals("throwResult22", result);
    }

    @NotNull
    @Override
    public Map<String, String> getProperties() {
        return Map.of(
                "zeebe.gateway-address", zeebeContainer.getExternalGatewayAddress(),
                "zeebe.enabled", "true");
    }
}
