package io.micronaut.configuration.zeebe.core.annotation.variable;

import java.lang.annotation.*;

/**
 * Command to set and/or update the variables of a given flow element (e.g.
 * process instance, task, etc.)
 * <p>
 * 1. elementInstanceKey - The key of the element instance to set/update the
 * variables for 2. variables - The variables document as map 3. local - Whether
 * or not to update only the local scope. If true, the variables will be merged
 * strictly into the local scope (as indicated by elementInstanceKey); this
 * means the variables is not propagated to upper scopes. For example, let's say
 * we have two scopes, '1' and '2', with each having effective variables as: 1
 * `{ "foo" : 2 }` 2 `{ "bar" : 1 }` If we send an update request with
 * elementInstanceKey = 2, a new document of `{ "foo" : 5 }`, and local is true,
 * then scope 1 will be unchanged, and scope 2 will now be `{ "bar" : 1, "foo" 5
 * }`. If local was false, however, then scope 1 would be `{ "foo": 5 }`, and
 * scope 2 would be `{ "bar" : 1 }`.
 * <p>
 * Return void or SetVariablesResponse
 * <p>
 * io.camunda.zeebe.client.api.response.SetVariablesResponse;
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeSetVariables {

    boolean local() default false;
}
