package io.micronaut.configuration.zeebe.core.connection;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1;
import io.camunda.zeebe.client.api.response.BrokerInfo;
import io.camunda.zeebe.client.api.response.PartitionBrokerHealth;
import io.camunda.zeebe.client.api.response.PartitionInfo;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeClusterConnectionEstablishedEvent;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeClusterConnectionLostEvent;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeConnectionSignal;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.Toggleable;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Manages the connection state of the cluster. Can connect or disconnect to a
 * Zeebe broker on an event dispatched via a ApplicationEventPublisher.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Context
public class ZeebeClusterConnectionManager implements AutoCloseable, Toggleable {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeClusterConnectionManager.class);
    public static final String ZEEBE = "Zeebe";
    public static final String BROKERS = "brokers";
    public static final String INFO = "info";

    private final ApplicationEventPublisher<Object> eventPublisher;
    private final ZeebeConfiguration configuration;
    private ZeebeClient zeebeClient;
    protected HealthResult health;
    private volatile boolean connectionEnabled;
    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    public ZeebeClusterConnectionManager(ApplicationEventPublisher<Object> eventPublisher,
                                         ZeebeConfiguration configuration) {
        this.eventPublisher = eventPublisher;
        this.configuration = configuration;
        this.connectionEnabled = !configuration.isLazyConnection().orElse(Boolean.FALSE);
    }

    @Scheduled(fixedDelay = "5s")
    public void checkConnectionSync() {
        logger.trace("checkConnection() >> Run zeebe broker health verification....");
        lock.lock();
        try {
            checkConnection();
        } catch (Exception e) {
            logger.error("checkConnectionSync() >> ", e);
        } finally {
            lock.unlock();
        }
    }

    private void checkConnection() {
        if (!connectionEnabled)
            return;
        ZeebeClient useClient = this.zeebeClient;
        if (useClient == null) {
            Optional<ZeebeClient> connect = getConnectionClient();
            if (connect.isEmpty()) {
                logger.warn("checkConnection() >> Can't connect to Zeebe broker!");
                this.health = HealthResult.builder(ZEEBE, HealthStatus.DOWN).build();
                return;
            }
            useClient = connect.get();
        }
        boolean previousStatus = health != null && isUp();
        this.health = getHealthResult(useClient);

        if (isUp())
            this.zeebeClient = useClient;
        logger.debug("checkConnection() >> Connection to Zeebe broker is healthy: {}", isUp());
        if (previousStatus && !isUp()) {
            lostConnection();
            Optional.ofNullable(this.zeebeClient)
                    .ifPresent(zc -> {
                        zc.close();
                        this.zeebeClient = null;
                    });
        } else if (!previousStatus && isUp()) {
            establishedConnection();
        }
    }

    private boolean isUp() {
        return Objects.equals(health.getStatus(), HealthStatus.UP);
    }

    private Optional<ZeebeClient> getConnectionClient() {
        logger.debug("getConnectionClient() >> Build zeebe cluster client....");
        try {
            ZeebeClientBuilder zeebeClientBuilder = isCloudConfigurationPresent()
                    ? createCloudClient()
                    : ZeebeClient.newClientBuilder().usePlaintext();

            configuration.getDefaultRequestTimeout()
                    .ifPresent(timeout -> zeebeClientBuilder.defaultRequestTimeout(Duration.parse(timeout)));
            configuration.getDefaultJobPollInterval()
                    .ifPresent(duration -> zeebeClientBuilder.defaultJobPollInterval(Duration.ofMillis(duration)));
            configuration.getDefaultJobTimeout().ifPresent(timeout -> zeebeClientBuilder.defaultJobTimeout(Duration.parse(timeout)));
            configuration.getDefaultMessageTimeToLive().ifPresent(ttl -> zeebeClientBuilder.defaultMessageTimeToLive(Duration.parse(ttl)));
            configuration.getDefaultJobWorkerName().ifPresent(zeebeClientBuilder::defaultJobWorkerName);
            configuration.getGatewayAddress().ifPresent(zeebeClientBuilder::gatewayAddress);
            configuration.getNumJobWorkerExecutionThreads().ifPresent(zeebeClientBuilder::numJobWorkerExecutionThreads);
            configuration.getKeepAlive().ifPresent(keepAlive -> zeebeClientBuilder.keepAlive(Duration.parse(keepAlive)));
            configuration.getCaCertificatePath().ifPresent(zeebeClientBuilder::caCertificatePath);
            ZeebeClient zeeClient = zeebeClientBuilder.build();
            return Optional.of(zeeClient);
        } catch (Exception e) {
            logger.error("getConnectionClient() >> Failed build client, because: {}", e.getMessage());
        }
        return Optional.empty();
    }

    protected ZeebeClientBuilder createCloudClient() {
        final String clusterId = configuration.getClusterId().orElseThrow();
        final String clientId = configuration.getClientId().orElseThrow();
        final String clientSecret = configuration.getClientSecret().orElseThrow();
        ZeebeClientCloudBuilderStep1.ZeebeClientCloudBuilderStep2.ZeebeClientCloudBuilderStep3.ZeebeClientCloudBuilderStep4 builder = ZeebeClient
                .newCloudClientBuilder()
                .withClusterId(clusterId)
                .withClientId(clientId)
                .withClientSecret(clientSecret);
        configuration.getRegion().ifPresent(builder::withRegion);
        return builder;
    }

    protected boolean isCloudConfigurationPresent() {
        return configuration.getClusterId().isPresent()
                && configuration.getClientId().isPresent()
                && configuration.getClientSecret().isPresent();
    }

    public HealthResult getHealth() {
        return Optional.ofNullable(health)
                .orElseGet(() -> HealthResult.builder(ZEEBE, HealthStatus.DOWN).build());
    }

    public Optional<ZeebeClient> getClient() {
        logger.debug("getClient() >> Try to get client, client is present: {}", zeebeClient != null);
        return Optional.ofNullable(zeebeClient);
    }

    private HealthResult getHealthResult(final ZeebeClient client) {
        if (client == null)
            return HealthResult.builder(ZEEBE, HealthStatus.DOWN)
                    .details("Broker connection lost")
                    .build();
        List<BrokerInfo> topology;
        try {
            topology = client.newTopologyRequest().send().join().getBrokers();
        } catch (Exception e) {
            logger.error("getHealthResult() >> Can't get Zeebe broker topology: {}", e.getMessage());
            return HealthResult.builder(ZEEBE, HealthStatus.DOWN)
                    .details(e.getMessage())
                    .build();
        }

        if (topology == null || topology.isEmpty()) {
            return HealthResult.builder(ZEEBE, HealthStatus.DOWN)
                    .details("Broker doesn't have nodes!")
                    .build();
        }

        final List<BrokerInfo> withUnhealthyPartitions = topology.stream()
                .filter(brokerInfo -> brokerInfo.getPartitions().stream()
                        .anyMatch(pi -> pi.getHealth().equals(PartitionBrokerHealth.UNHEALTHY)))
                .collect(Collectors.toList());
        if (!withUnhealthyPartitions.isEmpty()) {
            final String failedBrokers = withUnhealthyPartitions.stream().map(bi -> "node-" + bi.getNodeId() + " on " + bi.getAddress())
                    .collect(Collectors.joining("; "));
            logger.warn("getHealthResult() >> brokers: {}  have unhealthy partitions!", failedBrokers);
        }

        final List<Integer> failedPartitions = topology.stream()
                .flatMap(brokerInfo -> brokerInfo.getPartitions().stream())
                .collect(Collectors.groupingBy(PartitionInfo::getPartitionId))
                .entrySet().stream()
                .filter(entry -> entry.getValue().stream().noneMatch(PartitionInfo::isLeader))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!failedPartitions.isEmpty()) {
            final String partitions = failedPartitions.stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.error(
                    "getHealthResult() >> Zeebe cluster isn't in a consistent state, because partitions with number: {} doesn't have a leader!",
                    partitions);
            return HealthResult.builder(ZEEBE, HealthStatus.DOWN)
                    .details(Map.of(
                            BROKERS, topology,
                            INFO, String.format("Partitions with number %s doesn't have a leader", partitions)))
                    .build();
        }
        return HealthResult.builder(ZEEBE, HealthStatus.UP)
                .details(Map.of(BROKERS, topology))
                .build();
    }

    /**
     * Manage connection state based on incoming signal
     *
     * @param signal Object of incoming signal
     */
    @Async
    @EventListener
    public void switchConnection(final ZeebeConnectionSignal signal) {
        logger.info("switchConnection() >> Switch Zeebe broker connection to {}", signal.isEnabled());
        this.connectionEnabled = signal.isEnabled();
        if (signal.isEnabled())
            checkConnectionSync();
        else
            close();
    }

    /**
     * Emmit a connection loss event
     */
    private void lostConnection() {
        logger.info("establishedConnection() >> Emit event: Connection to Zeebe broker lost!");
        eventPublisher.publishEvent(new ZeebeClusterConnectionLostEvent());
    }

    /**
     * Emmit an event about a successful connection
     */
    private void establishedConnection() {
        logger.info("establishedConnection() >> Emit event: Connection to Zeebe broker established!");
        eventPublisher.publishEvent(new ZeebeClusterConnectionEstablishedEvent());
    }

    @PreDestroy
    @Override
    public void close() {
        Optional.ofNullable(this.zeebeClient)
                .ifPresent(zeeClient -> {
                    this.zeebeClient = null;
                    zeeClient.close();
                });
    }

    @Override
    public boolean isEnabled() {
        return connectionEnabled;
    }
}
