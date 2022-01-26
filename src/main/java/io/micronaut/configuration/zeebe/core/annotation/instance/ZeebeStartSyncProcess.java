package io.micronaut.configuration.zeebe.core.annotation.instance;

import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * When annotated method is called, the response to the command will be received
 * after the process is completed. The response consists of a set of variables.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeStartSyncProcess {

    @AliasFor(member = "bpmnProcessId")
    String value() default "";

    @AliasFor(member = "value")
    String bpmnProcessId() default "";

    int version() default 0;
}
