# DungeonGates

Lightweight Minecraft Paper plugin for dungeon progression using WorldGuard regions and MythicMobs.

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0+-green)](https://worldguard.enginehub.org/)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-5.6+-red)](https://www.mythicmobs.net/)
[![SQLite](https://img.shields.io/badge/SQLite-Persistent-blue)](https://www.sqlite.org/)

---

## Features

- **WorldGuard Integration** — Dungeon rooms = existing WorldGuard regions (direct compile-time API)
- **MythicMobs Support** — Kill requirements via MythicMobs API (direct compile-time API)
- **Linear Progression** — Complete room requirements before advancing
- **Per-Player Progress** — Individual tracking, no interference
- **In-Game Setup** — Register rooms with `/dg add <region> <kills>`
- **Room Denial System** — Title messages, sounds, chat messages when denied
- **SQLite Progress Persistence** — Survives server restarts/reloads (stored in `plugins/DungeonGates/progress.db`)
- **Progress Reset** — Clears on death, logout, teleport, world exit
- **Knockback Denial** — Players knocked back into previous room instead of teleported
- **Entrance & Exit Control** — Second+ rooms require previous completion on entry AND exit
- **Admin Bypass** — `dungeongates.bypass` permission skips all checks
- **Debug Mode** — `/dg debug` for troubleshooting
- **Configurable Denial** — Title, subtitle, sound, knockback strength
- **Sound & Title Notifications** — Clear feedback when denied entry

---

## Requirements

| Dependency | Minimum Version | Type |
|------------|-----------------|------|
| Paper | 1.21.1+ | Required |
| Java | 21 | Required |
| WorldGuard | 7.0+ | Required |
| MythicMobs | 5.6+ | Required |

---

## Installation

1. Download `DungeonGates-1.0.0.jar` from [Releases](https://github.com/evnrca/DungeonGates/releases)
2. Place in `plugins/` folder
3. Ensure WorldGuard and MythicMobs are installed
4. Start server → generates `plugins/DungeonGates/config.yml` and `plugins/DungeonGates/progress.db`

---

## Quick Start

### 1. Create WorldGuard Regions
```
/rg define room1
/rg define room2
/rg define room3
```

### 2. Register Rooms
```
/dg add room1 10
/dg add room2 15
/dg add room3 20
```
Syntax: `/dg add <worldguard-region> <required-kills>`

### 3. Spawn MythicMobs in each region
```
/mm mob spawn DUNGEON_ZOMBIE 5 <x> <y> <z>
```

### 4. Play!
Players kill required MythicMobs in each room to unlock the next.

---

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/dg add <world> <region> <kills>` | `dungeongates.admin` | Register a dungeon room (with world) |
| `/dg add <region> <kills>` | `dungeongates.admin` | Register in 'world' (legacy) |
| `/dg remove <world> <region>` | `dungeongates.admin` | Remove a room |
| `/dg remove <region>` | `dungeongates.admin` | Remove from 'world' (legacy) |
| `/dg list [world]` | `dungeongates.admin` | List rooms (all or specific world) |
| `/dg status [player]` | `dungeongates.status` / `.others` | Check progress |
| `/dg reset <player> [world] [region]` | `dungeongates.reset` | Reset progress |
| `/dg reload` | `dungeongates.admin` | Reload config |
| `/dg debug [on|off]` | `dungeongates.admin` | Toggle debug mode |
| `/dg info` | `dungeongates.admin` | Show plugin info (version, author, repo) |

---

## How It Works

1. **Register rooms** with `/dg add <region> <kills>` (order = registration order)
2. **Players kill MythicMobs** in the WorldGuard region
3. **On region entry**, plugin checks if previous room is complete
4. **On region exit**, plugin checks if current room is complete
5. **If not complete** → deny entry/exit:
    - **Title & Subtitle** — "ROOM LOCKED! Kill X more MythicMobs to proceed."
    - **Sound** — Configurable (default: VILLAGER_NO)
    - **Chat Message** — "You need X more MythicMob kills to enter region!"
    - **Knockback** — Pushes player back into the previous room
6. **If complete** → Allow entry/exit, update last valid location

### Room Access Rules
- **First room** — Always accessible (entry and exit)
- **Next room in sequence** — Requires previous room completion (entry AND exit)
- **Previously completed rooms** — Always accessible (free return)
- **Non-sequential rooms** — Blocked unless already completed

### Progress Reset Conditions
Progress is **fully cleared** when:
- Player dies in dungeon
- Player logs out
- Player teleports out of dungeon
- Player exits the dungeon world (via portal, world change)

After reset, player must re-complete all rooms.

---

## Configuration

`plugins/DungeonGates/config.yml`

```yaml
denial:
  action: CANCEL              # CANCEL, VELOCITY, TELEPORT, KNOCKBACK (KNOCKBACK recommended)
  velocity:
    horizontal: 1.5
    vertical: 0.4
  title: "&c&lROOM LOCKED!"
  subtitle: "&7Kill &e{remaining} &7more MythicMobs to proceed."
  sound: "ENTITY_VILLAGER_NO"
  sound-volume: 1.0
  sound-pitch: 1.0

messages:
  prefix: "&8[&6Dungeon Gates&8] "
  requirement-not-met: "&cYou need {remaining} more MythicMob kills to enter {region}!"
  progress: "&eProgress: {current}/{required} MythicMobs killed"
  completed: "&aRoom &e{region} &acompleted! You may now proceed."
  progress-reset-death: "&cYou died! Your dungeon progress has been reset."
  progress-reset-logout: "&cYour dungeon progress has been reset (logout)."
  progress-reset-teleport: "&cYour dungeon progress has been reset (teleport)."
  progress-reset-world-exit: "&cYour dungeon progress has been reset (left world)."

rooms:
  # region: kills (order = registration order)
  # room1: 10
  # room2: 15
```

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dungeongates.admin` | OP | All admin commands |
| `dungeongates.status` | TRUE | Check own progress |
| `dungeongates.status.others` | OP | Check others' progress |
| `dungeongates.reset` | OP | Reset progress |
| `dungeongates.bypass` | OP | Bypass all region checks (admin) |

---

## Progress Persistence (SQLite)

Progress is stored in `plugins/DungeonGates/progress.db` (SQLite database):
- **Survives restarts/reloads** — No progress lost on server restart
- **WAL mode** — High concurrency, minimal locking
- **Auto-saves** — On kill, quit, admin commands, shutdown
- **In-memory cache** — Fast access for frequent operations

Schema:
```sql
CREATE TABLE dungeon_progress (
    uuid TEXT NOT NULL,
    room_key TEXT NOT NULL,  -- "world:region"
    kills INTEGER DEFAULT 0,
    completed INTEGER DEFAULT 0,
    last_updated INTEGER DEFAULT 0,
    PRIMARY KEY (uuid, room_key)
);
```

---

## Build

```bash
git clone https://github.com/evnrca/DungeonGates.git
cd DungeonGates
./gradlew shadowJar
# Output: build/libs/DungeonGates-1.0.0.jar
```

Requires JDK 21. Dependencies: `paper-api` (compileOnly), `worldguard-bukkit`, `worldedit-bukkit`, `Mythic-Dist` (all compileOnly — not shaded), `sqlite-jdbc` (shaded into JAR).

---

## License

MIT License

---

## Credits

- [PaperMC](https://papermc.io/) — Server API
- [WorldGuard](https://worldguard.enginehub.org/) — Region protection
- [MythicMobs](https://www.mythicmobs.net/) — Custom mobs
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) — Embedded database