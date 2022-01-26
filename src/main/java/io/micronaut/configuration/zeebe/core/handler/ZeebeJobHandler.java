package io.micronaut.configuration.zeebe.core.handler;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.micronaut.configuration.zeebe.core.binder.JobBinderRegistry;
import io.micronaut.configuration.zeebe.core.configuration.WorkerConfiguration;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.inject.ExecutableMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of Zeebe job handler
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class ZeebeJobHandler implements JobHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeJobHandler.class);
    private final WorkerConfiguration configuration;
    private final ExecutableMethod<?, ?> method;
    private final Object bean;
    private final JobBinderRegistry jobBinderRegistry;
    private final Scheduler executorScheduler;

    public ZeebeJobHandler(WorkerConfiguration configuration, ExecutableMethod<?, ?> method,
                           Object bean, JobBinderRegistry jobBinderRegistry,
                           Scheduler executorScheduler) {
        this.configuration = configuration;
        this.method = method;
        this.bean = bean;
        this.jobBinderRegistry = jobBinderRegistry;
        this.executorScheduler = executorScheduler;
    }

    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {
        logger.debug("handle() >> Handle job with type:{} and instance id: {} for process: {}",
                job.getType(), job.getProcessInstanceKey(), job.getBpmnProcessId());
        if (method.getReturnType().isVoid()) {
            try {
                final BoundExecutable boundExecutable = getBoundExecutable(job);
                boundExecutable.invoke(bean);
                completeJob(client, job, null);
            } catch (Exception e) {
                handleThrowable(client, job, e);
            }
            return;
        }
        invokeMethod(job)
                .subscribeOn(executorScheduler)
                .subscribe(r -> completeJob(client, job, r), throwable -> handleThrowable(client, job, throwable));

    }

    private void completeJob(final JobClient client, final ActivatedJob job, final Object r) {
        logger.debug("completeJob() >> Send result to zeebe...");
        if (!configuration.isAutoComplete())
            return;
        final Optional<?> resultContainer = (Objects.nonNull(r) && r.getClass().isAssignableFrom(Optional.class))
                ? (Optional<?>) r
                : Optional.ofNullable(r);
        resultContainer.ifPresentOrElse(
                result -> client.newCompleteCommand(job.getKey())
                        .variables(Map.of(configuration.getOutputVariableName(), result)).send()
                        .join(),
                () -> client.newCompleteCommand(job.getKey()).send().join());
    }

    private void handleThrowable(final JobClient client, final ActivatedJob job, final Throwable throwable) {
        logger.warn("handleThrowable() >> for job: {}, process id: {}, instance id: {}, cause: {}",
                job.getType(), job.getBpmnProcessId(), job.getProcessInstanceKey(), throwable.getMessage());
        if (!configuration.isAutoComplete())
            return;
        final Throwable cause = unboxThrowable(throwable);
        final Optional<String> errorCode = getErrorCode(cause);
        if (errorCode.isPresent()) {
            logger.debug("handleThrowable() >> Get error with code: {}", errorCode.get());
            client.newThrowErrorCommand(job.getKey())
                    .errorCode(errorCode.get())
                    .errorMessage(Optional.ofNullable(cause.getMessage())
                            .orElse(cause.getClass().getSimpleName()))
                    .send().join();
            return;
        }
        logger.debug("handleThrowable() >> Get Fail: {}", cause.getMessage());
        client.newFailCommand(job.getKey())
                .retries(job.getRetries() - 1)
                .errorMessage(Optional.ofNullable(cause.getMessage())
                        .orElse(cause.getClass().getSimpleName()))
                .send().join();
    }

    private BoundExecutable getBoundExecutable(ActivatedJob job) {
        DefaultExecutableBinder<ActivatedJob> executableBinder = new DefaultExecutableBinder<>();
        return executableBinder.bind(method, jobBinderRegistry, job);
    }

    private Mono<?> invokeMethod(ActivatedJob job) {
        logger.debug("invokeMethod() >> Try to handle result...");
        final BoundExecutable boundExecutable = getBoundExecutable(job);
        if (method.getReturnType().isAsync()) {
            logger.debug("invokeMethod() >> handle completable future...");
            return Mono.fromFuture((CompletableFuture<?>) boundExecutable.invoke(bean));
        } else if (method.getReturnType().isReactive()) {
            if (method.getReturnType().getType().isAssignableFrom(Mono.class)) {
                logger.debug("invokeMethod() >> handle single...");
                return (Mono<?>) boundExecutable.invoke(bean);
            }
            logger.debug("invokeMethod() >> handle flowable...");
            return ((Flux<?>) boundExecutable.invoke(bean)).collectList();
        } else {
            logger.debug("invokeMethod() >> handle sync result...");
            return Mono.fromCallable(() -> boundExecutable.invoke(bean));
        }
    }

    private Throwable unboxThrowable(final Throwable throwable) {
        if (throwable instanceof ExecutionException || throwable instanceof CompletionException) {
            return (throwable.getCause() == null)
                    ? throwable
                    : unboxThrowable(throwable.getCause());
        } else {
            return throwable;
        }
    }

    private Optional<String> getErrorCode(final Throwable t) {
        final Map<String, List<Class<? extends Throwable>>> errors = configuration.getErrors();
        return errors.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(error -> error.isInstance(t)))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
