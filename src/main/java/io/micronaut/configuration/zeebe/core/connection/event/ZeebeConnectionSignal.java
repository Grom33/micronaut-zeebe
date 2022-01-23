package io.micronaut.configuration.zeebe.core.connection.event;

/**
 * Zeebe cluster connection management event.
 * <p>
 * The event is sent to the Micronaut event bus, if the enabled attribute is set
 * to true then the connection will be established, if it is false then the
 * connection will be terminated.
 *
 * @author Gromov Vitaly.
 * @see io.micronaut.context.event.ApplicationEventPublisher
 * @since 0.1.0
 */
public class ZeebeConnectionSignal {

    private final boolean enabled;

    public ZeebeConnectionSignal(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
