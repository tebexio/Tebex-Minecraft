package io.tebex.minecraft.shop;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MenuActionsTest {
  @Test
  void ignoresOutsideAndPlayerInventorySlotsAndConsumesActionOnce() {
    AtomicInteger calls = new AtomicInteger();
    MenuActions menu = new MenuActions(9, Collections.singletonMap(2, calls::incrementAndGet));
    for (int slot : new int[] {-999, -1, 9, 36, 0}) menu.select(slot);
    assertEquals(0, calls.get());
    menu.select(2);
    menu.select(2);
    assertEquals(1, calls.get());
  }
}
