package io.micronaut.configuration.zeebe.core.mock;

import io.micronaut.configuration.zeebe.core.annotation.job.ZeebeWorker;
import jakarta.inject.Singleton;

/**
 * @author : Vitaly Gromov
 * @since : 1.0.0
 **/
@SuppressWarnings("java:S2925")
@Singleton
public class SleepWorker {

    private long jobKey;

    @ZeebeWorker("sleepWorker")
    public int sleep(final long key) {
        jobKey = key;
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getJobKey() {
        return jobKey;
    }

    public void erase() {
        jobKey = 0;
    }
}
