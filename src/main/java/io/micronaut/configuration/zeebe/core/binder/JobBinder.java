package io.micronaut.configuration.zeebe.core.binder;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.core.bind.ArgumentBinder;

/**
 * Binder for zeebe ActiveJob
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public interface JobBinder<T> extends ArgumentBinder<T, ActivatedJob> {
}
