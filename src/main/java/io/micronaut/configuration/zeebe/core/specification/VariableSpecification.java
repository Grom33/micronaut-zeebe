package io.micronaut.configuration.zeebe.core.specification;

/**
 * Variable specification for worker
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class VariableSpecification {

    // Variable name in business process
    private String variableName;
    private boolean required;
    // Basic type of variable, like int, string, bool, map.
    private String type;
    // java class to which the variable will be converted
    private String mappedType;
    private String documentation;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMappedType() {
        return mappedType;
    }

    public void setMappedType(String mappedType) {
        this.mappedType = mappedType;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
