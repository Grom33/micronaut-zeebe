package io.micronaut.configuration.zeebe.core;

import io.camunda.zeebe.client.api.response.*;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.mock.SleepWorker;
import io.micronaut.configuration.zeebe.core.mock.ZeebeTestClient;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import static org.awaitility.Awaitility.await;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZeebeClientIntegrationTest extends ZeebeStarter implements TestPropertyProvider {

    public static final String ROOT_VALUE_1 = "root-value1";
    public static final String ROOT_VALUE_2 = "root-value2";
    @Inject
    ZeebeTestClient testClient;
    @Inject
    ZeebeClusterConnectionManager connectionManager;
    @Inject
    SleepWorker sleepWorker;

    @AfterEach
    void tearDown() {
        sleepWorker.erase();
    }

    @Test
    void pushMessageTest() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(1);
        assertNotNull(testClient);
        deploySchema("bpmn/deploy-command-test.cloud-bpmn");
        CompletableFuture.runAsync(() -> {
            try {
                semaphore.acquire();
                final Dto dto = testClient.syncStartByProcessNameWithMapping("testPushMessage", Map.of("test", "test"));
                assertNotNull(dto);
                assertEquals(ROOT_VALUE_1, dto.getValue1());
                assertEquals(ROOT_VALUE_2, dto.getValue2());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        });
        testClient.send("test", Map.of(
                "value1", ROOT_VALUE_1,
                "value2", ROOT_VALUE_2));
        semaphore.acquire();
    }

    @Test
    void startSyncProcessTest() {
        await().atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> connectionManager.getClient().isPresent());
        deploySchema("bpmn/test-worker.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        Dto dto = testClient.syncStartWithMapping(payload);
        assertNotNull(dto);
        assertEquals(ROOT_VALUE_1, dto.getValue1());
        assertEquals(ROOT_VALUE_2, dto.getValue2());
    }

    @Test
    void startAsyncProcessTest() {
        await().atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> connectionManager.getClient().isPresent());
        deploySchema("bpmn/test-worker.cloud-bpmn");
        final Map<String, Object> payload = getMapFromPath("json/sync-test-worker-test-input-data.json");
        ProcessInstanceEvent processInstanceEvent = testClient.asyncStart(payload);
        assertNotNull(processInstanceEvent);
        assertTrue(processInstanceEvent.getProcessInstanceKey() > 0);
    }

    @Test
    void deployCommandTest() {
        final InputStream inputStream = ZeebeClientIntegrationTest.class
                .getClassLoader().getResourceAsStream("bpmn/test-worker.cloud-bpmn");
        DeploymentEvent deploy = testClient.deploy(inputStream, "test.bpmn");
        assertEquals(1, deploy.getProcesses().size());
        assertEquals("testWorker", deploy.getProcesses().get(0).getBpmnProcessId());
    }

    @Test
    void cancelInstanceCommandTest() {
        deploySchema("bpmn/processThatShouldBeCanceled.cloud-bpmn");
        ProcessInstanceEvent processInstanceEvent = testClient.asyncStartByName("processThatShouldBeCanceled", Map.of("test", "test"));
        CancelProcessInstanceResponse cancelProcessInstanceResponse = testClient
                .cancelInstance(processInstanceEvent.getProcessInstanceKey());
        assertNotNull(cancelProcessInstanceResponse);
    }

    @Test
    void completeCommandTest() {
        deploySchema("bpmn/sleepWorkerTest.bpmn");
        ProcessInstanceEvent processInstanceEvent = testClient.asyncStartByName("sleepWorkerTest", Map.of("test", "test"));
        assertNotNull(processInstanceEvent);
        await().atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> sleepWorker.getJobKey() > 0);
        CompleteJobResponse completeJobResponse = testClient.completeJob(sleepWorker.getJobKey());
        assertNotNull(completeJobResponse);

    }

    @Test
    void errorCommandTest() {
        deploySchema("bpmn/sleepWorkerTest.bpmn");
        ProcessInstanceEvent processInstanceEvent = testClient.asyncStartByName("sleepWorkerTest", Map.of("test", "test"));
        assertNotNull(processInstanceEvent);
        await().atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> sleepWorker.getJobKey() > 0);
        testClient.sendError(sleepWorker.getJobKey(), "ERROR", "500");
    }

    @Test
    void failCommandTest() {
        deploySchema("bpmn/sleepWorkerTest.bpmn");
        ProcessInstanceEvent processInstanceEvent = testClient.asyncStartByName("sleepWorkerTest", Map.of("test", "test"));
        assertNotNull(processInstanceEvent);
        await().atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> sleepWorker.getJobKey() > 0);
        FailJobResponse fail = testClient.sendFail(sleepWorker.getJobKey(), 0, "ERROR");
        assertNotNull(fail);

    }

    @NotNull
    @Override
    public Map<String, String> getProperties() {
        return Map.of(
                "zeebe.gateway-address", zeebeContainer.getExternalGatewayAddress(),
                "zeebe.enabled", "true");
    }
}
