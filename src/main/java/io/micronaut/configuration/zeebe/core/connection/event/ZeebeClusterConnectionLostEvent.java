package io.micronaut.configuration.zeebe.core.connection.event;

/**
 * Zeebe cluster lost connection event
 * <p>
 * This event will be sent to the Micronaut event bus if the connection with the
 * Zeebe broker is lost.
 * 
 * @author Gromov Vitaly.
 * @see io.micronaut.context.event.ApplicationEventPublisher
 * @since 1.0.0
 */
@SuppressWarnings("java:S2094")
public class ZeebeClusterConnectionLostEvent {

}
