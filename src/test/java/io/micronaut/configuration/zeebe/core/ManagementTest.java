package io.micronaut.configuration.zeebe.core;

import io.micronaut.configuration.zeebe.core.configuration.WorkerConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeConnectionSignal;
import io.micronaut.configuration.zeebe.core.mock.exception.ZeebeTest1Exception;
import io.micronaut.configuration.zeebe.core.mock.exception.ZeebeTest2Exception;
import io.micronaut.configuration.zeebe.core.registry.WorkerRegistry;
import io.micronaut.health.HealthStatus;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author : Vitaly Gromov
 * @since : 1.0.0
 **/
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManagementTest extends ZeebeStarter implements TestPropertyProvider {

    @Inject
    private EmbeddedServer server;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    WorkerRegistry workerRegistry;

    @Inject
    ZeebeClusterConnectionManager connectionManager;

    @Test
    void healthCheckTest() {
        final String url = "/health";
        final MutableHttpRequest<Object> request = HttpRequest.GET(url);
        final HttpResponse<HealthResult> response = client.toBlocking().exchange(request, HealthResult.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(HealthStatus.UP, response.body().getStatus());
        Map<String, Object> details = (Map<String, Object>) response.body().getDetails();
        Map<String, Object> zeebe = (Map<String, Object>) details.get("Zeebe");
        assertEquals("UP", zeebe.get("status"));
    }

    @Test
    void configurationTest() {
        Collection<WorkerConfiguration> workerConfigurations = workerRegistry.getWorkerConfigurations();
        assertNotNull(workerConfigurations);

        WorkerConfiguration workerConfiguration = workerRegistry.getWorkerConfigurations().stream()
                .filter(w -> "sync-throw-worker".equals(w.getType()))
                .findFirst().orElseThrow();
        assertTrue(workerConfiguration.isEnabled());
        assertEquals("PT5M", workerConfiguration.getTimeout().toString());
        assertEquals(32, workerConfiguration.getMaxJobsToActivate());
        assertEquals("PT0.1S", workerConfiguration.getPollInterval().toString());
        assertEquals("PT10S", workerConfiguration.getRequestTimeout().toString());
        assertTrue(workerConfiguration.getFetchVariables().contains("trigger"));
        assertTrue(workerConfiguration.getFetchVariables().contains("keepCalm"));
        assertEquals(2, workerConfiguration.getErrors().size());

        List<Class<? extends Throwable>> throwable1 = workerConfiguration.getErrors().get("11");
        assertTrue(throwable1.contains(ZeebeTest1Exception.class));
        List<Class<? extends Throwable>> throwable2 = workerConfiguration.getErrors().get("22");
        assertTrue(throwable2.contains(ZeebeTest2Exception.class));

        assertEquals("SyncWorker_throwWorker", workerConfiguration.getOutputVariableName());
        assertTrue(workerConfiguration.isAutoComplete());
        assertNotNull(workerConfiguration.getHandler());
    }

    @Test
    void switchWorkerTest() {
        Collection<WorkerConfiguration> workerConfigurations = workerRegistry.getWorkerConfigurations();
        assertNotNull(workerConfigurations);
        final String type = "sync-throw-worker";
        WorkerConfiguration workerConfiguration = workerRegistry.getWorkerConfigurations().stream()
                .filter(w -> type.equals(w.getType()))
                .findFirst().orElseThrow();
        assertTrue(workerConfiguration.isEnabled());

        boolean stoped = workerRegistry.stopWorker(type);
        assertTrue(stoped);

        workerConfiguration = workerRegistry.getWorkerConfigurations().stream()
                .filter(w -> type.equals(w.getType()))
                .findFirst().orElseThrow();
        assertFalse(workerConfiguration.isEnabled());

        boolean resumed = workerRegistry.resumeWorker(type);
        assertTrue(resumed);

        workerConfiguration = workerRegistry.getWorkerConfigurations().stream()
                .filter(w -> type.equals(w.getType()))
                .findFirst().orElseThrow();
        assertTrue(workerConfiguration.isEnabled());
    }

    @Test
    void switchConnectionManagerTest() {
        assertEquals(HealthStatus.UP, connectionManager.getHealth().getStatus());

        connectionManager.switchConnection(new ZeebeConnectionSignal(false));
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> connectionManager.getHealth().getStatus().equals(HealthStatus.DOWN));
        assertEquals(HealthStatus.DOWN, connectionManager.getHealth().getStatus());

        connectionManager.switchConnection(new ZeebeConnectionSignal(true));
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> connectionManager.getHealth().getStatus().equals(HealthStatus.UP));
        assertEquals(HealthStatus.UP, connectionManager.getHealth().getStatus());
    }

    @NotNull
    @Override
    public Map<String, String> getProperties() {
        return Map.of(
                "zeebe.gateway-address", zeebeContainer.getExternalGatewayAddress(),
                "zeebe.enabled", "true");
    }
}
