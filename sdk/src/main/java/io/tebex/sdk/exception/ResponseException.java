package io.tebex.sdk.exception;

public class ResponseException extends Throwable {
    private final String message;

    public ResponseException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
