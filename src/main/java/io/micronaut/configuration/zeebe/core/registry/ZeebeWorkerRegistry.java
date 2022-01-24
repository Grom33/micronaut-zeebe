package io.micronaut.configuration.zeebe.core.registry;

import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextMapper;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextVariable;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import io.micronaut.configuration.zeebe.core.binder.JobBinderRegistry;
import io.micronaut.configuration.zeebe.core.configuration.WorkerConfiguration;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeClusterConnectionEstablishedEvent;
import io.micronaut.configuration.zeebe.core.connection.event.ZeebeClusterConnectionLostEvent;
import io.micronaut.configuration.zeebe.core.executor.WorkerExecutorServiceConfig;
import io.micronaut.configuration.zeebe.core.handler.ZeebeJobHandler;
import io.micronaut.configuration.zeebe.core.specification.VariableSpecification;
import io.micronaut.configuration.zeebe.core.specification.WorkerSpecification;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Singleton
public class ZeebeWorkerRegistry implements WorkerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeWorkerRegistry.class);
    public static final String DOCUMENTATION = "documentation";
    public static final String AUTO_COMPLETE = "autoComplete";
    public static final String OUTPUT_VARIABLE_NAME = "outputVariableName";
    public static final String ERRORS = "errors";
    public static final String REQUEST_TIMEOUT = "requestTimeout";
    public static final String POLL_INTERVAL = "pollInterval";
    public static final String MAX_JOBS_TO_ACTIVATE = "maxJobsToActivate";
    public static final String TIMEOUT = "timeout";
    public static final String TYPE = "type";
    public static final String CODE = "code";
    public static final String THROWABLE = "throwable";
    private final JobBinderRegistry jobBinderRegistry;
    private final Scheduler executorScheduler;
    private final BeanContext beanContext;
    private final ZeebeClusterConnectionManager connectionManager;
    private final Map<String, WorkerConfiguration> workerConfigurations = new ConcurrentHashMap<>();
    private final Map<String, WorkerSpecification> workerSpecifications = new ConcurrentHashMap<>();
    private final Map<String, JobWorker> activeWorkers = new ConcurrentHashMap<>();
    private final String serviceName;

    @Inject
    public ZeebeWorkerRegistry(JobBinderRegistry jobBinderRegistry, BeanContext beanContext,
                               ZeebeClusterConnectionManager connectionManager,
                               @Named(WorkerExecutorServiceConfig.ZEEBE) ExecutorService executorService,
                               ApplicationConfiguration applicationConfiguration) {
        this.jobBinderRegistry = jobBinderRegistry;
        this.beanContext = beanContext;
        this.connectionManager = connectionManager;
        this.executorScheduler = Schedulers.fromExecutor(executorService);
        this.serviceName = applicationConfiguration.getName().orElse("UNKNOWN");
    }

    @Override
    public void registerWorker(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        logger.debug("Register() >> Register worker");

        final Optional<WorkerConfiguration> maybeWorkerConfiguration = getConfiguration(beanDefinition, method);
        if (maybeWorkerConfiguration.isEmpty()) {
            // TODO: 22.01.2022 What if configuration is empty
            logger.warn("registerWorker() >> Can't get worker configuration for bean: {}", beanDefinition.getBeanType());
            return;
        }
        final WorkerConfiguration workerConfiguration = maybeWorkerConfiguration.get();
        if (workerConfigurations.containsKey(workerConfiguration.getType())) {
            logger.error("registerWorker() >> Such worker: {} is already registered!", workerConfiguration.getType());
            throw new ConfigurationException(String.format("Such worker: %s is already registered!", workerConfiguration.getType()));
        }
        final Object bean = beanContext.getBean(beanDefinition.getBeanType());
        final JobHandler jobHandler = new ZeebeJobHandler(workerConfiguration, method, bean, jobBinderRegistry, executorScheduler);
        workerConfiguration.setHandler(jobHandler);
        workerConfigurations.put(workerConfiguration.getType(), workerConfiguration);
        final Optional<WorkerSpecification> specification = getSpecification(beanDefinition, method, workerConfiguration);
        specification.ifPresent(spec -> workerSpecifications.put(workerConfiguration.getType(), spec));
        CompletableFuture.runAsync(() -> openWorker(workerConfiguration));
    }

    private Optional<WorkerSpecification> getSpecification(final BeanDefinition<?> beanDefinition,
                                                           final ExecutableMethod<?, ?> method,
                                                           final WorkerConfiguration workerConfiguration) {

        logger.debug("getSpecification() >> Get specification for bean: {}", beanDefinition.getName());
        final AnnotationValue<ZeebeWorker> annotation = method.getAnnotation(ZeebeWorker.class);
        if (annotation == null)
            return Optional.empty();

        final WorkerSpecification specification = new WorkerSpecification();
        specification.setTypeName(workerConfiguration.getType());

        final String documentation = annotation.stringValue(DOCUMENTATION)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(beanDefinition::getName);
        specification.setDocumentation(documentation);

        final VariableSpecification outputVarSpecification = new VariableSpecification();
        outputVarSpecification.setVariableName(workerConfiguration.getOutputVariableName());
        final String simpleName = method.getReturnType().getType().getSimpleName();
        outputVarSpecification.setType(simpleName);
        outputVarSpecification.setMappedType(simpleName);
        outputVarSpecification.setRequired(!method.getReturnType().isOptional());
        specification.setOutput(outputVarSpecification);

        return Optional.of(specification);
    }

    private void openWorker(WorkerConfiguration workerConfiguration) {
        logger.debug("openWorker() >> Try to open worker with type: {}", workerConfiguration.getType());
        final Optional<JobWorker> opened = Optional.ofNullable(activeWorkers.get(workerConfiguration.getType()));
        if (opened.isPresent() && opened.get().isOpen()) {
            logger.debug("openWorker() >> Worker with type: {} is already open!", workerConfiguration.getType());
            return;
        }
        connectionManager.getClient().ifPresent(zeebeClient -> {
            logger.info("openWorker() >> Open worker with type: {}", workerConfiguration.getType());
            final JobWorker jobWorker = zeebeClient.newWorker()
                    .jobType(workerConfiguration.getType())
                    .handler(workerConfiguration.getHandler())
                    .pollInterval(workerConfiguration.getPollInterval())
                    .requestTimeout(workerConfiguration.getRequestTimeout())
                    .fetchVariables(workerConfiguration.getFetchVariables())
                    .maxJobsActive(workerConfiguration.getMaxJobsToActivate())
                    .name(serviceName)
                    .timeout(workerConfiguration.getTimeout())
                    .open();
            activeWorkers.put(workerConfiguration.getType(), jobWorker);
        });
    }

    @Async
    @EventListener
    public void onEstablishedConnectionEvent(ZeebeClusterConnectionEstablishedEvent event) {
        logger.debug("onEstablishedConnectionEvent() >> Catch established connection event, try to up workers");
        workerConfigurations.forEach((type, configuration) -> {
            Optional.ofNullable(activeWorkers.get(type))
                    .ifPresent(JobWorker::close);
            openWorker(configuration);
        });
    }

    @Async
    @EventListener
    public void onLostConnectionEvent(ZeebeClusterConnectionLostEvent event) {
        logger.debug("onLostConnectionEvent() >> Catch lost connection event, downgrade workers");
        activeWorkers.entrySet().removeIf(e -> true);
    }

    private Optional<WorkerConfiguration> getConfiguration(BeanDefinition<?> beanDefinition,
                                                           ExecutableMethod<?, ?> method) {
        logger.debug("getConfiguration() >> Get configuration for bean: {}", beanDefinition.getName());
        final AnnotationValue<ZeebeWorker> annotation = method.getAnnotation(ZeebeWorker.class);
        if (annotation == null)
            return Optional.empty();

        final WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setEnabled(true);
        final String type = annotation.stringValue(TYPE)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(beanDefinition::getName);
        workerConfiguration.setType(type);

        final Duration timeout = annotation.stringValue(TIMEOUT)
                .filter(StringUtils::isNotEmpty)
                .flatMap(t -> ConversionService.SHARED.convert(t, Duration.class))
                .orElse(Duration.ofMinutes(5));
        workerConfiguration.setTimeout(timeout);

        final int maxJobsToActivate = annotation.intValue(MAX_JOBS_TO_ACTIVATE)
                .orElse(32);
        workerConfiguration.setMaxJobsToActivate(maxJobsToActivate);

        annotation.longValue(POLL_INTERVAL)
                .ifPresentOrElse(
                        p -> workerConfiguration.setPollInterval(Duration.ofMillis(p)),
                        () -> workerConfiguration.setPollInterval(Duration.ofMillis(100)));

        final Duration requestTimeout = annotation.stringValue(REQUEST_TIMEOUT)
                .filter(StringUtils::isNotEmpty)
                .flatMap(t -> ConversionService.SHARED.convert(t, Duration.class))
                .orElse(Duration.ofSeconds(10));
        workerConfiguration.setRequestTimeout(requestTimeout);

        final List<String> fetchVariables = getFetchVariableNames(method);
        workerConfiguration.setFetchVariables(fetchVariables);

        final Map<String, List<Class<? extends Throwable>>> errors = annotation.getAnnotations(ERRORS).stream()
                .collect(Collectors.toMap(
                        t -> t.stringValue(CODE).orElseThrow(),
                        t -> Stream.of(t.classValues(THROWABLE))
                                .map(e -> (Class<? extends Throwable>) e)
                                .collect(Collectors.toList())));
        workerConfiguration.setErrors(errors);

        workerConfiguration.setMaxJobsToActivate(maxJobsToActivate);

        final String outputVariableName = annotation.stringValue(OUTPUT_VARIABLE_NAME)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> beanDefinition.getBeanType().getSimpleName() + "_" + method.getName());
        workerConfiguration.setOutputVariableName(outputVariableName);

        final boolean autoComplete = annotation.booleanValue(AUTO_COMPLETE)
                .orElse(Boolean.TRUE);
        workerConfiguration.setAutoComplete(autoComplete);

        return Optional.of(workerConfiguration);
    }

    private List<String> getFetchVariableNames(ExecutableMethod<?, ?> method) {
        final List<String> variablesNames = new ArrayList<>();

        Optional.ofNullable(method.getAnnotation(ZeebeWorker.class))
                .ifPresent(annotation -> variablesNames.addAll(List.of(annotation
                        .stringValues("fetchVariables"))));

        Arrays.stream(method.getArguments())
                .filter(argument -> argument.isAnnotationPresent(ZeebeContextVariable.class))
                .map(argument -> argument.getAnnotationMetadata()
                        .stringValue(ZeebeContextVariable.class)
                        .orElse(argument.getName()))
                .forEach(variablesNames::add);

        Arrays.stream(method.getArguments())
                .filter(argument -> argument.isAnnotationPresent(ZeebeContextMapper.class))
                .flatMap(this::extractVariablesForMapper)
                .forEach(variablesNames::add);

        return variablesNames;
    }

    private Stream<String> extractVariablesForMapper(Argument<?> argument) {
        final String path = argument.getAnnotationMetadata().stringValue(ZeebeContextMapper.class)
                .orElse("");

        if (StringUtils.isNotEmpty(path)) {
            final String root = StringUtils.splitOmitEmptyStrings(path, '.').iterator().next();
            return Stream.of(root);
        }
        return Arrays.stream(argument.getType().getDeclaredFields())
                .map(Field::getName);
    }

    @Override
    public boolean stopWorker(String type) {
        try {
            final JobWorker activeJobWorker = activeWorkers.get(type);
            if (activeJobWorker == null) {
                return true;
            }
            final WorkerConfiguration workerConfiguration = workerConfigurations.get(type);
            if (activeJobWorker.isOpen())
                activeJobWorker.close();
            workerConfiguration.setEnabled(false);
            activeWorkers.remove(type);
            return true;
        } catch (Exception e) {
            logger.error("stopWorker() >> Error while stopping worker with type: {}, because: {}", type, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean resumeWorker(final String type) {
        final JobWorker jobWorker = activeWorkers.get(type);
        if (jobWorker != null && jobWorker.isOpen())
            return true;

        final WorkerConfiguration workerConfiguration = workerConfigurations.get(type);
        if (workerConfiguration == null) {
            logger.error("resumeWorker() >> Worker with name: {} isn't define!", type);
            return false;
        }
        workerConfiguration.setEnabled(true);
        openWorker(workerConfiguration);
        return true;
    }

    @Override
    public boolean restartWorker(final String type) {
        return stopWorker(type) && resumeWorker(type);
    }

    @Override
    public Collection<WorkerConfiguration> getWorkerConfigurations() {
        // TODO: 22.01.2022 need deep copy
        return List.copyOf(workerConfigurations.values());
    }

    @Override
    public Collection<WorkerSpecification> getWorkerSpecification() {
        // TODO: 22.01.2022 need deep copy
        return List.copyOf(workerSpecifications.values());
    }
}
