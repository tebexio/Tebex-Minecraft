package io.tebex.minecraft.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommandResult {
  private final boolean isSuccess;
  private String message = "";
  private Throwable exception = null;

  public static CommandResult from(boolean isSuccess) {
    return new CommandResult(isSuccess);
  }

  public CommandResult withMessage(String message) {
    this.message = message == null ? "" : message;
    return this;
  }

  public CommandResult withException(Throwable e) {
    this.exception = e;
    return this;
  }
}
