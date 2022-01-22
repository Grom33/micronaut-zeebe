package io.micronaut.configuration.zeebe.core.indicator;

import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.context.annotation.Requires;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Zeebe cluster health check
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(property = "zeebe.health.enabled", value = "true", defaultValue = "false")
public class ZeebeIndicator implements HealthIndicator {

    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public ZeebeIndicator(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Publisher<HealthResult> getResult() {
        return Mono.fromCallable(connectionManager::getHealth);
    }

}
