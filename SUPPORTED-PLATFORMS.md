# Supported platforms

Every distributable target is a visible, self-contained Gradle project. Its directory owns the loader entrypoint and local `PlatformHost` implementation, plus native lifecycle, scheduling, players, permissions, commands, events, and GUI/container code.

| Project | Source directory | Supported target | Minimum Java | Entrypoint | Local adapter |
| --- | --- | --- | --- | --- | --- |
| `bukkit` | `bukkit/` | Bukkit/Spigot 1.8.8+ API baseline | 8 | `TebexBukkitPlugin` | `BukkitPlatform` |
| `folia` | `folia/` | Paper/Folia 1.21.4 | 21 | `TebexFoliaPlugin` | `FoliaPlatform` |
| `bungeecord` | `bungeecord/` | BungeeCord/Waterfall 1.18+ | 8 | `TebexBungeePlugin` | `BungeePlatform` |
| `velocity` | `velocity/` | Velocity 3.3+ | 17 | `TebexVelocityPlugin` | `VelocityPlatform` |
| `fabric-26.1` | `fabric/fabric-26.1/` | Minecraft 26.1, Fabric Loader 0.18.4 | 25 | `TebexFabricPlugin` | `FabricPlatform` |
| `fabric-26.2` | `fabric/fabric-26.2/` | Minecraft 26.2, Fabric Loader 0.19.3 | 25 | `TebexFabricPlugin` | `FabricPlatform` |
| `forge-1.20.1` | `forge/forge-1.20.1/` | Minecraft 1.20.1, Forge 47.4.20 | 17 | `TebexForgePlugin` | `ForgePlatform` |
| `forge-1.21.1` | `forge/forge-1.21.1/` | Minecraft 1.21.1, Forge 52.1.14 | 21 | `TebexForgePlugin` | `ForgePlatform` |
| `forge-26.1` | `forge/forge-26.1/` | Minecraft 26.1, Forge 62.0.9 | 25 | `TebexForgePlugin` | `ForgePlatform` |
| `forge-26.2` | `forge/forge-26.2/` | Minecraft 26.2, Forge 65.0.9 | 25 | `TebexForgePlugin` | `ForgePlatform` |
| `forge-26.3` | `forge/forge-26.3/` | Minecraft 26.3, Forge 66.0.8 | 25 | `TebexForgePlugin` | `ForgePlatform` |
| `neoforge-1.20.2` | `neoforge/neoforge-1.20.2/` | Minecraft 1.20.2, NeoForge 20.2.93 | 17 | `TebexNeoForgePlugin` | `NeoForgePlatform` |
| `neoforge-1.21.1` | `neoforge/neoforge-1.21.1/` | Minecraft 1.21.1, NeoForge 21.1.230 | 21 | `TebexNeoForgePlugin` | `NeoForgePlatform` |
| `neoforge-26.1` | `neoforge/neoforge-26.1/` | Minecraft 26.1, NeoForge 26.1.0.19-beta | 25 | `TebexNeoForgePlugin` | `NeoForgePlatform` |
| `neoforge-26.2` | `neoforge/neoforge-26.2/` | Minecraft 26.2, NeoForge 26.2.0.57 | 25 | `TebexNeoForgePlugin` | `NeoForgePlatform` |