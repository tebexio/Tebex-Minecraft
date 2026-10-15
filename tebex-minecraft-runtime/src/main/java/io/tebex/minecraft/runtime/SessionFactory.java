package io.tebex.minecraft.runtime;

/** Creates one authenticated SDK lifetime for a runtime connection attempt. */
interface SessionFactory {
  SdkSession create(String secretKey);
}
