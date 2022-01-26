package io.micronaut.configuration.zeebe.core.intercept.command;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.configuration.zeebe.core.annotation.deploy.ZeebeProcessDeploy;
import io.micronaut.configuration.zeebe.core.configuration.ZeebeConfiguration;
import io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager;
import io.micronaut.configuration.zeebe.core.intercept.Command;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static io.micronaut.configuration.zeebe.core.connection.ZeebeClusterConnectionManager.ZEEBE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Command to deploy new processes.
 *
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Requires(beans = ZeebeConfiguration.class)
@Requires(
        property = ZeebeConfiguration.ZEEBE_DEFAULT + ZeebeConfiguration.COMMAND_EXCLUDE + ".deploy",
        notEquals = StringUtils.TRUE)
@Singleton
public class DeployCommand implements Command<ZeebeProcessDeploy> {

    private static final Logger logger = LoggerFactory.getLogger(DeployCommand.class);
    public static final String RESOURCE_BYTES = "resourceBytes";
    public static final String RESOURCE_NAME = "resourceName";
    public static final String PROCESS_DEFINITION = "processDefinition";
    public static final String FILENAME = "filename";
    public static final String CLASSPATH_RESOURCE = "classpathResource";
    public static final String RESOURCE_STREAM = "resourceStream";
    public static final String RESOURCE_STRING = "resourceString";
    public static final String CHARSET = "charset";
    public static final String BROKER_UNUVALIABLE_MESSAGE = "Can't start process, zeebe broker isn't available!";
    private final ZeebeClusterConnectionManager connectionManager;
    private final ZeebeConfiguration configuration;
    private final ExecutorService executorService;

    @Inject
    public DeployCommand(ZeebeClusterConnectionManager connectionManager,
                         ZeebeConfiguration configuration,
                         @Named(ZEEBE) ExecutorService executorService) {
        this.connectionManager = connectionManager;
        this.configuration = configuration;
        this.executorService = executorService;
    }

    @Override
    public CompletableFuture<?> invoke(MethodInvocationContext<Object, Object> context) {
        final Argument<?>[] arguments = context.getArguments();
        final Object[] parameterValues = context.getParameterValues();

        byte[] resourceBytes = null;
        String resourceName = null;
        BpmnModelInstance processDefinition = null;
        String filename = null;
        String classpathResource = null;
        InputStream resourceStream = null;
        String resourceString = null;
        Charset charset = null;

        for (int i = 0; i < arguments.length; i++) {
            switch (arguments[i].getName()) {
                case RESOURCE_BYTES:
                    resourceBytes = (byte[]) parameterValues[i];
                    break;
                case RESOURCE_NAME:
                    resourceName = (String) parameterValues[i];
                    break;
                case PROCESS_DEFINITION:
                    processDefinition = (BpmnModelInstance) parameterValues[i];
                    break;
                case FILENAME:
                    filename = (String) parameterValues[i];
                    break;
                case CLASSPATH_RESOURCE:
                    classpathResource = (String) parameterValues[i];
                    break;
                case RESOURCE_STREAM:
                    resourceStream = (InputStream) parameterValues[i];
                    break;
                case RESOURCE_STRING:
                    resourceString = (String) parameterValues[i];
                    break;
                case CHARSET:
                    charset = (Charset) parameterValues[i];
                    break;
                default:
                    logger.debug("invoke() >> unknown argument: {} for deploy command.", arguments[i].getName());
            }
        }
        if (Objects.nonNull(resourceBytes) && StringUtils.isNotEmpty(resourceName))
            return deployByBytesArray(resourceBytes, resourceName);
        if (Objects.nonNull(processDefinition) && StringUtils.isNotEmpty(resourceName))
            return deployByProcessDefinition(processDefinition, resourceName);
        if (StringUtils.isNotEmpty(filename))
            return deployByFileName(filename);
        if (StringUtils.isNotEmpty(classpathResource))
            return deployByClassPath(classpathResource);
        if (Objects.nonNull(resourceStream) && StringUtils.isNotEmpty(resourceName))
            return deployByResourceStream(resourceStream, resourceName);
        if (StringUtils.isNotEmpty(resourceString) && Objects.nonNull(charset) && StringUtils.isNotEmpty(resourceName))
            return deployByResourceString(resourceString, charset, resourceName);
        if (StringUtils.isNotEmpty(resourceString) && StringUtils.isNotEmpty(resourceName))
            return deployByResourceStringUtf8(resourceString, resourceName);

        throw new IllegalArgumentException("Not enough arguments for deploy process!");
    }

