# micronaut-zeebe

Integration between Micronaut and Zeebe

## Introduction
Zeebe is a workflow engine from Camunda, designed to meet the scalability requirements of high-performance applications running on cloud-native software architectures and to support workflows that span multiple microservices in low latency, high-throughput scenarios.
Micronaut features dedicated support for defining zeebe workers and send commands to zeebe broker.

## Quick Start
To add support for zeebe to an existing project, you should first add the Micronaut Zeebe configuration to your build configuration. For example in Gradle:
```groovy
dependencies {
    compile 'com.github.grom33:micronaut-zeebe:1.0.0'
}
```
and you configure application.yaml 
```yaml
zeebe:
  enabled: true
  gateway-address: localhost:26500
```
## Zeebe worker

To connect the worker, you need to add an annotation over the method to which the zeebe call will be addressed
An example, a simple worker that performs some actions, closes the task but does not return any data to the process context:
```java
 @ZeebeWorker("test-worker-simple")
    public void simpleWorker(@ZeebeContextVariable("valueX") String valueX) {
        log.debug("simpleWorker() >> valueX: {}", valueX);
    }
```
Or the worker is more complicated, takes various data from the process, works with some application layers, and returns the data to the business process context:
```java
@ZeebeWorker(type = "reactive-test-worker-complex", outputVariableName = "testResult")
    public Mono<Document> writeHistory(final Long processInstanceKey,
                                         String bpmnProcessId,
                                         @ZeebeHeader("header1") String header1,
                                         @Nullable @ZeebeContextVariable("valueX") String valueX,
                                         @ZeebeContextVariable("userContext") Map<String, Object> usrCtx,
                                         @ZeebeContextMapper SomeA someDto,
                                         @ZeebeContextMapper SomeB secondDto) {
        Storage<Document> storage = storageRegistry.findOrThrow(HISTORY);
        History history = new History();
        history.setFoo(header1);
        history.setBar(valueX);
        return storage.save(history);
    }
```

### Retrieving data from the business process context

To retrieve data from a process context, there are the following annotations:
- **@ZeebeHeader("header1")** - Retrieves a value from the header of the service task with name `header1`;
- **@ZeebeContextVariable("valueX")** - Retrieves the value of a variable from the process context with name `valueX`;
- **@ZeebeContextMapper** - Converts the data of business process context to a Dto, the type of which is specified by the annotated method field. You can also specify the path to an object that is nested in another object `@ZeebeContextMapper("value1.inner")`

When requesting data from the process context, a list of fetchVariables is formed, a set of variables that need to be requested from the business process context, while the fields of the dto class using `@ZeebeContextMapper` will also be included in this list.
This is done in order to save traffic and memory, so as not to download the entire business process context into the worker call.
If for some reason additional fields are needed, they can be specified in the `@ZeebeWorker` annotation in the `fetchVariables` attribute.
### Available system data

The following data is available and can be passed to the method:
- `Long key` - job key;
- `Map<String, Object> variables` - all fetched variables;
- `Map<String, String>` headers - all service task headers;
- `Long processInstanceKey` - business process instance key;
- `String bpmnProcessId` - business process name (id);
- `int processDefinitionVersion` - business process version;
- `long processDefinitionKey` - the key of the deployed version of the business process;
- `String elementId` - name of service task;
- `long elementInstanceKey` - service task instance key;
- `int retries` - amount of attempts left;
- `long deadline` - time in unix timestamp before which this task will be transferred to another worker, because it will be considered overdue.

example:
```java
    @ZeebeWorker("simple_worker")
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
                                      long deadline) {
        ......
        }
```
!!! **Optional arguments must be annotated with `@Nullable`**. If any data not marked nullable is missing, an exception will be thrown.
A worker method can return values both synchronously and asynchronous/reactive.

