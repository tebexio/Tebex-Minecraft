2.4.0
=====
- Feature/Fix: XUIDs are now supported, verified with Floodgate API. This resolves issues delivering commands on Geyser stores. (thanks @itscainnotkain!)
- Feature: Support for Fabric 26.1 (thanks @Manganoos!)
- Feature: Bedrock checkout links will instruct the user to type the checkout link into their browser.

- Fix: Improper class name for Bungee plugin (thanks @iiismael!)

**Notice**: Fabric versions prior to 26.1 are no longer supported after this version.

2.3.3
=====
Fix: UUID matching not working properly for offline players / Geyser servers (thanks @itscainnotkain)

Fix: Duplicated command queue handling for Folia (thanks @sluhtie)

Fix: Permission checks on certain Fabric versions (thanks @chaaadii )
Fix: /buy GUI will no longer show if a store is not connected
Fix: ArrayIndexOutOfBounds exception for multithreaded servers
Fix: Memory optimization and thread-safety improved for Fabric and BungeeCord
Fix: Backwards compatibility support for all GUI colors
Fix: Incorrect telemetry sent from Fabric plugins
Fix: ClassCastException on certain store types when commands are executed

2.3.2
=====
Fix: Unnecessary command checks initiated during loading phase
Fix: UUID replacement for offline stores #108

2.3.1
=====
Fix: UUIDs incorrectly replaced with player name for offline servers
Fix: Failure to process one player would cause processing for other players to stop
Fix: Debug settings were not saved to file
Fix: Errors loading config on Velocity
Fix: Secret key would not persist after setting on certain setups
Fix: [Fabric] Proper versions of fabric-permissions-api per version (thank you, MemencioPerez!)

2.3.0
=====
Tebex now officially supports Folia! Thank you, @R00tB33rMan

This version includes builds for Fabric 1.21.1, 1.21.4, 1.21.5, 1.21.6, and 1.21.7. You must use the correct variant for your version of Fabric.

## Features
- Secret key can now be specified via the TEBEX_SECRET_KEY environment variable. This will override any configuration setting.
- [SDK] Included unit tests and mocks for SDK and platform
- [SDK] Data classes now use Lombok
- [SDK] Improved overall documentation

## Changes
- All in-game command code moved to SDK. See TebexCommands.
- Further generalized Platform into BasePlatform

## Fixes
- Player UUIDs that fill {id} placeholders are now no longer truncated. Fixes support for other mods/plugin commands.
- /buy GUI packages now respect store order
- [Velocity] All logs used the incorrect logger causing improper log levels
- Tebex docs link in configuration is no longer incorrect
- Failure to send plugin events no longer leads to unbounded growth
- /sendlink command did not execute asynchronously #88

2.2.1
=====
Notice: This is the final supported build for Fabric 1.20.1 and 1.20.4.

Feature: A build for Fabric 1.21.4 is now available (Thanks @ZekeZDev) #82
Feature: Better command suggestions on Velocity (Thanks @RealBauHD) #70

Fix: Create coupon caused unnecessary RuntimeException if Cart used as effective type #78
Fix: Unexpected status code when sending server events caused by excessive precision in date format #79
Fix: /information request was duplicated on plugin enable
Fix: Some requests would attempt to run if secret key was not set or invalid
Fix: Proper version string replacement in all variants
Fix: Improvements to debug logging

2.2.0
=====
Feature: Player IP addresses for join events are now anonymized before sending to Tebex

Fix [Fabric]: Items can no longer be taken out of or placed into the /buy GUI
Fix: Threads did not shut down properly on Fabric #71
Fix: Online commands did not respect command delays
Fix: Server events use Instant.now() for UTC timing. #73
Fix: Buy GUI now appropriately re-sizes according to number of packages in a category #75

2.1.0
=====
This version resolves the majority of cross-version and inter-plugin issues experienced on the current Bukkit variant.

- Bukkit/Spigot/Paper support: 1.8.8+
- BungeeCord support: 1.18+
- Velocity support: API 3.3.0 and above
- Fabric support: 1.20.1, 1.20.4, 1.21.1

