package io.tebex.minecraft.shop;

import java.util.HashMap;
import java.util.Map;

/** An action is consumed once; outside/player inventory slots never trigger shop navigation. */
public final class MenuActions {
  private final int size;
  private final Map<Integer, Runnable> actions;
  private boolean consumed;

  public MenuActions(int size, Map<Integer, Runnable> actions) {
    this.size = size;
    this.actions = new HashMap<>(actions);
  }

  public void select(int slot) {
    if (consumed || slot < 0 || slot >= size) return;
    Runnable action = actions.get(slot);
    if (action != null) {
      consumed = true;
      action.run();
    }
  }
}