    /**
     * Command to deploy new processes.
     *
     * @param resourceBytes the process resource as byte array
     * @param resourceName  the name of the resource (e.g. "process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByBytesArray(byte[] resourceBytes, String resourceName) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand().addResourceBytes(resourceBytes, resourceName).send().toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param processDefinition the process as model resourceName – the name of the
     *                          resource (e.g. "process.bpmn")
     * @param resourceName      the name of the resource (e.g. "process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByProcessDefinition(BpmnModelInstance processDefinition, String resourceName) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand().addProcessModel(processDefinition, resourceName)
                .send().toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param filename the absolute path of the process resource (e.g.
     *                 "~/wf/process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByFileName(String filename) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand()
                .addResourceFile(filename).send().toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param classpathResource the path of the process resource in the classpath
     *                          (e.g. "wf/process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByClassPath(String classpathResource) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand()
                .addResourceFromClasspath(classpathResource)
                .send()
                .toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param resourceStream the process resource as stream
     * @param resourceName   the name of the resource (e.g. "process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByResourceStream(InputStream resourceStream, String resourceName) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand().addResourceStream(resourceStream, resourceName)
                .send().toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param resourceString the process resource as String
     * @param charset        the charset of the String
     * @param resourceName   the name of the resource (e.g. "process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByResourceString(String resourceString, Charset charset, String resourceName) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand().addResourceString(resourceString, charset, resourceName)
                .send().toCompletableFuture();
    }

    /**
     * Command to deploy new processes.
     *
     * @param resourceString the process resource as UTF-8-encoded String
     * @param resourceName   the name of the resource (e.g. "process.bpmn")
     * @return the key of the deployed process
     */
    private CompletableFuture<DeploymentEvent> deployByResourceStringUtf8(String resourceString, String resourceName) {
        final Optional<ZeebeClient> client = connectionManager.getClient();
        if (client.isEmpty())
            throw new IllegalStateException(BROKER_UNUVALIABLE_MESSAGE);
        return client.get().newDeployCommand().addResourceStringUtf8(resourceString, resourceName)
                .send().toCompletableFuture();
    }

    @PostConstruct
    public void initSchemas() {
        logger.debug("initSchemas() >> Try to initialize embedded schemas on startup ......");
        CompletableFuture.runAsync(() -> {
            Optional<ZeebeConfiguration.SchemaInitConfiguration> initSchemaConfiguration = Optional
                    .ofNullable(configuration.getInitSchemaConfiguration());
            if (initSchemaConfiguration.isEmpty()) {
                logger.debug("initSchemas() >> Init embedded schema configuration isn't defined! Skip schema initializations.");
                return;
            }
            ZeebeConfiguration.SchemaInitConfiguration initConf = initSchemaConfiguration.get();
            if (initConf.isEnabled().isEmpty() || !initConf.isEnabled().get()) {
                logger.debug("initSchemas() >> Embedded schema initialization is disabled.");
                return;
            }
            while (connectionManager.getClient().isEmpty()) {
                try {
                    MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warn("initSchemas() >> interrupt waiting zeebe client, because: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            final String folderPrefix = initConf.getClasspathFolders().orElse("");
            initConf.getSchemas()
                    .ifPresent(schemas -> deploySchemasByList(folderPrefix, schemas));
        }, executorService);

    }

    private void deploySchemasByList(String folderPrefix, List<String> schemas) {
        schemas.stream()
                .filter(StringUtils::isNotEmpty)
                .forEach(schemaName -> {
                    try {
                        deployByClassPath(folderPrefix + "/" + schemaName)
                                .thenApplyAsync(r -> r.getProcesses().get(0))
                                .thenApplyAsync(r -> {
                                    logger.info(
                                            "initSchemas() >> Deploy embedded schema with process id: {}, version: {}, definition key: {}",
                                            r.getBpmnProcessId(), r.getVersion(), r.getProcessDefinitionKey());
                                    return r;
                                }).join();
                    } catch (Exception e) {
                        logger.error("deploySchemasByList() >> Can't deploy embedded schema with name: {}, because: {}",
                                schemaName, e.getMessage());
                    }
                });
    }
}
