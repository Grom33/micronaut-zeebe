package io.micronaut.configuration.zeebe.core.annotation.instance;

import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Command to create/start a new instance of a process.
 * <p>
 * 1. bpmnProcessId: Set the BPMN process id of the process to create an
 * instance of. This is the static id of the process in the BPMN XML (i.e.
 * "bpmn:process id='my-process'"). 2. variables: Set the initial variables of
 * the process instance. 3. version: Set the version of the process to create an
 * instance of. The version is assigned by the broker while deploying the
 * process. It can be picked from the deployment or process event.
 * <p>
 * Annotated method can return void or ProcessInstanceEvent
 *
 * io.camunda.zeebe.client.api.response.ProcessInstanceEvent
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeStartAsyncProcess {

    @AliasFor(member = "bpmnProcessId")
    String value() default "";

    /**
     * Set the BPMN process id of the process to create an instance of. This is the
     * static id of the process in the BPMN XML (i.e. "bpmn:process
     * id='my-process'").
     */
    @AliasFor(member = "value")
    String bpmnProcessId() default "";

    /**
     * Set the version of the process to create an instance of. The version is
     * assigned by the broker while deploying the process. It can be picked from the
     * deployment or process event
     */
    int version() default 0;
}
