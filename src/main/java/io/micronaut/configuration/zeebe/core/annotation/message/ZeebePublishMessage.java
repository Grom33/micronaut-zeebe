package io.micronaut.configuration.zeebe.core.annotation.message;

import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Command to publish a message which can be correlated to a process instance.
 * You should define params in annotated method: - correlationKey: Set the value
 * of the correlation key of the message. This value will be used together with
 * the message name to find matching message subscriptions. - variables: Set the
 * variables of the message.
 * <p>
 * Annotated method can return void or PublishMessageResponse
 *
 * @author Gromov Vitaly.
 * @see io.camunda.zeebe.client.api.response.PublishMessageResponse
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebePublishMessage {

    /**
     * the name of the message
     */
    @AliasFor(member = "messageName")
    String value() default "";

    /**
     * Set the time-to-live of the message. The message can only be correlated
     * within the given time-to-live. If the duration is zero or negative then the
     * message can only be correlated to open subscriptions (e.g. to an entered
     * message catch event).
     */
    String timeToLive() default "0";
}
