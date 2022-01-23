package io.micronaut.configuration.zeebe.core.intercept;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.reflect.GenericTypeUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */

public interface Command<T extends Annotation> {

    @SuppressWarnings("java:S1452")
    CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context);

    default Class<T> getAnnotationClass() {
        return GenericTypeUtils.resolveInterfaceTypeArgument(this.getClass(), Command.class)
                .filter(Class::isAnnotation)
                .map(c -> (Class<T>) c)
                .orElseThrow();
    }

}
