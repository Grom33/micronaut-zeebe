package io.micronaut.configuration.zeebe.core.specification;

import java.util.List;

/**
 * Worker specification that can be useful for public API about workers of service
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class WorkerSpecification {

    // Worker name
    private String typeName;
    private String documentation;
    // headers for worker
    private List<HeaderSpecification> headers;
    // variables for worker
    private List<VariableSpecification> variables;
    // error that worker can throw
    private List<ErrorSpecification> errors;
    // variable that worker publishes to business process context
    private VariableSpecification output;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<HeaderSpecification> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeaderSpecification> headers) {
        this.headers = headers;
    }

    public List<VariableSpecification> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableSpecification> variables) {
        this.variables = variables;
    }

    public VariableSpecification getOutput() {
        return output;
    }

    public void setOutput(VariableSpecification output) {
        this.output = output;
    }

    public List<ErrorSpecification> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorSpecification> errors) {
        this.errors = errors;
    }
}
