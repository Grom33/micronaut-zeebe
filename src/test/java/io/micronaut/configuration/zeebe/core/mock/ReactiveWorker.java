package io.micronaut.configuration.zeebe.core.mock;

import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextMapper;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextVariable;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Singleton
public class ReactiveWorker {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveWorker.class);

    @ZeebeWorker(type = "reactive-test-worker-multiply-x-2", outputVariableName = "result")
    public Mono<Integer> multiplyWorker(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return Mono.just(value * 2);
    }

    @ZeebeWorker(type = "reactive-test-worker-multiply-x-2-optional", outputVariableName = "result")
    public Mono<Optional<Integer>> multiplyWorkerOptional(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return Mono.just(Optional.of(value * 2));
    }

    @ZeebeWorker(type = "reactive-test-worker-return-dto", outputVariableName = "resultDto")
    public Mono<Dto> returnDtoWorker(@ZeebeContextMapper final Dto dto) {
        logger.debug("returnDtoWorker() >> dto: {}", dto);
        return Mono.just(dto);
    }

    @ZeebeWorker(type = "reactive-test-worker-return-flowable", outputVariableName = "resultList")
    public Flux<String> returnFlowableWorker(@ZeebeContextMapper final Dto dto) {
        logger.debug("returnDtoWorker() >> dto: {}", dto);
        return Flux.just("value1", "value2");
    }
}
