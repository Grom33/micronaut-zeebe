package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.CompleteJobResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.complete.ZeebeCompleteCommand;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to complete a job. If the job is linked to a process instance then
 * this command will complete the related activity and continue the flow.
 *
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".fail",
        notEquals = StringUtils.TRUE)
@Singleton
public class CompleteCommand implements Command<ZeebeCompleteCommand> {

    public static final String JOB_KEY = "jobKey";
    public static final String VARIABLES = "variables";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public CompleteCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long jobKey = 0;
        Map<String, Object> variables = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(JOB_KEY))
                jobKey = (long) parameterValues[i];
            if (arguments[i].getName().equals(VARIABLES))
                variables = (Map<String, Object>) parameterValues[i];
        }
        if (jobKey == 0)
            throw new IllegalArgumentException("Job key must not be zero!");
        return completeJob(jobKey, variables);
    }

    /**
     * Command to complete a job. If the job is linked to a process instance then
     * this command will complete the related activity and continue the flow.
     *
     * @param jobKey    - the key which identifies the job
     * @param variables - the variables as map
     */
    private CompletableFuture<CompleteJobResponse> completeJob(long jobKey, Map<String, Object> variables) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        final var step1 = client.get().newCompleteCommand(jobKey);
        if (Objects.isNull(variables) || variables.isEmpty())
            return step1.send().toCompletableFuture();
        return step1.variables(variables).send().toCompletableFuture();
    }
}
