package io.micronaut.configuration.zeebe.core.mock;

import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextMapper;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeContextVariable;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeHeader;
import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import io.micronaut.configuration.zeebe.core.annotation.job.error.ZeebeError;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;
import io.micronaut.configuration.zeebe.core.mock.exception.ZeebeTest1Exception;
import io.micronaut.configuration.zeebe.core.mock.exception.ZeebeTest2Exception;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SyncWorker implements TestData {

    private static final Logger logger = LoggerFactory.getLogger(SyncWorker.class);
    public static final String SYNC_TEST_WORKER_VOID = "sync-test-worker-void";
    public static final String SYNC_TEST_WORKER_MULTIPLY_X_2 = "sync-test-worker-multiply-x-2";
    public static final String SYNC_TEST_WORKER_MULTIPLY_X_2_OPTIONAL = "sync-test-worker-multiply-x-2-optional";
    public static final String SYNC_TEST_WORKER_TEST_INPUT_DATA = "sync-test-worker-test-input-data";
    public static final String SYNC_TEST_WORKER_RETURN_DTO = "sync-test-worker-return-dto";
    public static final String RESULT_DTO = "resultDto";

    private Map<String, Object> testData = new ConcurrentHashMap<>();

    @ZeebeWorker(type = SYNC_TEST_WORKER_MULTIPLY_X_2, outputVariableName = "result")
    public int multiplyWorker(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return value * 2;
    }

    @ZeebeWorker(type = SYNC_TEST_WORKER_MULTIPLY_X_2_OPTIONAL, outputVariableName = "result")
    public Optional<Integer> multiplyWorkerOptional(@ZeebeContextVariable("valueToMultiply") final int value) {
        logger.debug("multiply() >> value: {}", value);
        return Optional.of(value * 2);
    }

    @ZeebeWorker(SYNC_TEST_WORKER_VOID)
    public void voidWorker(@ZeebeContextVariable("someIntValue") final int value) {
        logger.debug("voidWorker() >> value: {}", value);
        testData.put(SYNC_TEST_WORKER_VOID, value);
    }

    @ZeebeWorker(SYNC_TEST_WORKER_TEST_INPUT_DATA)
    public String inputDataTestWorker(final Long key,
                                      final Map<String, Object> variables,
                                      final Map<String, String> headers,
                                      final Long processInstanceKey,
                                      String bpmnProcessId,
                                      int processDefinitionVersion,
                                      long processDefinitionKey,
                                      String elementId,
                                      long elementInstanceKey,
                                      int retries,
                                      long deadline,
                                      @Nullable @ZeebeContextVariable("value10") final String v,
                                      @ZeebeHeader("header1") final String h,
                                      @Nullable @ZeebeContextMapper("test-input.x.y") final Dto dto,
                                      @Nullable @ZeebeContextMapper final Dto dto2) {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("variables", variables);
        data.put("dto", dto);
        data.put("dto2", dto2);
        data.put("processInstanceKey", processInstanceKey);
        data.put("bpmnProcessId", bpmnProcessId);
        data.put("processDefinitionVersion", processDefinitionVersion);
        data.put("processDefinitionKey", processDefinitionKey);
        data.put("elementId", elementId);
        data.put("elementInstanceKey", elementInstanceKey);
        data.put("retries", retries);
        data.put("deadline", deadline);
        testData.put(SYNC_TEST_WORKER_TEST_INPUT_DATA, data);
        logger.debug("inputDataTestWorker() >> Get data: {}", data);
        return h;
    }

    @ZeebeWorker(type = SYNC_TEST_WORKER_RETURN_DTO, outputVariableName = RESULT_DTO)
    public Dto returnDtoWorker(@ZeebeContextMapper final Dto dto) {
        logger.debug("returnDtoWorker() >> dto: {}", dto);
        return dto;
    }

    @ZeebeWorker(type = "sync-throw-worker", errors = {
            @ZeebeError(code = "11", throwable = { ZeebeTest1Exception.class }),
            @ZeebeError(code = "22", throwable = { ZeebeTest2Exception.class })
    })
    public void throwWorker(@Nullable @ZeebeContextVariable("trigger") Boolean trigger,
                            @Nullable @ZeebeContextVariable("keepCalm") String keepCalm) {
        logger.debug("throwWorker() >> trigger: {}, keepCalm: {}", trigger, keepCalm);
        if (StringUtils.isNotEmpty(keepCalm))
            return;
        if (trigger)
            throw new ZeebeTest1Exception();
        else
            throw new ZeebeTest2Exception();
    }

    @Override
    public Object getTestData(String workerName) {
        return testData.get(workerName);
    }

    @Override
    public void drainTestData() {
        testData = new ConcurrentHashMap<>();
    }

}
