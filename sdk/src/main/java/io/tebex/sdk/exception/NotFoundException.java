package io.tebex.sdk.exception;

/**
 * Represents an exception thrown when a requested item is not found.
 */
public class NotFoundException extends Throwable {

    /**
     * Returns the error message associated with the exception.
     *
     * @return The error message.
     */
    @Override
    public String getMessage() {
        return "That does not exist!";
    }
}