package io.micronaut.configuration.zeebe.core.configuration;

import io.camunda.zeebe.client.api.worker.JobHandler;
import io.micronaut.core.util.Toggleable;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Class define worker configuration.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public class WorkerConfiguration implements Toggleable, Serializable {

    // The nam of worker
    private String type;
    private boolean enabled;
    private Duration timeout;
    private Integer maxJobsToActivate;
    private Duration pollInterval;
    private Duration requestTimeout;
    // List of variables that need to be fetched to job
    private List<String> fetchVariables;
    // Exception map, in case of an exception, an error with the corresponding code
    // will be transferred to the business process.
    private Map<String, List<Class<? extends Throwable>>> errors;
    // Specifies the name of the variable in the context of the business process, in
    // which the result of the job execution will be placed.
    private String outputVariableName;
    // Use for auto complete job.
    private boolean autoComplete;
    // Исполнитель работы
    private transient JobHandler handler;

    public JobHandler getHandler() {
        return handler;
    }

    public void setHandler(JobHandler handler) {
        this.handler = handler;
    }

    public String getOutputVariableName() {
        return outputVariableName;
    }

    public void setOutputVariableName(String outputVariableName) {
        this.outputVariableName = outputVariableName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxJobsToActivate() {
        return maxJobsToActivate;
    }

    public void setMaxJobsToActivate(Integer maxJobsToActivate) {
        this.maxJobsToActivate = maxJobsToActivate;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public List<String> getFetchVariables() {
        return fetchVariables;
    }

    public void setFetchVariables(List<String> fetchVariables) {
        this.fetchVariables = fetchVariables;
    }

    public Map<String, List<Class<? extends Throwable>>> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, List<Class<? extends Throwable>>> errors) {
        this.errors = errors;
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }
}
