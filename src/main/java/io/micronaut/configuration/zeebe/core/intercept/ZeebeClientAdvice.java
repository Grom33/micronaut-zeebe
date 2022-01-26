package io.micronaut.configuration.zeebe.core.intercept;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.ZeebeClient;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.executor.WorkerExecutorServiceConfig;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.ReturnType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author Gromov Vitaly.
 * @since 0.1.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Context
@InterceptorBean(ZeebeClient.class)
public class ZeebeClientAdvice implements MethodInterceptor<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeClientAdvice.class);
    private final Map<Class<? extends Annotation>, Command<? extends Annotation>> commands;
    private final Scheduler executorScheduler;

    @Inject
    public ZeebeClientAdvice(List<Command<? extends Annotation>> commandList,
                             @Named(WorkerExecutorServiceConfig.ZEEBE) ExecutorService executorService) {
        this.commands = commandList.stream()
                .collect(Collectors.toUnmodifiableMap(Command::getAnnotationClass, command -> command));
        if (logger.isInfoEnabled())
            logger.info("constructor() >> Zeebe commands registered: {}",
                    commandList.stream()
                            .map(c -> c.getAnnotationClass().getSimpleName())
                            .collect(Collectors.toList()));
        this.executorScheduler = Schedulers.fromExecutor(executorService);
    }

    @Nullable
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        logger.debug("intercept() >> Method invocation context");
        final Optional<Command<? extends Annotation>> zeebeCommand = getZeebeCommand(context);
        if (zeebeCommand.isPresent()) {
            final CompletableFuture<?> invoke = zeebeCommand.get().invoke(context);
            final ReturnType<Object> returnType = context.getReturnType();
            if (returnType.isAsync())
                return invoke;
            if (returnType.isReactive()) {
                if (returnType.getType().isAssignableFrom(Mono.class)) {
                    return Mono.fromFuture(invoke)
                            .subscribeOn(executorScheduler);
                }
                return Mono.fromFuture(invoke)
                        .flux()
                        .subscribeOn(executorScheduler);
            }
            final Object result = invoke.join();
            if (returnType.isVoid())
                return null;
            return result;
        }
        logger.debug("intercept() >> method {} doesn't zeebe client command", context.getName());
        return null;
    }

    private Optional<Command<? extends Annotation>> getZeebeCommand(MethodInvocationContext<Object, Object> context) {
        final List<Class<? extends Annotation>> existed = commands.keySet().stream()
                .filter(context::hasAnnotation)
                .collect(Collectors.toList());
        if (existed.size() > 1)
            throw new IllegalArgumentException(
                    String.format("Zeebe method %s can't contain more then one Zeebe command annotation!",
                            context.getName()));
        return (existed.isEmpty())
                ? Optional.empty()
                : Optional.of(commands.get(existed.get(0)));
    }

}
