package io.micronaut.configuration.zeebe.core.annotation.incendent;

import java.lang.annotation.*;

/**
 * Command to report a business error (i.e. non-technical) that occurs while
 * processing a job. The error is handled in the process by an error catch
 * event. If there is no error catch event with the specified errorCode then an
 * incident will be raised instead.
 * <p>
 * 1. jobKey - the key which identifies the job 2. errorCode - the errorCode
 * that will be matched against an error catch event. If the errorCode can't be
 * matched to an error catch event in the process, an incident will be created
 * 3. errorMsg - error message. Provide an error message describing the reason
 * for the non-technical error. If the error is not caught by an error catch
 * event, this message will be a part
 * <p>
 * Return Void
 * 
 * @author Gromov Vitaly.
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Inherited
public @interface ZeebeThrowError {

}
