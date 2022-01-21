package io.micronaut.configuration.zeebe.core.binder;

import java.lang.annotation.Annotation;

/**
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public interface AnnotatedJobBinder<A extends Annotation, T> extends JobBinder<T> {

    Class<A> annotationType();
}
