package io.micronaut.configuration.zeebe.core.connection.event;

/**
 * @author Gromov Vitaly.
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
