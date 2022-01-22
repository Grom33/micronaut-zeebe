package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.SetVariablesResponse;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.variable.ZeebeSetVariables;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".set-variables",
        notEquals = StringUtils.TRUE)
@Singleton
public class SetVariablesCommand implements Command<ZeebeSetVariables> {

    public static final String VARIABLES = "variables";
    public static final String ELEMENT_INSTANCE_KEY = "elementInstanceKey";

    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public SetVariablesCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        final boolean local = context.booleanValue(ZeebeSetVariables.class, "local").orElse(false);
        long elementInstanceKey = 0;
        Map<String, Object> variables = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(ELEMENT_INSTANCE_KEY))
                elementInstanceKey = (long) parameterValues[i];
            if (arguments[i].getName().equals(VARIABLES))
                variables = (Map<String, Object>) parameterValues[i];
        }
        if (elementInstanceKey == 0)
            throw new IllegalArgumentException("Element instance key must not be zero!");
        return setVariables(elementInstanceKey, variables, local);
    }

    /**
     * Command to set and/or update the variables of a given flow element (e.g.
     * process instance, task, etc.)
     *
     * @param elementInstanceKey - The key of the element instance to set/update the
     *                           variables for
     * @param variables          - The variables document as map
     * @param local              - Whether or not to update only the local scope. If
     *                           true, the variables will be merged strictly into
     *                           the local scope (as indicated by
     *                           elementInstanceKey); this means the variables is
     *                           not propagated to upper scopes. For example, let's
     *                           say we have two scopes, '1' and '2', with each
     *                           having effective variables as: 1 => `{ "foo" : 2 }`
     *                           2 => `{ "bar" : 1 }` If we send an update request
     *                           with elementInstanceKey = 2, a new document of `{
     *                           "foo" : 5 }`, and local is true, then scope 1 will
     *                           be unchanged, and scope 2 will now be `{ "bar" : 1,
     *                           "foo" 5 }`. If local was false, however, then scope
     *                           1 would be `{ "foo": 5 }`, and scope 2 would be `{
     *                           "bar" : 1 }`.
     * @return key of the set variables command
     */
    private CompletableFuture<SetVariablesResponse> setVariables(long elementInstanceKey, Map<String, Object> variables, boolean local) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        return client.get().newSetVariablesCommand(elementInstanceKey)
                .variables(variables).local(local).send().toCompletableFuture();
    }
}
