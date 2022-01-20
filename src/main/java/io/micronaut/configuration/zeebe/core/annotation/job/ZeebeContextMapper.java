package io.micronaut.configuration.zeebe.core.annotation.job;

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.*;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Bindable
@Inherited
public @interface ZeebeContextMapper {

    @AliasFor(member = "path")
    String value() default "";

    @AliasFor(member = "value")
    String path() default "";

    String documentation() default "";
}
