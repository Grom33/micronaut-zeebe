package io.micronaut.configuration.zeebe.core.annotation.job;

import io.micronaut.configuration.zeebe.core.annotation.job.error.ZeebeError;
import io.micronaut.context.annotation.AliasFor;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.*;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Executable(processOnStartup = true)
@Bindable
@Inherited
public @interface ZeebeWorker {

    @AliasFor(member = "type")
    String value() default "";

    @AliasFor(member = "value")
    String type() default "";

    String timeout() default "5m";

    int maxJobsToActivate() default 32;

    long pollInterval() default 100;

    String requestTimeout() default "10s";

    String[] fetchVariables() default {};

    ZeebeError[] errors() default {};

    String outputVariableName() default "";

    boolean autoComplete() default true;

    String documentation() default "";

}
