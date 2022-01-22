package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ResolveIncidentResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeResolveIncident;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to resolve an existing incident.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".resolve-incident",
        notEquals = StringUtils.TRUE)
@Singleton
public class ResolveIncidentCommand implements Command<ZeebeResolveIncident> {

    private static final Logger logger = LoggerFactory.getLogger(ResolveIncidentCommand.class);
    public static final String INCIDENT_KEY = "incidentKey";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public ResolveIncidentCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long incidentKey = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(INCIDENT_KEY))
                incidentKey = (long) parameterValues[i];
        }
        if (incidentKey == 0)
            throw new IllegalArgumentException("Incident key must not be zero!");
        return resolveIncident(incidentKey);
    }

    /**
     * Command to resolve an existing incident.
     *
     * @param incidentKey the key of the corresponding incident
     */
    private CompletableFuture<ResolveIncidentResponse> resolveIncident(long incidentKey) {
        logger.debug("resolveIncident() >> Call resolve incident command with parameters: incidentKey: {}", incidentKey);
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't resolve incident, zeebe broker isn't available!");
        return client.get().newResolveIncidentCommand(incidentKey).send().toCompletableFuture();
    }
}
