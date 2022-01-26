package io.micronaut.configuration.zeebe.core.annotation.complete;

import java.lang.annotation.*;

/**
 * Command to complete a job. If the job is linked to a process instance then
 * this command will complete the related activity and continue the flow.
 * <p>
 * 1. jobKey - the key which identifies the job 2. variables - the variables as
 * map
 * <p>
 * Return void or CompleteJobResponse
 * <p>
 * io.camunda.zeebe.client.api.response.CompleteJobResponse;
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeCompleteCommand {
}