## Fixes
- `java.lang.NoSuchMethodError: com.google.gson.JsonArray.add` on certain Minecraft versions fixed
- Join event queue could become too large and indefinitely fail to send
- Triumph dependency made redundant (Bukkit)
- sgui dependency made redundant (Fabric)
- `tebex reload` now appropriately reloads the buy command
- `com.google.gson` is now relocated to avoid version conflicts
- `com.cryptomorin.xseries` is now relocated to avoid conflicts
- Improved tracking of warning and error events relating to processing player commands
- An appropriate `User-Agent` is now set for Tebex API requests
- Store listings are always appropriately refreshed after setting a secret key
- Updated all remaining dependencies

2.0.6
=====
## Features
- Improved warning and error logs. All warnings will suggest solutions. Revised which errors and data are sent automatically to Tebex.

## Fixes
- `/buy` command no longer causes the crashing/errors affecting **1.20.6** and **1.21**
- `isBuyCommandEnabled` setting was ignored in previous versions and now works properly

2.0.5
=====
- Checks for commands would not be rescheduled if an error occurred, preventing commands from being checked until server restart
- Commands that required slots and delays incorrectly used the delay value as the number of slots required
- Improvements to error reporting system
- `/tebex report` did not allow for multiple words
- `/tebex lookup` no longer causes an error if the player had no purchases, and has more descriptive error messaging
- Version did not not appear correctly in Velocity
- Version did not appear correctly in Fabric
- Index out of bounds if /tebex debug is used without an arg on Velocity

2.0.4
=====

### Fixes
- The `{id}` parameter is now properly replaced on Geyser and Minecraft Offline/Geyser store types. For offline servers, it will be replaced with the user's name. For online servers, this will be the player's UUID.
- Fix for `java.lang.String cannot be cast to class java.util.UUID` on Offline/Geyser servers
- Fixed the use of deprecated characters directly in components in the Velocity module
- Relocated Adventure to prevent conflicts with older Adventure APIs on the server

2.0.3
=====

### Features
- **SDK:** `{uuid}` command parameter is now filled by the plugin if a uuid is available and not filled by Tebex API

### Fixes
- **Minecraft Offline/Geyser:** Offline actions (such as removing groups) with no payment or package attached will now be processed properly.
- **Minecraft Offline/Geyser:** Certain types of offline commands still could not be parsed and executed, causing console errors.
- **Bukkit:** `/sendlink` now sends the checkout link to the target player
- **SDK:** mojangIdToJavaId() no longer returns a null ID if any provided parameters are null

2.0.2
=====

### Features
- Improvements to debug mode showing relevant HTTP request and response data
- `/tebex lookup` provides more in-depth feedback when users are not found

### Changes
- `/tebex ban` no longer requires an ip or reason

### Fixes
- Servers running offline mode are now able to process commands
- Online commands are handled more efficiently on large servers to avoid rate limits (code 429)
- Fix for `Failed to get online commands: java.lang.UnsupportedOperationException: JsonNull` caused by online commands with no package reference
- `/tebex sendlink` incorrectly used player id instead of username
- Fix `/tebex report` now sends the entire report
- Arguments such as `{id}` and `{username}` are now properly parsed for all command types

2.0.1
=====

### Fixes
- Command usage instructions are now shown if incorrect/not enough args are used
- Store information was not properly reloaded after running `tebex secret`, causing errors until the server was restarted.
- `/tebex lookup` now uses the appropriate endpoint
- Some commands' usage instructions improperly included a `.` in the command name
- `/tebex report` now properly sends all report information to Tebex

2.0.0
=====
- Plugin has been rewritten from the ground up to include support for: **Bukkit**, **Spigot**, **PaperSpigot**, **Fabric**, **Velocity**, and **BungeeCord**.
  - Your configuration will be migrated from previous versions, if detected.
  
### Features
- Added `/tebex debug <true/false>` for in-depth logging
- Added `/tebex report` to send in-game reports to Tebex
- Added `/tebex goals` to print active community goals
- Added `/tebex checkout` to checkout via command line
- Added `/tebex sendlink` to send a checkout link to a player
- Added automatic error reporting which can be toggled on/off in configuration.

### Changes
- Use `/buy` command to open store GUI instead of using signs.

### Fixes
- Less console spam. Enable in-depth logging with `/tebex debug`
- Any API timeouts are now reported and handled gracefully
