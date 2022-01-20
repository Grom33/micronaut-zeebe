package io.micronaut.configuration.zeebe.core.annotation.incendent;

import java.lang.annotation.*;

/**
 * Command to mark a job as failed. If the given retries are greater than zero
 * then this job will be picked up again by a job subscription. Otherwise, an
 * incident is created for this job.
 * <p>
 * 1. jobKey - The key which identifies the job 2. remainingRetries - The
 * remaining retries of this job (e.g. "jobEvent.getRetries() - 1"). If the
 * retries are greater than zero then this job will be picked up again by a job
 * subscription. Otherwise, an incident is created for this job. 3. errorMsg -
 * Error message to be attached to the failed job. Provide an error message
 * describing the reason for the job failure. If failing the job creates an
 * incident, this error message will be used as incident message.
 * <p>
 * Return: void or FailJobResponse
 *
 * io.camunda.zeebe.client.api.response.FailJobResponse;
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeThrowFail {
}
