package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.CancelProcessInstanceResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeCancelProcessInstance;
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
 * Command to cancel a process instance.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".cancel-instance",
        notEquals = StringUtils.TRUE)
@Singleton
public class CancelInstanceCommand implements Command<ZeebeCancelProcessInstance> {

    private static final Logger logger = LoggerFactory.getLogger(CancelInstanceCommand.class);
    public static final String PROCESS_INSTANCE_KEY = "processInstanceKey";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public CancelInstanceCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long processInstanceKey = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(PROCESS_INSTANCE_KEY))
                processInstanceKey = (long) parameterValues[i];
        }
        if (processInstanceKey == 0)
            throw new IllegalArgumentException("Process instance key must not be zero!");
        return cancelInstance(processInstanceKey);
    }

    /**
     * Command to cancel a process instance.
     *
     * @param processInstanceKey the key which identifies the corresponding process
     *                           instance.
     * @return Result
     */
    private CompletableFuture<CancelProcessInstanceResponse> cancelInstance(long processInstanceKey) {
        logger.debug("cancelInstance() >> Call cancel instance command with parameters: processInstanceKey: {}", processInstanceKey);
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        return client.get().newCancelInstanceCommand(processInstanceKey).send().toCompletableFuture();
    }
}
