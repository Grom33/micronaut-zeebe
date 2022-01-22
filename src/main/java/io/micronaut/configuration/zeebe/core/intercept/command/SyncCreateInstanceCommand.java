package io.micronaut.configuration.zeebe.core.intercept.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.CreateProcessInstanceCommandStep1;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeStartAsyncProcess;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeStartSyncProcess;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command to create/start a new instance of a process When this method is
 * called, the response to the command will be received after the process is
 * completed. The response consists of a set of variables.
 *
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".sync-create-instance",
        notEquals = StringUtils.TRUE)
@Singleton
public class SyncCreateInstanceCommand implements Command<ZeebeStartSyncProcess> {

    public static final String VARIABLES = "variables";
    public static final String BPMN_PROCESS_ID = "bpmnProcessId";

    private final ZeebeClusterConnectionManager connectionManager;
    private final ObjectMapper mapper;

    @Inject
    public SyncCreateInstanceCommand(ZeebeClusterConnectionManager connectionManager, ObjectMapper mapper) {
        this.connectionManager = connectionManager;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        String bpmnProcessId = (String) context.getValue(ZeebeStartSyncProcess.class, "value")
                .orElse("");
        int version = context.intValue(ZeebeStartAsyncProcess.class, "version")
                .orElse(0);
        Map<String, Object> variables = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(BPMN_PROCESS_ID))
                bpmnProcessId = (String) parameterValues[i];
            if (arguments[i].getName().equals(BPMN_PROCESS_ID))
                version = (int) parameterValues[i];
            if (arguments[i].getName().equals(VARIABLES))
                variables = (Map<String, Object>) parameterValues[i];
        }
        if (StringUtils.isEmpty(bpmnProcessId))
            throw new IllegalArgumentException("Message name must not be null or empty!");
        Class<Object> type = context.getReturnType().getType();
        if (type.isPrimitive())
            throw new IllegalArgumentException("Return type of sync start process command must not be primitive!");
        // TODO: 27.11.2021 Добавить возможность указывать переменные в аннтоации
        final List<String> fetchVariables = (!type.isAssignableFrom(Map.class))
                ? Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toList())
                : List.of();
        CompletableFuture<ProcessInstanceResult> instanceResult = syncCreateInstance(bpmnProcessId, version, variables, fetchVariables);
        if (type.isAssignableFrom(ProcessInstanceResult.class))
            return instanceResult;
        if (type.isAssignableFrom(Map.class))
            return instanceResult
                    .thenApplyAsync(ProcessInstanceResult::getVariablesAsMap);
        return instanceResult.thenApplyAsync(r -> mapper.convertValue(r.getVariablesAsMap(), type));
    }

    /**
     * Command to create/start a new instance of a process When this method is
     * called, the response to the command will be received after the process is
     * completed. The response consists of a set of variables.
     *
     * @param bpmnProcessId  - Set the BPMN process id of the process to create an
     *                       instance of. This is the static id of the process in
     *                       the BPMN XML (i.e. "<bpmn:process id='my-process'>").
     * @param version        - Set the version of the process to create an instance
     *                       of. The version is assigned by the broker while
     *                       deploying the process. It can be picked from the
     *                       deployment or process event.
     * @param variables      - Set the initial variables of the process instance.
     * @param fetchVariables - Set a list of variables names which should be fetched
     *                       in the response.
     * @return Process result.
     */
    private CompletableFuture<ProcessInstanceResult> syncCreateInstance(String bpmnProcessId, int version, Map<String, Object> variables,
                                                                        List<String> fetchVariables) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        if (StringUtils.isEmpty(bpmnProcessId))
            throw new IllegalArgumentException("Process id must not be null or empty!");
        var step2 = client.get().newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcessId);
        CreateProcessInstanceCommandStep1.CreateProcessInstanceWithResultCommandStep1 step3;
        if (version == 0) {
            step3 = (Objects.isNull(variables) || variables.isEmpty())
                    ? step2.latestVersion().withResult()
                    : step2.latestVersion().variables(variables).withResult();
        } else {
            step3 = (Objects.isNull(variables) || variables.isEmpty())
                    ? step2.version(version).withResult()
                    : step2.version(version).variables(variables).withResult();
        }

        return (fetchVariables.isEmpty())
                ? step3.send().toCompletableFuture()
                : step3.fetchVariables(fetchVariables).send().toCompletableFuture();
    }
}
