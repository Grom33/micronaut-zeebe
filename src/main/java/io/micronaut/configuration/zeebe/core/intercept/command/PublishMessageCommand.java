package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.message.ZeebePublishMessage;
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command to publish a message which can be correlated to a process instance.
 *
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".push-message", notEquals = StringUtils.TRUE)
@Singleton
public class PublishMessageCommand implements Command<ZeebePublishMessage> {

    private static final Logger logger = LoggerFactory.getLogger(PublishMessageCommand.class);
    public static final String MESSAGE_NAME = "messageName";
    public static final String CORRELATION_KEY = "correlationKey";
    public static final String MESSAGE_ID = "messageId";
    public static final String VARIABLES = "variables";

    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public PublishMessageCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        String messageName = (String) context.getValue(ZeebePublishMessage.class, "value")
                .orElse(null);
        String correlationKey = null;
        String messageId = null;
        Map<String, ?> variables = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(MESSAGE_NAME))
                messageName = (String) parameterValues[i];
            if (arguments[i].getName().equals(CORRELATION_KEY))
                correlationKey = (String) parameterValues[i];
            if (arguments[i].getName().equals(VARIABLES))
                variables = (Map<String, ?>) parameterValues[i];
            if (arguments[i].getName().equals(MESSAGE_ID))
                messageId = (String) parameterValues[i];
        }
        return publishMessage(messageName, correlationKey, messageId, variables);
    }

    /**
     * Command to publish a message which can be correlated to a process instance.
     *
     * @param messageName    - the name of the message
     * @param correlationKey - Set the value of the correlation key of the message.
     *                       This value will be used together with the message name
     *                       to find matching message subscriptions.
     * @param messageId      - Set the id of the message. The message is rejected if
     *                       another message is already published with the same id,
     *                       name and correlation-key.
     * @param variables      - Set the variables of the message.
     * @return Returns the record key of the message that was published
     */
    private CompletableFuture<?> publishMessage(String messageName, String correlationKey,
                                                String messageId, Map<String, ?> variables) {
        logger.debug("publishMessage() >> Call publish message with parameters: message name: {}, correlation key: {}, variables: {}",
                messageName, correlationKey, variables);
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (StringUtils.isEmpty(messageName))
            throw new IllegalArgumentException("Message name must not be null or empty!");
        if (StringUtils.isEmpty(correlationKey))
            throw new IllegalArgumentException("Correlation key must not be null or empty!");
        if (client.isEmpty())
            throw new IllegalStateException("Can't publish message, zeebe broker isn't available!");
        var step3 = client.get().newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationKey);
        if (StringUtils.isNotEmpty(messageId))
            step3 = step3.messageId(messageId);
        if (Objects.nonNull(variables) && !variables.isEmpty())
            step3 = step3.variables(variables);
        return step3.send().toCompletableFuture();
    }

}
