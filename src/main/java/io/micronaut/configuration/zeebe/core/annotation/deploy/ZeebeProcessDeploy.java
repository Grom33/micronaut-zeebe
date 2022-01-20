package io.micronaut.configuration.zeebe.core.annotation.deploy;

import java.lang.annotation.*;

/**
 * Command to deploy process to Zeebe broker.
 * 
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeProcessDeploy {
}
