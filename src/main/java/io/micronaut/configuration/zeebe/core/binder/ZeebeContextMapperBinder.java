package io.micronaut.configuration.zeebe.core.binder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextMapper;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.micronaut.core.util.StringUtils.isEmpty;

/**
 * Binder for context mapper annotation. Extract data from context of business
 * process and map it to object with defined class.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class ZeebeContextMapperBinder<T> implements AnnotatedJobBinder<ZeebeContextMapper, T> {

    private final ObjectMapper mapper;

    @Inject
    public ZeebeContextMapperBinder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<ZeebeContextMapper> annotationType() {
        return ZeebeContextMapper.class;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, ActivatedJob source) {

        final String path = context.getArgument().getAnnotationMetadata()
                .stringValue(ZeebeContextMapper.class, "path")
                .orElse("");

        if (StringUtils.isNotEmpty(path)) {
            Optional<Object> rawExtractedValue = extractVariableFromMap(path, source.getVariablesAsMap());
            if (rawExtractedValue.isEmpty() && !context.getArgument().isNullable())
                throw new UnsatisfiedArgumentException(context.getArgument());
            if (rawExtractedValue.isEmpty())
                return BindingResult.EMPTY;
            final T value = mapper.convertValue(rawExtractedValue.get(), context.getArgument().getType());
            if (Objects.nonNull(value))
                return () -> Optional.of(value);
            return BindingResult.EMPTY;
        }

        final T value = source.getVariablesAsType(context.getArgument().getType());
        if (Objects.nonNull(value))
            return () -> Optional.of(value);
        return BindingResult.EMPTY;
    }

    /**
     * Method tries to extract object from map of business process context by
     * defined path
     *
     * @param path   Path to object from map of business process context
     * @param source Map of business process context
     * @return extracted object
     */
    private Optional<Object> extractVariableFromMap(final String path, final Map<String, Object> source) {
        if (isEmpty(path) || Objects.isNull(source) || source.size() == 0)
            return Optional.empty();
        Object result = null;
        if (path.contains(".")) {
            final List<String> pathKeys = StringUtils.splitOmitEmptyStringsList(path, '.');
            Map point = source;
            for (int i = 0, pathKeysSize = pathKeys.size(); i < pathKeysSize; i++) {
                final String pathKey = pathKeys.get(i);
                if (!point.containsKey(pathKey) || point.get(pathKey) == null)
                    return Optional.empty();
                final Object currentValue = point.get(pathKey);
                if (i == pathKeysSize - 1) {
                    result = currentValue;
                } else {
                    if (currentValue instanceof Map) {
                        point = (Map) currentValue;
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } else {
            result = source.get(path);
        }
        return Optional.ofNullable(result);
    }
}
