package io.micronaut.configuration.zeebe.core.binder;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class JobBinderRegistry implements ArgumentBinderRegistry<ActivatedJob> {

    private final Map<Class<? extends Annotation>, ArgumentBinder<?, ?>> argumentBinderMap = new HashMap<>();

    public JobBinderRegistry(JobBinder<?>... binders) {
        Stream.of(binders).forEach(binder -> {
            if (binder instanceof AnnotatedJobBinder) {
                AnnotatedJobBinder<?, ?> annotatedJobBinder = (AnnotatedJobBinder<?, ?>) binder;
                argumentBinderMap.put(annotatedJobBinder.annotationType(), annotatedJobBinder);
            }
        });
    }

    @Override
    public <T> Optional<ArgumentBinder<T, ActivatedJob>> findArgumentBinder(Argument<T> argument, ActivatedJob source) {
        Optional<Class<? extends Annotation>> annotationType = argument.getAnnotationMetadata()
                .getAnnotationTypeByStereotype(Bindable.class);
        if (annotationType.isPresent()) {
            JobBinder<T> binder = (JobBinder<T>) argumentBinderMap.get(annotationType.get());
            return Optional.ofNullable(binder);
        } else {
            return Optional.of(new JobDefaultBinder<>());
        }
    }
}
