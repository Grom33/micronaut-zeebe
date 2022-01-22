package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.FailJobResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeThrowFail;
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
 * Command to mark a job as failed. If the given retries are greater than zero
 * then this job will be picked up again by a job subscription. Otherwise, an
 * incident is created for this job.
 *
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".fail",
        notEquals = StringUtils.TRUE)
@Singleton
public class FailCommand implements Command<ZeebeThrowFail> {

    public static final String REMAINING_RETRIES = "remainingRetries";
    public static final String JOB_KEY = "jobKey";
    public static final String ERROR_MSG = "errorMsg";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public FailCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long jobKey = 0;
        int remainingRetries = 0;
        String errorMsg = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(JOB_KEY))
                jobKey = (long) parameterValues[i];
            if (arguments[i].getName().equals(REMAINING_RETRIES))
                remainingRetries = (int) parameterValues[i];
            if (arguments[i].getName().equals(ERROR_MSG))
                errorMsg = (String) parameterValues[i];
        }
        if (jobKey == 0)
            throw new IllegalArgumentException("Job key must not be zero!");
        return failCommand(jobKey, remainingRetries, errorMsg);
    }

    /**
     * Command to mark a job as failed. If the given retries are greater than zero
     * then this job will be picked up again by a job subscription. Otherwise, an
     * incident is created for this job.
     *
     * @param jobKey           - The key which identifies the job
     * @param remainingRetries - The remaining retries of this job (e.g.
     *                         "jobEvent.getRetries() - 1"). If the retries are
     *                         greater than zero then this job will be picked up
     *                         again by a job subscription. Otherwise, an incident
     *                         is created for this job.
     * @param errorMsg         - Error message to be attached to the failed job.
     *                         Provide an error message describing the reason for
     *                         the job failure. If failing the job creates an
     *                         incident, this error message will be used as incident
     *                         message.
     */
    private CompletableFuture<FailJobResponse> failCommand(long jobKey, int remainingRetries, String errorMsg) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        final var step1 = client.get().newFailCommand(jobKey).retries(remainingRetries);
        if (StringUtils.isNotEmpty(errorMsg))
            return step1.errorMessage(errorMsg).send().toCompletableFuture();
        return step1.send().toCompletableFuture();
    }
}
