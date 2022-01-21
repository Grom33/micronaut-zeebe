package io.micronaut.configuration.zeebe.core.binder;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextVariable;
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
 * Binder that extract object from map of business process
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class ZeebeContextVariablesBinder<T> implements AnnotatedJobBinder<ZeebeContextVariable, T> {

    @Override
    public Class<ZeebeContextVariable> annotationType() {
        return ZeebeContextVariable.class;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, ActivatedJob source) {
        final Map<String, Object> vars = source.getVariablesAsMap();
        final AnnotationMetadata annotationMetadata = context.getAnnotationMetadata();
        final String name = annotationMetadata
                .stringValue(ZeebeContextVariable.class)
                .orElse(context.getArgument().getName());
        final Object value = vars.get(name);
        if (Objects.nonNull(value)) {
            Optional<T> converted = ConversionService.SHARED.convert(value, context);
            return () -> converted;
        }
        return BindingResult.EMPTY;
    }
}
