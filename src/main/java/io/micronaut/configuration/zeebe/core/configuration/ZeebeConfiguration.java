package io.micronaut.configuration.zeebe.core.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Configuration for connection to Zeebe broker cluster
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */

@ConfigurationProperties(ZeebeConfiguration.ZEEBE_DEFAULT)
@Requires(property = ZeebeConfiguration.ZEEBE_DEFAULT)
@Requires(property = ZeebeConfiguration.ZEEBE_DEFAULT + ".enabled", notEquals = StringUtils.FALSE)
public interface ZeebeConfiguration {

    String ZEEBE_DEFAULT = "zeebe";
    String COMMAND_EXCLUDE = ".command.exclude";

    boolean isEnabled();

    /**
     * This option define that the connection should be lazy.
     * 
     * @return
     */
    Optional<Boolean> isLazyConnection();

    @Nullable
    SchemaInitConfiguration getInitSchemaConfiguration();

    /**
     * The clusterId when connecting to Camunda Cloud. Don't set this for a local
     * Zeebe Broker.
     *
     * @return the clusterId
     * @see io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1#withClusterId(String)
     */
    Optional<String> getClusterId();

    /**
     * The clientId to connect to Camunda Cloud. Don't set this for a local Zeebe
     * Broker.
     *
     * @return the the clientId
     * @see io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1.ZeebeClientCloudBuilderStep2#withClientId(String)
     */
    Optional<String> getClientId();

    /**
     * The clientSecret to connect to Camunda Cloud. Don't set this for a local
     * Zeebe Broker.
     *
     * @return the name of the clientSecret
     * @see io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1.ZeebeClientCloudBuilderStep2.ZeebeClientCloudBuilderStep3#withClientSecret(String)
     */
    Optional<String> getClientSecret();

    /**
     * The region of the cloud cluster
     *
     * @return the region where your cluster is located
     * @see io.camunda.zeebe.client.ZeebeClientCloudBuilderStep1.ZeebeClientCloudBuilderStep2.ZeebeClientCloudBuilderStep3.ZeebeClientCloudBuilderStep4#withRegion(String)
     */
    Optional<String> getRegion();

    /**
     * the default request timeout as ISO 8601 standard formatted String e.g. PT20S
     * for a timeout of 20 seconds
     *
     * @return the default request timeout
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#defaultRequestTimeout(Duration)
     */
    Optional<String> getDefaultRequestTimeout();

    /**
     * the default job poll interval in milliseconds e.g. 100 for a timeout of 100
     * milliseconds
     *
     * @return the default job poll interval
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#defaultJobPollInterval(Duration)
     */
    Optional<Long> getDefaultJobPollInterval();

    /**
     * the default job timeout as ISO 8601 standard formatted String e.g. PT5M for a
     * timeout of 5 minutes
     *
     * @return the default job timeout
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#defaultJobTimeout(Duration)
     */
    Optional<String> getDefaultJobTimeout();

    /**
     * the default message time to live as ISO 8601 standard formatted String e.g.
     * PT1H for a timeout of 1 hour
     *
     * @return the default message time to live
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#defaultMessageTimeToLive(Duration)
     */
    Optional<String> getDefaultMessageTimeToLive();

    /**
     * the default job worker name
     *
     * @return the default name of a worker
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#defaultJobWorkerName(String)
     */
    Optional<String> getDefaultJobWorkerName();

    /**
     * the gateway address to which the client should connect
     *
     * @return the gateway address
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#gatewayAddress(String)
     */
    Optional<String> getGatewayAddress();

    /**
     * the number of threads used to execute workers
     *
     * @return the count of job worker execution threads
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#numJobWorkerExecutionThreads(int)
     */
    Optional<Integer> getNumJobWorkerExecutionThreads();

    /**
     * the interval for keep allive messages to be sent as ISO 8601 standard
     * formatted String e.g. PT45S for 45 seconds
     *
     * @return the interval to send keep alive message
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#keepAlive(Duration)
     */
    Optional<String> getKeepAlive();

    /**
     * the path to a ca certificate
     *
     * @return the custom ca certificate path
     * @see io.camunda.zeebe.client.ZeebeClientBuilder#caCertificatePath(String)
     */
    Optional<String> getCaCertificatePath();

    @ConfigurationProperties("init-schema")
    interface SchemaInitConfiguration {

        Optional<Boolean> isEnabled();

        Optional<String> getClasspathFolders();

        Optional<List<String>> getSchemas();
    }
}
