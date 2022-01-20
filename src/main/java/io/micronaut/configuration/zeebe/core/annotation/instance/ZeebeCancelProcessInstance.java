package io.micronaut.configuration.zeebe.core.annotation.instance;

import java.lang.annotation.*;

/**
 * Command to cancel a process instance. 1.processInstanceKey the key which
 * identifies the corresponding process instance. Return: void or
 * CancelProcessInstanceResponse
 *
 * io.camunda.zeebe.client.api.response.CancelProcessInstanceResponse;
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeCancelProcessInstance {
}
