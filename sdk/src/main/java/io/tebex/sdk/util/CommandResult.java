package io.tebex.sdk.util;

import com.google.gson.JsonArray;

public class CommandResult {
    private final boolean isSuccess;
    private String message = "";
    private Throwable exception = null;

    private CommandResult(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public static CommandResult from(boolean isSuccess) {
        CommandResult result = new CommandResult(isSuccess);
        return result;
    }

    public CommandResult withMessage(String message) {
        this.message = message == null ? "" : message;
        return this;
    }

    public CommandResult withException(Throwable e) {
        this.exception = e;
        return this;
    }

    public boolean getIsSuccess() {
        return this.isSuccess;
    }

    public Throwable getException() {
        return this.exception;
    }

    public String getMessage() {
        return this.message == null ? "" : this.message;
    }
}
