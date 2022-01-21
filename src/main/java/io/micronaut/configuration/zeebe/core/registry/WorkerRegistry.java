package io.micronaut.configuration.zeebe.core.registry;

import io.micronaut.configuration.zeebe.core.configuration.WorkerConfiguration;
import io.micronaut.configuration.zeebe.core.specification.WorkerSpecification;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;

import java.util.Collection;

/**
 * A worker registry interface that allows you to register and manage workers.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public interface WorkerRegistry {

    /**
     * Register a worker/
     *
     * @param beanDefinition Bean Definition of the class that was annotated with the ZeebeWorker annotation
     * @param method         Definition of worker class method
     * @see io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker
     */
    void registerWorker(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method);

    /**
     * Stops the worker with the specified name
     *
     * @param type Name of worker
     * @return success
     */
    boolean stopWorker(String type);

    /**
     * Resume the worker with the specified name
     *
     * @param type Name of worker
     * @return success
     */
    boolean resumeWorker(String type);

    /**
     * Restart the worker with the specified name
     *
     * @param type Name of worker
     * @return success
     */
    boolean restartWorker(String type);

    /**
     * @return Worker configuration
     */
    Collection<WorkerConfiguration> getWorkerConfigurations();

    /**
     * @return Worker specification
     */
    Collection<WorkerSpecification> getWorkerSpecification();
}
