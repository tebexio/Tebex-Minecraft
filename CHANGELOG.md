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