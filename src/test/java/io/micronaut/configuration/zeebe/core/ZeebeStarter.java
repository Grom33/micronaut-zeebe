package io.micronaut.configuration.zeebe.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.zeebe.containers.ZeebeContainer;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
public abstract class ZeebeStarter extends Assertions {

    protected static final ZeebeContainer zeebeContainer;
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static ZeebeClient zbClnt;

    static {
        zeebeContainer = new ZeebeContainer();
        zeebeContainer.start();
    }

    protected static ZeebeClient getZeebeClient() {
        if (zbClnt == null) {
            final ZeebeClient client = ZeebeClient.newClientBuilder()
                    .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
                    .usePlaintext()
                    .defaultRequestTimeout(Duration.ofSeconds(30))
                    .build();
            zbClnt = client;
            return client;
        } else {
            return zbClnt;
        }
    }

    protected DeploymentEvent deploySchema(final String path) {
        final InputStream inputStream = ZeebeStarter.class.getClassLoader().getResourceAsStream(path);
        return getZeebeClient().newDeployCommand().addResourceStream(inputStream, "test.bpmn")
                .send().join();
    }

    protected ProcessInstanceResult startProcessInSyncMode(final String processId,
                                                           final Map<String, Object> payload) {
        return getZeebeClient()
                .newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .variables(payload)
                .withResult()
                .send()
                .join();
    }

    public List<Map<String, Object>> getListMapFromPath(String path) {
        try {
            final String data = getStringFromResources(path).orElseThrow();
            return mapper.readValue(data, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Map<String, Object> getMapFromPath(String path) {
        try {
            final String data = getStringFromResources(path).orElseThrow();
            return mapper.readValue(data, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static Optional<String> getStringFromResources(String relativePath) {
        try (InputStream inputStream = ZeebeStarter.class.getClassLoader().getResourceAsStream(relativePath)) {
            final String result = toString(inputStream, String.valueOf(StandardCharsets.UTF_8));
            return Stream.of(result.split("\n")).reduce((s1, s2) -> s1 + s2);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String toString(final InputStream input, final String encode) throws IOException {
        try {
            final StringBuilder buffer = new StringBuilder(8012);
            final InputStreamReader in = new InputStreamReader(new BufferedInputStream(input), encode);
            final char[] cbuf = new char[8012];
            int len;
            while ((len = in.read(cbuf)) != -1) {
                buffer.append(cbuf, 0, len);
            }
            return buffer.toString();
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                    // TODO
                }
            }
        }
    }

}
