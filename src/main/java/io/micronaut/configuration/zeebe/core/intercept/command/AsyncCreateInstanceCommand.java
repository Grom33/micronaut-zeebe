package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeStartAsyncProcess;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to create/start a new instance of a process
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */

@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".async-create-instance",
        notEquals = StringUtils.TRUE)
@Singleton
public class AsyncCreateInstanceCommand implements Command<ZeebeStartAsyncProcess> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCreateInstanceCommand.class);
    public static final String VARIABLES = "variables";
    public static final String BPMN_PROCESS_ID = "bpmnProcessId";
    public static final String VERSION = "version";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public AsyncCreateInstanceCommand(ZeebeClusterConnectionManager connectionManager,
                                      BeanContext beanContext) {

        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        String bpmnProcessId = (String) context.getValue(ZeebeStartAsyncProcess.class, "value")
                .orElse("");
        int version = context.intValue(ZeebeStartAsyncProcess.class, "version")
                .orElse(0);
        Map<String, Object> variables = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(BPMN_PROCESS_ID))
                bpmnProcessId = (String) parameterValues[i];
            if (arguments[i].getName().equals(VERSION))
                version = (int) parameterValues[i];
            if (arguments[i].getName().equals(VARIABLES))
                variables = (Map<String, Object>) parameterValues[i];
        }
        if (StringUtils.isEmpty(bpmnProcessId))
            throw new IllegalArgumentException("Message name must not be null or empty!");
        return asyncCreateInstance(bpmnProcessId, version, variables);
    }

    /**
     * Command to create/start a new instance of a process
     *
     * @param bpmnProcessId Set the BPMN process id of the process to create an
     *                      instance of. This is the static id of the process in the
     *                      BPMN XML (i.e. "<bpmn:process id='my-process'>").
     * @param variables     Set the initial variables of the process instance.
     * @param version       Set the version of the process to create an instance of.
     *                      The version is assigned by the broker while deploying
     *                      the process. It can be picked from the deployment or
     *                      process event.
     */
    private CompletableFuture<ProcessInstanceEvent> asyncCreateInstance(String bpmnProcessId, int version, Map<String, Object> variables) {
        logger.debug("asyncCreateInstance() >> Call create instance command with parameters: bpmnProcessId: {}, version: {}, variables: {}",
                bpmnProcessId, version, variables);
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        if (StringUtils.isEmpty(bpmnProcessId))
            throw new IllegalArgumentException("Process id must not be null or empty!");
        var step2 = client.get().newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcessId);
        CompletableFuture<ProcessInstanceEvent> processInstanceEvent;
        if (version == 0) {
            processInstanceEvent = (Objects.isNull(variables) || variables.isEmpty())
                    ? step2.latestVersion().send().toCompletableFuture()
                    : step2.latestVersion().variables(variables).send().toCompletableFuture();
        } else {
            processInstanceEvent = (Objects.isNull(variables) || variables.isEmpty())
                    ? step2.version(version).send().toCompletableFuture()
                    : step2.version(version).variables(variables).send().toCompletableFuture();
        }
        return processInstanceEvent;
    }
}
