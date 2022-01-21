package io.micronaut.configuration.zeebe.core.specification;

/**
 * the header specification that the worker needs
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class HeaderSpecification {

    // header name
    private String headerName;
    // header mandatory
    private boolean required;
    private String documentation;

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
