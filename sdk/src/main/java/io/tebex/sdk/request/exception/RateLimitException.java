package io.tebex.sdk.request.exception;

public class RateLimitException extends Throwable {
    private final String message;

    public RateLimitException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
