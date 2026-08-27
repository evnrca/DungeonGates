# DungeonGates

 Minecraft Paper plugin for creating dungeon progression systems using WorldGuard regions and MythicMobs.

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0+-green)](https://worldguard.enginehub.org/)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-5.6+-red)](https://www.mythicmobs.net/)

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Placeholders (PlaceholderAPI)](#placeholders-placeholderapi)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Building from Source](#building-from-source)
- [Troubleshooting](#troubleshooting)
- [Future Expansion](#future-expansion)
- [License](#license)

---

## Features

| Feature | Description |
|---------|-------------|
| **WorldGuard Integration** | Dungeon rooms defined by existing WorldGuard regions |
| **MythicMobs Support** | Kill requirements tracked via MythicMobs API |
| **Linear Progression** | Players must complete room requirements before advancing |
| **Per-Player Progress** | Individual progress tracking for each player |
| **In-Game Setup** | Simple commands to register rooms without editing config files |
| **Configurable Actions** | Multiple options for failed progression (velocity, teleport, knockback, cancel) |
| **PlaceholderAPI Support** | 7 placeholders for progress display in other plugins |
| **Performance Optimized** | Event-based detection, caching, no constant scanning |
| **Configuration Validation** | Startup checks with clear error messages |
| **Modular Architecture** | Clean separation for future expansion |

---

## Requirements

| Dependency | Minimum Version | Type |
|------------|-----------------|------|
| **Paper** | 1.21.1+ | Required |
| **Java** | 21 | Required |
| **WorldGuard** | 7.0+ | Required |
| **MythicMobs** | 5.6+ | Required |
| **PlaceholderAPI** | 2.11+ | Optional |

---

## Installation

### Automatic (Recommended)
1. Download the latest `DungeonGates.jar` from [Releases](https://github.com/evnrca/DungeonGates/releases)
2. Place in your server's `plugins/` folder
3. Ensure WorldGuard and MythicMobs are installed and working
4. Start/restart your server
5. The plugin will generate a default `config.yml` in `plugins/DungeonGates/`

### Manual Build
```bash
# Requires JDK 21 and Gradle
git clone https://github.com/evnrca/DungeonGates.git
cd DungeonGates
./gradlew shadowJar
# Output: build/libs/DungeonGates-1.0.0.jar
```

---

## Quick Start

### 1. Create WorldGuard Regions

Define your dungeon rooms using WorldGuard:

```bash
# Stand in one corner of room 1
/rg define room1

# Stand in one corner of room 2
/rg define room2

# Stand in one corner of room 3
/rg define room3

# Stand in one corner of room 4
/rg define room4
```

> **Tip**: Use `/rg define room1 -g` to use your WorldEdit selection.

### 2. Register Rooms

Register each room with a MythicMob kill requirement:

```bash
/dg add room1 room1 10
/dg add room2 room2 20
/dg add room3 room3 15
/dg add room4 room4 30
```

**Syntax**: `/dg add <room_name> <worldguard_region> <required_kills>`

| Argument | Description |
|----------|-------------|
| `room_name` | Internal name for the room (used in commands/messages) |
| `worldguard_region` | The WorldGuard region name you created |
| `required_kills` | Number of MythicMobs players must kill in this room |

### 3. Spawn MythicMobs

Use MythicMobs to spawn mobs in each region:

```bash
# Spawn 5 DUNGEON_ZOMBIEs at coordinates
/mm mob spawn DUNGEON_ZOMBIE 5 <x> <y> <z>

# Or use MythicMobs spawners in your region
/mm spawner create DungeonSpawner DUNGEON_ZOMBIE 10 30 60
```

### 4. Play!

Players enter `room1`, kill 10 MythicMobs, then can proceed to `room2`, etc.

---

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/dg add <room> <region> <kills>` | `dungeongates.admin` | Register a new dungeon room |
| `/dg remove <room>` | `dungeongates.admin` | Remove a registered room |
| `/dg list` | `dungeongates.admin` | List all registered rooms |
| `/dg status [player]` | `dungeongates.status` / `dungeongates.status.others` | Check dungeon progress |
| `/dg reset <player> [room]` | `dungeongates.reset` | Reset player progress |
| `/dg reload` | `dungeongates.admin` | Reload configuration |

### Command Examples

```bash
# Add rooms
/dg add entrance entrance_region 5
/dg add corridor1 corridor_region 15
/dg add boss_room boss_region 1

# List all rooms
/dg list

# Check own progress
/dg status

# Check another player's progress (admin)
/dg status Steve

# Reset player's progress in specific room
/dg reset Steve room1

# Reset all progress for player
/dg reset Steve

# Reload config after manual edits
/dg reload
```

### Example `/dg list` Output

```
=== Dungeon Gates Rooms ===
1. entrance - 5 MythicMob kills
2. corridor1 - 15 MythicMob kills
3. boss_room - 1 MythicMob kills
==============================
```

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dungeongates.admin` | OP | All admin commands (add, remove, list, reload) |
| `dungeongates.status` | TRUE | Check own dungeon progress |
| `dungeongates.status.others` | OP | Check other players' dungeon progress |
| `dungeongates.reset` | OP | Reset player progress |

---

## Configuration

### File Location
`plugins/DungeonGates/config.yml`

### Complete Configuration Reference

```yaml
# DungeonGates Configuration
# Documentation: https://github.com/evnrca/DungeonGates

# Progress tracking mode
# INDIVIDUAL - Each player tracks their own progress (default)
# SHARED - Progress is shared across all players in the dungeon
settings:
  progress-mode: INDIVIDUAL

# Action to take when player fails to meet requirements for next room
failed-progression:
  # Supported actions: VELOCITY, TELEPORT, CANCEL, KNOCKBACK
  action: VELOCITY
  
  # VELOCITY settings - launches player backward
  velocity:
    horizontal: 1.5
    vertical: 0.4
  
  # TELEPORT settings - teleports player back
  teleport:
    # true = teleport to last valid location in previous room
    # false = teleport to region center of previous room
    use-last-location: true
  
  # KNOCKBACK settings - applies knockback effect
  knockback:
    horizontal: 1.0
    vertical: 0.3

# Message configuration (all messages support color codes & placeholders)
messages:
  prefix: "&8[&6Dungeon Gates&8] "
  
  # Displayed when player tries to enter next room without completing current
  requirements-not-met:
    - "&cYou cannot proceed yet!"
    - "&7You still need to kill &e{remaining_kills} &7more MythicMobs in &e{current_room}&7."
  
  # Displayed when player completes a room
  room-completed:
    - "&aRoom &e{current_room} &acompleted!"
    - "&eYou may now proceed to the next room."
  
  # Displayed when checking progress
  room-progress:
    - "&eProgress in &f{current_room}&e: &f{current_kills}&7/&f{required_kills} &e(&f{progress_percent}%&e)"
  
  # Command feedback messages
  room-added: "&aSuccessfully added room &e{room} &awith requirement of &e{kills} &aMythicMob kills."
  room-removed: "&aSuccessfully removed room &e{room}&a."
  room-already-exists: "&cRoom &e{room} &cis already registered."
  region-not-found: "&cWorldGuard region &e{region} &cdoes not exist."
  invalid-kills: "&cKill requirement must be a positive number."
  no-rooms-registered: "&eNo dungeon rooms are currently registered."
  rooms-list-header: "&6=== &eDungeon Gates Rooms &6==="
  rooms-list-format: "&e{index}. &f{room} &7- &e{kills} &7MythicMob kills"
  rooms-list-footer: "&6=============================="
  
  status-current-room: "&eCurrent Room: &f{room}"
  status-progress: "&eMythicMob Kills: &f{current}&7/&f{required} &7(&f{remaining} &7remaining)"
  status-completed: "&aRoom completed! You may proceed."
  
  progress-reset: "&aProgress reset for &e{player}&a."
  progress-reset-room: "&aProgress reset for &e{player} &ain room &e{room}&a."
  config-reloaded: "&aConfiguration reloaded successfully."
  
  # Error messages
  error-player-not-found: "&cPlayer &e{player} &cnot found."
  error-no-progress: "&eNo progress found for &e{player}&e."
  error-not-in-dungeon: "&cYou are not currently in a registered dungeon room."
  error-generic: "&cAn error occurred. Check console for details."

# Progress reset behavior
progress-reset:
  on-player-death: false
  on-server-restart: false
  on-dungeon-exit: false
  on-completion: true

# Dungeon rooms configuration (auto-generated by /dg add)
# Format:
# rooms:
#   room1:
#     region: "room1"
#     required-mythicmob-kills: 10
#   room2:
#     region: "room2"
#     required-mythicmob-kills: 20
rooms: {}

# Future expansion: Specific MythicMob requirements
# When enabled, only kills of specified mobs count toward requirements
requirements:
  total-mythicmob-kills: 0
  specific-mythicmobs:
    enabled: false
    mobs: {}
    # Example:
    # DUNGEON_ZOMBIE: 5
    # DUNGEON_SPIDER: 3
```

### Message Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Player name |
| `{room}` / `{current_room}` | Current room name |
| `{next_room}` | Next room name |
| `{current_kills}` | Current kill count in room |
| `{required_kills}` | Required kills for room |
| `{remaining_kills}` | Kills remaining to complete |
| `{progress_percent}` | Progress percentage (e.g., 45.5) |
| `{index}` | Room index in list |
| `{kills}` | Kill requirement (for room-added) |
| `{region}` | WorldGuard region name |

---

## Placeholders (PlaceholderAPI)

If PlaceholderAPI is installed, these placeholders are available for use in other plugins (HolographicDisplays, Scoreboards, etc.):

| Placeholder | Description |
|-------------|-------------|
| `%dungeongates_current_room%` | Current room name (or "None") |
| `%dungeongates_current_kills%` | Current kills in current room |
| `%dungeongates_required_kills%` | Required kills for current room |
| `%dungeongates_remaining_kills%` | Kills remaining to complete room |
| `%dungeongates_progress_percent%` | Progress percentage (0-100) |
| `%dungeongates_rooms_completed%` | Total rooms completed by player |
| `%dungeongates_total_rooms%` | Total dungeon rooms registered |

### Example HolographicDisplays Configuration

```yaml
holograms:
  dungeon_progress:
    world: world
    x: 100.5
    y: 65.0
    z: 200.5
    lines:
      - "&6Dungeon Progress"
      - "&eRoom: &f%dungeongates_current_room%"
      - "&eKills: &f%dungeongates_current_kills%&7/&f%dungeongates_required_kills%"
      - "&eProgress: &f%dungeongates_progress_percent%%"
```

---

## How It Works

### Room Progression Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Room 1    │────▶│   Room 2    │────▶│   Room 3    │────▶│   Room 4    │
│  (5 kills)  │     │  (15 kills) │     │  (20 kills) │     │  (1 boss)   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │                   │
      ▼                   ▼                   ▼                   ▼
  [WG Region]        [WG Region]        [WG Region]        [WG Region]
```

1. **Room Registration**: Each room links to a WorldGuard region with a kill requirement
2. **Progression Order**: Rooms are ordered by registration sequence (first added = first room)
3. **Kill Tracking**: When a player kills a MythicMob in a registered region, progress increments
4. **Room Entry Check**: When a player tries to enter the next room, the plugin checks if the previous room is complete
5. **Failed Progression**: If requirements aren't met, the configured action is applied

### Kill Counting Logic

A kill counts toward progress **only if**:
- ✅ The killed entity is a valid MythicMob (checked via MythicMobs API)
- ✅ The player receives kill credit (is the killer)
- ✅ The kill happens inside a registered WorldGuard dungeon room
- ✅ The kill belongs to the player's **current** dungeon room

### Failed Progression Actions

| Action | Behavior |
|--------|----------|
| **VELOCITY** | Launches player backward toward previous room (configurable horizontal/vertical force) |
| **TELEPORT** | Teleports player to last valid location in previous room (or region center) |
| **CANCEL** | Prevents movement into next room (player stays at boundary) |
| **KNOCKBACK** | Applies knockback effect toward previous room |

---

## Architecture

```
com.dungeongates
├── DungeonGatesPlugin          # Main plugin class, lifecycle management
├── commands
│   └── DungeonGatesCommand     # Command handling, tab completion
├── config
│   └── ConfigManager           # Config loading, validation, room persistence
├── dungeon
│   ├── DungeonRoom             # Room model (name, region, requiredKills, order)
│   ├── PlayerProgress          # Per-player progress container (Map<room, RoomProgress>)
│   ├── RoomProgress            # Per-room progress (kills, specificMobKills, completed)
│   ├── RoomManager             # Room registration, lookup, ordering, validation
│   └── ProgressManager         # Kill tracking, completion checks, entry validation
├── integrations
│   ├── WorldGuardHook          # Region detection, validation, region-to-room mapping
│   ├── MythicMobsHook          # MythicMob identification, kill validation
│   └── PlaceholderAPIHook      # PlaceholderExpansion implementation
├── listeners
│   ├── PlayerMovementListener  # Room entry/exit detection, failed progression handling
│   ├── MythicMobKillListener   # EntityDeathEvent handling, kill attribution
│   └── PlayerQuitListener      # Cleanup on quit, progress reset handling
└── utils
    ├── ColorUtil               # Legacy (&) color code → Adventure Component
    └── MessageUtil             # Placeholder replacement, message sending
```

### Key Design Decisions

| Aspect | Approach |
|--------|----------|
| **Data Storage** | YAML (config.yml) with abstraction for future DB migration |
| **Region Detection** | WorldGuard API via `ApplicableRegionSet` (event-based, cached) |
| **MythicMob Detection** | `MythicBukkit.inst().getMobManager().isActiveMob()` |
| **Progress Tracking** | `ConcurrentHashMap<UUID, PlayerProgress>` (thread-safe) |
| **Room Ordering** | Registration order stored in config, mutable via manual edit |
| **Event Priorities** | `MONITOR` for kill/movement (non-cancelling, observes outcome) |

---

## Building from Source

### Prerequisites
- JDK 21+
- Gradle 8+ (or use wrapper)

### Build Commands

```bash
# Clone repository
git clone https://github.com/evnrca/DungeonGates.git
cd DungeonGates

# Build shaded JAR (includes dependencies)
./gradlew shadowJar

# Output: build/libs/DungeonGates-1.0.0.jar

# Run tests (if added)
./gradlew test

# Clean build
./gradlew clean shadowJar
```

### Build Output

The `shadowJar` task produces a fat JAR with:
- All plugin classes
- Shaded dependencies (SnakeYAML)
- Proper `plugin.yml` manifest attributes

---

## Troubleshooting

### "WorldGuard region not found"
```bash
# Verify region exists
/rg info <region_name>

# Common issues:
# - Region name is case-sensitive
# - Region must be in a loaded world
# - Use exact name from /rg list
```

### "MythicMobs not found"
- Install MythicMobs 5.6+
- Ensure MythicMobs loads before DungeonGates (handled by `depend` in plugin.yml)
- Check console for MythicMobs startup messages

### Kills not counting
```bash
# Verify mob is a MythicMob
/mm mob info <entity>

# Check kill happens inside the WorldGuard region
# Ensure player gets kill credit (not environmental damage)
# Verify room is registered: /dg list
```

### Players can skip rooms
- Verify room order with `/dg list`
- Check WorldGuard regions don't overlap unexpectedly
- Ensure previous room has completion requirement > 0
- Check console for validation warnings on startup

### Configuration not saving
- Ensure server has write permissions to `plugins/DungeonGates/`
- Check console for IO errors on save

---

## Future Expansion

The codebase is designed for modularity with these extension points ready:

| Feature | Status | Implementation Notes |
|---------|--------|---------------------|
| **Specific MythicMob Requirements** | Architecture Ready | `requirements.specific-mob` config section exists, `RoomProgress` tracks per-mob kills |
| **Party/Shared Progress** | Framework Exists | `settings.progress-mode: SHARED` reserved, `ProgressManager` abstracted |
| **Multiple Dungeon Paths** | Room Ordering Supports | Branching via manual config edit of room order |
| **Database Storage** | Abstraction Ready | `ConfigManager` interface allows SQLite/MySQL migration |
| **Boss Requirements** | Extensible | Add new requirement types to `ProgressManager` |
| **Rewards** | Event Hooks Ready | Listen for room completion in other plugins |
| **Timers** | Progress Tracking Ready | `RoomProgress` has `completedAt` timestamp |
| **Multiple Completion Conditions** | Modular Design | Add new `Requirement` implementations |

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Support

- **Issues**: [GitHub Issues](https://github.com/evnrca/DungeonGates/issues)
- **Discussions**: [GitHub Discussions](https://github.com/evnrca/DungeonGates/discussions)

---

## License

MIT License - See [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 DungeonGates

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Credits

- [PaperMC](https://papermc.io/) - High-performance Minecraft server API
- [WorldGuard](https://worldguard.enginehub.org/) - Region protection
- [MythicMobs](https://www.mythicmobs.net/) - Custom mob system
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - Placeholder system
- [Adventure](https://github.com/KyoriPowered/Adventure) - Text component library

---

**Made with ❤️ for the Minecraft server community**
