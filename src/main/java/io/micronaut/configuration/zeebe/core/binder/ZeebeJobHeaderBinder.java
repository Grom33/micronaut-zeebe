package io.micronaut.configuration.zeebe.core.binder;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeHeader;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Binder that extract header value of business process service task
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class ZeebeJobHeaderBinder<T> implements AnnotatedJobBinder<ZeebeHeader, T> {

    @Override
    public Class<ZeebeHeader> annotationType() {
        return ZeebeHeader.class;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, ActivatedJob source) {
        final Map<String, String> headers = source.getCustomHeaders();
        final AnnotationMetadata annotationMetadata = context.getAnnotationMetadata();
        final String name = annotationMetadata
                .stringValue(ZeebeHeader.class)
                .orElse(context.getArgument().getName());
        final Object value = headers.get(name);
        if (Objects.nonNull(value)) {
            Optional<T> converted = ConversionService.SHARED.convert(value, context);
            return () -> converted;
        }
        return BindingResult.EMPTY;
    }
}
