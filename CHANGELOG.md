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