### Worker settings
The worker is configured through an annotation, an example:
```java
@ZeebeWorker(
            type = "test-worker", // 
            timeout = "10m", // Task execution timeout by this worker
            maxJobsToActivate = 50, // The maximum number of tasks that this worker can simultaneously accept
            pollInterval = 50, // Broker polling interval for tasks
            requestTimeout = "20s", // Broker request timeout
            errors = {  // configuring exception handling
                    @ZeebeError(code = "0", throwable = {   // You can define which Exceptions will generate 
                            IllegalArgumentException.class, // an error in business process with the specified code.
                            IllegalStateException.class     // Inheritance is also supported.
                    })
            },
            outputVariableName = "result", // The name of the variable in the context of the business process, which will contain the result of the worker execution
            fetchVariables = {"value1", "value2"}, // Additional variable names to be obtained from the business process context
            documentation = "The worker does a very important job......" // Brief description of the worker. The field is for documentation
    )
    public CompletableFuture<String> handle(final Map<String, Object> variables){
        ...
        }
```
If it is necessary to pass the error code to the business process in case of exceptions during the execution of the work, it is necessary to use an annotation `@ZeebeError` in parameter `errors`. 
In the annotation, you must specify the error code and list the exceptions that match. 
Exception inheritance is supported, you can specify the exception superclass. 
But you need to be careful, because the super class can override other classes lying lower in the inheritance hierarchy. 
You can specify many annotations `@ZeebeError`.

## Zeebe command
Zeebe has many commands in API. These commands can be executed through methods with special annotations.
You need to create an interface and mark it with the `@ZeebeClient` annotation, add the necessary methods and mark them with the appropriate annotations.
The following commands are supported:

#### @ZeebePublishMessage
Command to publish a message which can be correlated to a process instance. Example:
```java
    @ZeebePublishMessage("someMassageName")
    void send(String correlationKey, Map<String, Object> variables);
```
Or
```java
    @ZeebePublishMessage
    void send(String messageName, String correlationKey, Map<String, Object> variables);
```
Or
```java
    @ZeebePublishMessage
    void send(String messageName, String correlationKey, String messageId, Map<String, Object> variables);
```
parameters:
- `String messageName` - the name of the message. Message name can be specified also in annotation `@ZeebePublishMessage("someMassageName")`;
- `String correlationKey` (optional) - the value of the correlation key of the message. This value will be used together with the message name to find matching message subscriptions;
- `String messageId` (optional) - the id of the message. The message is rejected if another message is already published with the same id, name and correlation-key;
- `Map<String, Object> variables` - the variables of the message;

return value:
- `void` - return nothing;
- `long` - the record key of the message that was published;


#### @ZeebeStartAsyncProcess
Asynchronous start of a business process, example:
```java
    @ZeebeStartAsyncProcess("SomeProcessName")
    ProcessInstanceEvent asyncStart(Map<String, Object> variables);
```
Or
```java
    @ZeebeStartAsyncProcess
    void anotherAsyncStart(String bpmnProcessId, Map<String, Object> variables);
```
Or
```java
    @ZeebeStartAsyncProcess
    ProcessInstanceEvent anotherAsyncStart(String bpmnProcessId, String version, Map<String, Object> variables);
```
parameters:
- `String bpmnProcessId` - the BPMN process id of the process to create an instance of. This is the static id of the process in the BPMN XML `(i.e. "<bpmn:process id='my-process'>")`
- `String version` (optional) - the version of the process to create an instance of. The version is assigned by the broker while deploying the process. It can be picked from the deployment or process event.
- `Map<String, Object> variables` (optional) - the initial variables of the process instance

return value:
- `void` - return nothing;
- `ProcessInstanceEvent` - class contains data of created instance process;

#### @ZeebeStartSyncProcess
Command to create/start a new instance of a process When this method is called, the response to the command will be received after the process is
completed. The response consists of a set of variables. Example:
```java
    @ZeebeStartSyncProcess("testWorker")
    Map<String, Object> syncStart(Map<String, Object> variables);
```
Or with result serialization in to Dto
```java
    @ZeebeStartSyncProcess("testWorker")
    Dto syncStartWithMapping(Map<String, Object> variables);
```

parameters:
- `String bpmnProcessId` - the BPMN process id of the process to create an instance of. This is the static id of the process in the BPMN XML `(i.e. "<bpmn:process id='my-process'>")`
- `String version` (optional) - the version of the process to create an instance of. The version is assigned by the broker while deploying the process. It can be picked from the deployment or process event.
- `Map<String, Object> variables` (optional) - the initial variables of the process instance.

