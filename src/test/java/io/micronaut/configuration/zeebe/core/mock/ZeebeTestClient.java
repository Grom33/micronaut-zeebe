package io.micronaut.configuration.zeebe.core.mock;

import io.camunda.zeebe.client.api.response.*;
import io.micronaut.configuration.zeebe.core.annotation.ZeebeClient;
import io.micronaut.configuration.zeebe.core.annotation.complete.ZeebeCompleteCommand;
import io.micronaut.configuration.zeebe.core.annotation.deploy.ZeebeProcessDeploy;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeResolveIncident;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeThrowError;
import io.micronaut.configuration.zeebe.core.annotation.incendent.ZeebeThrowFail;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeCancelProcessInstance;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeStartAsyncProcess;
import io.micronaut.configuration.zeebe.core.annotation.instance.ZeebeStartSyncProcess;
import io.micronaut.configuration.zeebe.core.annotation.message.ZeebePublishMessage;
import io.micronaut.configuration.zeebe.core.mock.dto.Dto;

import java.io.InputStream;
import java.util.Map;

@ZeebeClient
public interface ZeebeTestClient {

    @ZeebePublishMessage("candidate")
    void send(String correlationKey, Map<String, Object> variables);

    @ZeebeStartAsyncProcess("testWorker")
    ProcessInstanceEvent asyncStart(Map<String, Object> variables);

    @ZeebeStartAsyncProcess
    long anotherAsyncStart(String bpmnProcessId, Map<String, Object> variables);

    @ZeebeStartSyncProcess("testWorker")
    Map<String, Object> syncStart(Map<String, Object> variables);

    @ZeebeStartSyncProcess("testWorker")
    Dto syncStartWithMapping(Map<String, Object> variables);

    @ZeebeStartSyncProcess
    Dto syncStartByProcessNameWithMapping(String bpmnProcessId, Map<String, Object> variables);

    @ZeebeStartAsyncProcess
    ProcessInstanceEvent asyncStartByName(String bpmnProcessId, Map<String, Object> variables);

    @ZeebeProcessDeploy
    DeploymentEvent deploy(InputStream resourceStream, String resourceName);

    @ZeebeCancelProcessInstance
    CancelProcessInstanceResponse cancelInstance(long processInstanceKey);

    @ZeebeResolveIncident
    boolean resolveIncident(long incidentKey);

    @ZeebeCompleteCommand
    CompleteJobResponse completeJob(long jobKey);

    @ZeebeThrowError
    Void sendError(long jobKey, String errorMsg, String errorCode);

    @ZeebeThrowFail
    FailJobResponse sendFail(long jobKey, int remainingRetries, String errorMsg);
}
