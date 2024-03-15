package io.tebex.sdk.analytics.exception;

/**
 * Represents an exception thrown when the server hasn't properly set up Analytics.
 */
public class ServerNotSetupException extends Throwable {

    /**
     * Returns the error message associated with the exception.
     *
     * @return The error message.
     */
    @Override
    public String getMessage() {
        return "Analytics not setup!";
    }
}