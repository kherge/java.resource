package io.herrera.kevin.resource;

/**
 * An exception that is thrown for a resource related error.
 */
public class ResourceException extends RuntimeException {

    ResourceException(String message) {
        super(message);
    }

    ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
