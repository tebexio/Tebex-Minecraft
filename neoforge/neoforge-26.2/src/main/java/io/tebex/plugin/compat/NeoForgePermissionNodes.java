package io.tebex.plugin.compat;

import io.tebex.minecraft.commands.PermissionDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/** Only the native permission API binding lives here; names/defaults are shared. */
public final class NeoForgePermissionNodes {
  private static final Map<String, PermissionNode<Boolean>> NODES = new LinkedHashMap<>();

  static {
    PermissionDefaults.nodes(true)
        .forEach(
            (name, publicAccess) ->
                NODES.put(
                    "tebex." + name,
                    new PermissionNode<>(
                        "tebex",
                        name,
                        PermissionTypes.BOOLEAN,
                        (player, uuid, context) ->
                            publicAccess || NativeVersion.hasAdminPermission(player))));
  }

  private NeoForgePermissionNodes() {}

  public static void register(PermissionGatherEvent.Nodes event) {
    event.addNodes(NODES.values().toArray(new PermissionNode<?>[0]));
  }

  public static boolean hasPermission(ServerPlayer player, String permission) {
    PermissionNode<Boolean> node = NODES.get(permission);
    return node == null
        ? NativeVersion.hasAdminPermission(player)
        : PermissionAPI.getPermission(player, node);
  }
}
