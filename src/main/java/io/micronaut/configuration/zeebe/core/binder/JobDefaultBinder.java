package io.micronaut.configuration.zeebe.core.binder;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class JobDefaultBinder<T> implements JobBinder<T> {

    public static final String HEADERS = "headers";
    public static final String VARIABLES = "variables";
    private final Map<Argument<?>, Function<ActivatedJob, ?>> defaultResolver;

    public JobDefaultBinder() {
        this.defaultResolver = new HashMap<>();
        /*
         * Returns: the unique key of the job
         */
        final Function<ActivatedJob, ?> getKey = ActivatedJob::getKey;
        /*
         * Returns: key of the process instance
         */
        final Function<ActivatedJob, ?> getProcessInstanceKey = ActivatedJob::getProcessInstanceKey;
        /*
         * Returns: BPMN process id of the process
         */
        final Function<ActivatedJob, ?> getBpmnProcessId = ActivatedJob::getBpmnProcessId;

        /*
         * Returns: version of the process
         */
        final Function<ActivatedJob, ?> getProcessDefinitionVersion = ActivatedJob::getProcessDefinitionVersion;

        /*
         * Returns: key of the process
         */
        final Function<ActivatedJob, ?> getProcessDefinitionKey = ActivatedJob::getProcessDefinitionKey;

        /*
         * Returns: id of the process element
         */
        final Function<ActivatedJob, ?> getElementId = ActivatedJob::getElementId;

        /*
         * Returns: key of the element instance
         */
        final Function<ActivatedJob, ?> getElementInstanceKey = ActivatedJob::getElementInstanceKey;

        /*
         * Returns: remaining retries
         */
        final Function<ActivatedJob, ?> getRetries = ActivatedJob::getRetries;

        /*
         * Returns: the unix timestamp until when the job is exclusively assigned to
         * this worker (time unit * is milliseconds since unix epoch). If the deadline
         * is exceeded, it can happen that the job is handed to another worker and the
         * work is performed twice.
         */
        final Function<ActivatedJob, ?> getDeadline = ActivatedJob::getDeadline;

        this.defaultResolver.put(
                Argument.of(Long.class, "key"),
                getKey);
        this.defaultResolver.put(
                Argument.of(long.class, "key"),
                getKey);
        this.defaultResolver.put(
                Argument.of(Long.class, "processInstanceKey"),
                getProcessInstanceKey);
        this.defaultResolver.put(
                Argument.of(long.class, "processInstanceKey"),
                getProcessInstanceKey);
        this.defaultResolver.put(
                Argument.of(String.class, "bpmnProcessId"),
                getBpmnProcessId);
        this.defaultResolver.put(
                Argument.of(Integer.class, "processDefinitionVersion"),
                getProcessDefinitionVersion);
        this.defaultResolver.put(
                Argument.of(int.class, "processDefinitionVersion"),
                getProcessDefinitionVersion);
        this.defaultResolver.put(
                Argument.of(Long.class, "processDefinitionKey"),
                getProcessDefinitionKey);
        this.defaultResolver.put(
                Argument.of(long.class, "processDefinitionKey"),
                getProcessDefinitionKey);
        this.defaultResolver.put(
                Argument.of(String.class, "elementId"),
                getElementId);
        this.defaultResolver.put(
                Argument.of(Long.class, "elementInstanceKey"),
                getElementInstanceKey);
        this.defaultResolver.put(
                Argument.of(long.class, "elementInstanceKey"),
                getElementInstanceKey);
        this.defaultResolver.put(
                Argument.of(Integer.class, "retries"),
                getRetries);
        this.defaultResolver.put(
                Argument.of(int.class, "retries"),
                getRetries);
        this.defaultResolver.put(
                Argument.of(Long.class, "deadline"),
                getDeadline);
        this.defaultResolver.put(
                Argument.of(long.class, "deadline"),
                getDeadline);
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, ActivatedJob source) {
        Argument<T> argument = context.getArgument();
        Function<ActivatedJob, ?> f = defaultResolver.get(argument);

        if (f != null) {
            Optional<?> opt = Optional.of(f.apply(source));
            return () -> (Optional<T>) opt;
        } else if (Objects.equals(argument.getType(), ActivatedJob.class)) {
            Optional<ActivatedJob> opt = Optional.of(source);
            // noinspection unchecked
            return () -> (Optional<T>) opt;
        } else {
            if (Objects.equals(argument.getName(), HEADERS)) {
                Object value = source.getCustomHeaders();
                Optional<T> converted = ConversionService.SHARED.convert(value, context);
                return () -> converted;
            } else if (Objects.equals(argument.getName(), VARIABLES)) {
                Object value = source.getVariablesAsMap();
                Optional<T> converted = ConversionService.SHARED.convert(value, context);
                return () -> converted;
            } else {
                Optional<T> converted = Optional.empty();
                return () -> converted;
            }
        }
    }
}
