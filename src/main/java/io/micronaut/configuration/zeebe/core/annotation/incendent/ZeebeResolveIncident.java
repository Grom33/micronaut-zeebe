package io.micronaut.configuration.zeebe.core.annotation.incendent;

import java.lang.annotation.*;

/**
 * Command to resolve an existing incident.
 * <p>
 * 1. incidentKey the key of the corresponding incident
 * <p>
 * Return void or ResolveIncidentResponse
 *
 * io.camunda.zeebe.client.api.response.ResolveIncidentResponse;
 * 
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeResolveIncident {
}
