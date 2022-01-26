package io.micronaut.configuration.zeebe.core.mock;

import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextMapper;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextVariable;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class AsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncWorker.class);
    public static final String ASYNC_TEST_WORKER_MULTIPLY_X_2_OPTIONAL = "async-test-worker-multiply-x-2-optional";
    public static final String ASYNC_TEST_WORKER_MULTIPLY_X_2 = "async-test-worker-multiply-x-2";
    public static final String ASYNC_TEST_WORKER_VOID = "async-test-worker-void";
    public static final String ASYNC_TEST_WORKER_RETURN_DTO = "async-test-worker-return-dto";

    @ZeebeWorker(type = ASYNC_TEST_WORKER_MULTIPLY_X_2, outputVariableName = "result")
    public CompletableFuture<Integer> multiplyWorker(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return CompletableFuture.completedFuture(value * 2);
    }

    @ZeebeWorker(type = ASYNC_TEST_WORKER_MULTIPLY_X_2_OPTIONAL, outputVariableName = "result")
    public CompletableFuture<Optional<Integer>> multiplyWorkerOptional(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return CompletableFuture.completedFuture(Optional.of(value * 2));
    }

    @ZeebeWorker(ASYNC_TEST_WORKER_VOID)
    public CompletableFuture<Void> voidWorker(@ZeebeContextVariable("someIntValue") final int value) {
        logger.debug("voidWorker() >> value: {}", value);
        return CompletableFuture.completedFuture(null);
    }

    @ZeebeWorker(type = ASYNC_TEST_WORKER_RETURN_DTO, outputVariableName = "resultDto")
    public CompletableFuture<Dto> returnDtoWorker(@ZeebeContextMapper final Dto dto) {
        logger.debug("returnDtoWorker() >> dto: {}", dto);
        return CompletableFuture.completedFuture(dto);
    }

}
