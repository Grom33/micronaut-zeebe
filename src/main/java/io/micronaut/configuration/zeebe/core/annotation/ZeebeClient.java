package io.micronaut.configuration.zeebe.core.annotation;

import io.micronaut.aop.Introduction;
import jakarta.inject.Singleton;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Introduction
@Singleton
public @interface ZeebeClient {
}
