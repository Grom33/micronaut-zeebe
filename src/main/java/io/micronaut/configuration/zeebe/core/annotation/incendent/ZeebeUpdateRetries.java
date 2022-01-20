package io.micronaut.configuration.zeebe.core.annotation.incendent;

import java.lang.annotation.*;

/**
 * Command to update the retries of a job. If the given retries are greater than
 * zero then this job will be picked up again by a job worker. This will not
 * close a related incident, which still has to be marked as resolved with
 * newResolveIncidentCommand(long incidentKey). 1. jobKey - the key of the job
 * to update 2. retries - the retries of this job Return void or
 * UpdateRetriesJobResponse
 *
 * io.camunda.zeebe.client.api.response.UpdateRetriesJobResponse;
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeUpdateRetries {
}
