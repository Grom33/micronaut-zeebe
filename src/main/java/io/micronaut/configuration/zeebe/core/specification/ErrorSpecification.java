package io.micronaut.configuration.zeebe.core.specification;

import java.util.List;

/**
 * specification error that a worker can throw
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class ErrorSpecification {

    // Error code
    private String code;
    // List of exceptions class name
    private List<String> causeExceptions;
    private String documentation;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getCauseExceptions() {
        return causeExceptions;
    }

    public void setCauseExceptions(List<String> causeExceptions) {
        this.causeExceptions = causeExceptions;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
