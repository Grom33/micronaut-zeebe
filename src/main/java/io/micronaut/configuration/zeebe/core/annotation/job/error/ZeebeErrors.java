package io.micronaut.configuration.zeebe.core.annotation.job.error;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Target({ METHOD, TYPE, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ZeebeErrors {

    ZeebeError[] value() default {};
}