return value:
- `void` - return nothing;
- `ProcessInstanceResult` - class contains result of execution of business process;
- `Dto` - the result can be mapped to a class that you define.

#### @ZeebeCancelProcessInstance
Stops the execution of a business process in Zeebe, example:
```java
    @ZeebeCancelProcessInstance
    CancelProcessInstanceResponse cancelProcessInstance(long processInstanceKey);
```
parameters:
- `long processInstanceKey` - the key which identifies the corresponding process instance.

return value:
- `void` - return nothing;
- `CancelProcessInstanceResponse` - class that indicate of canceling of business process instance;

#### @ZeebeResolveIncident
Resolves an incident that has occurred, for example:
```java
    @ZeebeResolveIncident
    ResolveIncidentResponse resolveIncident(long incidentKey);
```
parameters:
- `long incidentKey` - the key of the corresponding incident

return value:
- `void` - return nothing;
- `ResolveIncidentResponse` - the class that indicate of resolve an incident

#### @ZeebeThrowError
Command to report a business error (i.e. non-technical) that occurs while
processing a job. The error is handled in the process by an error catch
event. If there is no error catch event with the specified errorCode then an
incident will be raised instead. Example:
```java
    @ZeebeThrowError
    void throwError(long jobKey, String errorCode, String errorMsg);
```
parameters:
- `long jobKey` - the key which identifies the job;
- `String errorCode` - the errorCode that will be matched against an error catch event. If the errorCode can't be matched to an error catch event in the process, an incident will be created
- `String errorMsg` -  error message. Provide an error message describing the reason for the non-technical error. If the error is not caught by an error catch event, this message will be a part

return value:
- `void` - return nothing;

#### @ZeebeThrowFail
Command to mark a job as failed. If the given retries are greater than zero
then this job will be picked up again by a job subscription. Otherwise, an incident is created for this job.
Example:
```java
    @ZeebeThrowFail
    FailJobResponse throwFail(long jobKey, int remainingRetries, String errorMsg);
```
parameters:
- `long jobKey` - The key which identifies the job.
- `int remainingRetries` - The remaining retries of this job. If the retries are greater than zero then this job will be picked up again by a job subscription. Otherwise, an incident is created for this job.
- `String errorMsg` - Error message to be attached to the failed job. Provide an error message describing the reason for the job failure. If failing the job creates an incident, this error message will be used as incident message.
  
return value:
- `void` - return nothing;

#### @ZeebeUpdateRetries
Command to update the retries of a job. If the given retries are greater than zero then this job will be picked up again by a job worker. This will not
close a related incident, which still has to be marked as resolved with newResolveIncidentCommand(long incidentKey).
```java
    @ZeebeUpdateRetries
    UpdateRetriesJobResponse updateRetries(long jobKey, int retries);
```
parameters:
- `long jobKey` - the key of the job to update.
- `int retries` - the retries of this job.
  
return value:
- `void` - return nothing;

#### @ZeebeSetVariables
Command to set and/or update the variables of a given flow element (e.g. process instance, task, etc.)
```java
    @ZeebeSetVariables
    SetVariablesResponse setVariables(long elementInstanceKey, Map<String, Object> variables);
```

parameters:
- `long elementInstanceKey` - The key of the element instance to set/update the variables for.
- `Map<String, Object> variables` - The variables document as map.
- `boolean local`  (set in annotation: `@ZeebeSetVariables(local = true)`) - Whether or not to update only the local scope. 
If true, the variables will be merged strictly into the local scope (as indicated by
elementInstanceKey); this means the variables is not propagated to upper scopes. For example, let's
say we have two scopes, '1' and '2', with each having effective variables as: 1 => `{ "foo" : 2 }` 
2 => `{ "bar" : 1 }` If we send an update request with elementInstanceKey = 2, 
a new document of `{"foo" : 5 }`, and local is true, then scope 1 will be unchanged, 
and scope 2 will now be `{ "bar" : 1,"foo" 5 }`. If local was false, however, then scope
1 would be `{ "foo": 5 }`, and scope 2 would be `{"bar" : 1 }`

