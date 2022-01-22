package io.micronaut.configuration.zeebe.core.mock;

public interface TestData {

    Object getTestData(String workerName);

    void drainTestData();
}
