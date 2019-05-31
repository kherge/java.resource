package io.herrera.kevin.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the resource exception class functions as intended.
 */
public class ResourceExceptionTest {

    /**
     * Verify that the exception may only have a message.
     */
    @Test
    public void constructorMessageTest() {
        String message = "The exception message.";

        ResourceException exception = new ResourceException(message);

        assertEquals(message, exception.getMessage());
    }

    /**
     * Verify that the exception may have a message and cause.
     */
    @Test
    public void constructorMessageCauseTest() {
        Exception cause = new Exception();
        String message = "The exception message.";

        ResourceException exception = new ResourceException(message, cause);

        assertSame(cause, exception.getCause());
        assertEquals(message, exception.getMessage());
    }
}
