package io.tebex.minecraft.runtime;

/** Monotonic-enough wall clock used to coordinate polling and delayed delivery. */
interface RuntimeClock {
  long millis();
}
