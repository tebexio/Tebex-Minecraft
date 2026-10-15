package io.tebex.plugin.compat;

import io.tebex.minecraft.commands.PermissionDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

/** Only the native permission API binding lives here; names/defaults are shared. */
public final class ForgePermissionNodes {
  private static final Map<String, PermissionNode<Boolean>> NODES = new LinkedHashMap<>();

  static {
    PermissionDefaults.nodes(false)
        .forEach(
            (name, publicAccess) ->
                NODES.put(
                    "tebex." + name,
                    new PermissionNode<>(
                        "tebex",
                        name,
                        PermissionTypes.BOOLEAN,
                        (player, uuid, context) -> publicAccess || NativeVersion.isOp(player))));
  }

  private ForgePermissionNodes() {}

  public static void register(PermissionGatherEvent.Nodes event) {
    event.addNodes(NODES.values().toArray(new PermissionNode<?>[0]));
  }

  public static boolean hasPermission(ServerPlayer player, String permission) {
    PermissionNode<Boolean> node = NODES.get(permission);
    return node == null ? NativeVersion.isOp(player) : PermissionAPI.getPermission(player, node);
  }
}
