package io.micronaut.configuration.zeebe.core.processsor;

import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.registry.WorkerRegistry;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class ZeebeJobMethodProcessor implements ExecutableMethodProcessor<ZeebeWorker> {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeJobMethodProcessor.class);
    private final WorkerRegistry zeebeWorkerRegistry;

    @Inject
    public ZeebeJobMethodProcessor(WorkerRegistry zeebeWorkerRegistry, BeanContext beanContext) {
        this.zeebeWorkerRegistry = zeebeWorkerRegistry;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        logger.debug("process() >> bean def: {}, method: {}", beanDefinition.getBeanType(), method.getArguments());
        zeebeWorkerRegistry.registerWorker(beanDefinition, method);
    }
}
