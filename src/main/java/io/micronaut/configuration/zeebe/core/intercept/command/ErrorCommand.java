package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeThrowError;
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
 * Command to report a business error (i.e. non-technical) that occurs while
 * processing a job. The error is handled in the process by an error catch
 * event. If there is no error catch event with the specified errorCode then an
 * incident will be raised instead.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".error",
        notEquals = StringUtils.TRUE)
@Singleton
public class ErrorCommand implements Command<ZeebeThrowError> {

    public static final String JOB_KEY = "jobKey";
    public static final String ERROR_MSG = "errorMsg";
    public static final String ERROR_CODE = "errorCode";
    private final ZeebeClusterConnectionManager connectionManager;

    @Inject
    public ErrorCommand(ZeebeClusterConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();
        long jobKey = 0;
        String errorCode = null;
        String errorMsg = null;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getName().equals(JOB_KEY))
                jobKey = (long) parameterValues[i];
            if (arguments[i].getName().equals(ERROR_CODE))
                errorCode = (String) parameterValues[i];
            if (arguments[i].getName().equals(ERROR_MSG))
                errorMsg = (String) parameterValues[i];
        }
        if (jobKey == 0)
            throw new IllegalArgumentException("Job key must not be zero!");
        if (StringUtils.isEmpty(errorCode))
            throw new IllegalArgumentException("Error code must not be empty!");
        return throwError(jobKey, errorCode, errorMsg);
    }

    /**
     * Command to report a business error (i.e. non-technical) that occurs while
     * processing a job. The error is handled in the process by an error catch
     * event. If there is no error catch event with the specified errorCode then an
     * incident will be raised instead.
     *
     * @param jobKey    - the key which identifies the job
     * @param errorCode - the errorCode that will be matched against an error catch
     *                  event. If the errorCode can't be matched to an error catch
     *                  event in the process, an incident will be created
     * @param errorMsg  - error message. Provide an error message describing the
     *                  reason for the non-technical error. If the error is not
     *                  caught by an error catch event, this message will be a part
     */
    private CompletableFuture<Void> throwError(long jobKey, String errorCode, String errorMsg) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException("Can't start process, zeebe broker isn't available!");
        final var step1 = client.get().newThrowErrorCommand(jobKey).errorCode(errorCode);
        if (StringUtils.isNotEmpty(errorMsg))
            return step1.errorMessage(errorMsg).send().toCompletableFuture();
        return step1.send().toCompletableFuture();
    }
}