return value:
- `void` - return nothing;
- `SetVariablesResponse` - clasa contains key of the set variables command

#### @ZeebeCompleteCommand
Command to complete a job. If the job is linked to a process instance then this command will complete
the related activity and continue the flow.
```java
    @ZeebeCompleteCommand
    SetVariablesResponse completeCommand(long jobKey, Map<String, Object> variables);
```
parameters:
- `long jobKey` - the key which identifies the job
- `Map<String, Object> variables` - the variables as map

return value:
- `void` - return nothing;

#### @ZeebeProcessDeploy
Command to deploy new processes.
```java
    @ZeebeProcessDeploy
    SetVariablesResponse processDeploy(byte[] resourceBytes, String resourceName);
```
parameters:
- `byte[] resourceBytes` - the process resource as byte array
- `String resourceName` - the name of the resource (e.g. "process.bpmn")

return value:
- `void` - return nothing;
- `DeploymentEvent` - class contains data of the unique key of the deployment and the list of the processes which are deployed

## Initializing business process diagrams from project resources
To initialize business process diagrams from service resources, use the following setting
in `application.yaml`:
```yaml
zeebe:
  schema:
    enbled: true
    classpath-folders: bpmn # The name of the directory where business process diagrams are located
    schemas: # List of schemes to be deployed
      - test-worker.cloud-bpmn
```

## Health check
By default, the health check is disabled; to enable it, you must specify the following in the settings
in `application.yaml`:
```yaml
zeebe:
  health:
    enabled: true
```
## Connection management
Other configuration can be also specified, for self-hosted broker:
```yaml
zeebe:
  enabled: true
  gateway-address: localhost:26500 ## the gateway address to which the client should connect
  default-request-timeout: PT20S ## the default request timeout as ISO 8601 standard formatted String e.g. PT20S for a timeout of 20 seconds
  default-job-poll-interval: 100 ## the default job poll interval in milliseconds e.g. 100 for a timeout of 100 milliseconds
  default-job-timeout: PT5M ## the default job timeout as ISO 8601 standard formatted String e.g. PT5M for a timeout of 5 minutes
  default-message-time-to-live: PT1H ## the default message time to live as ISO 8601 standard formatted String e.g. PT1H for a timeout of 1 hour
  default-job-worker-name: simple ## the default job worker name
  num-job-worker-execution-threads: 8 ## the number of threads used to execute workers
  keep-alive: PT45S ## the interval for keep allive messages to be sent as ISO 8601 standard formatted String e.g. PT45S for 45 seconds
  ca-certificate-path: path ## the path to a ca certificate
```
and cloud broker:
```yaml
zeebe:
  .....
  cluster-id: CLOUDID ## The clusterId when connecting to Camunda Cloud. Don't set this for a local Zeebe Broker.
  client-id: CLIENTID ## The clientId to connect to Camunda Cloud. Don't set this for a local Zeebe Broker.
  client-secret: SECRET ## The clientSecret to connect to Camunda Cloud. Don't set this for a local Zeebe Broker.
```

the module constantly monitors the status of the connection to the cluster, including its status. 
In the case of a non-consistent state of the broker, or problems with the connection, the library 
disables the workers, and in case of restoration of the broker's functionality, it connects the 
workers back.
All events connected or disconnected to the broker are notified by sending an event to the micronaut
event bus. 
- `ZeebeClusterConnectionEstablishedEvent` - Zeebe cluster connection setup event.
- `ZeebeClusterConnectionLostEvent` - Zeebe cluster lost connection event.

The module supports lazy connection. For those cases when you want to control the event of connecting
an application to a broker. To enble lazy connection you must specify the following in the settings
in `application.yaml`:
```yaml
zeebe:
  lazy-connection: true
```
In this case, in order to force the module to connect to the broker, it is necessary to send an event 
to the micronaut bus. 
```java
applicationEventPublisher.publishEvent(new ZeebeConnectionSignal(true))
```

## License

This project licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.