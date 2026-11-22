package io.tebex.sdk.exception;

public final class InvalidSecretKeyException extends IllegalArgumentException {
    public InvalidSecretKeyException() {
        super("The Tebex secret key is empty.");
    }

    public InvalidSecretKeyException(int position) {
        super("The Tebex secret key contains an invalid character at position " + position + ".");
    }
}
