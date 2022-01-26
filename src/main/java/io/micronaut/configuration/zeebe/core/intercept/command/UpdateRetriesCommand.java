package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.UpdateRetriesJobResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeUpdateRetries;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to update the retries of a job. If the given retries are greater than
 * zero then this job will be picked up again by a job worker. This will not
 * close a related incident, which still has to be marked as resolved with
 * newResolveIncidentCommand(long incidentKey).
 *
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".update-retries",
        notEquals = StringUtils.TRUE)
@Singleton
public class UpdateRetriesCommand implements Command<ZeebeUpdateRetries> {

    public static final String JOB_KEY = "jobKey";
    public static final String RETRIES = "retries";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public UpdateRetriesCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long jobKey = 0;
        int retries = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(JOB_KEY))
                jobKey = (long) parameterValues[i];
            if (arguments[i].getName().equals(RETRIES))
                retries = (int) parameterValues[i];
        }
        if (jobKey == 0)
            throw new IllegalArgumentException("Job key must not be zero!");
        return updateRetries(jobKey, retries);
    }

    /**
     * Command to update the retries of a job. If the given retries are greater than
     * zero then this job will be picked up again by a job worker. This will not
     * close a related incident, which still has to be marked as resolved with
     * newResolveIncidentCommand(long incidentKey).
     *
     * @param jobKey  - the key of the job to update
     * @param retries - the retries of this job
     */
    private CompletableFuture<UpdateRetriesJobResponse> updateRetries(long jobKey, int retries) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        return client.get().newUpdateRetriesCommand(jobKey)
                .retries(retries).send().toCompletableFuture();
    }
}
