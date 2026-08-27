# DungeonGates

Lightweight Minecraft Paper plugin for dungeon progression using WorldGuard regions and MythicMobs.

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0+-green)](https://worldguard.enginehub.org/)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-5.6+-red)](https://www.mythicmobs.net/)

---

## Features

- **WorldGuard Integration** — Dungeon rooms = existing WorldGuard regions (reflection-based API)
- **MythicMobs Support** — Kill requirements via MythicMobs API (reflection-based)
- **Linear Progression** — Complete room requirements before advancing
- **Per-Player Progress** — Individual tracking, no interference
- **In-Game Setup** — Register rooms with `/dg add <region> <kills>`
- **Room Denial System** — Title messages, sounds, chat messages when denied
- **Progress Persistence** — Retains access to completed rooms
- **Progress Reset** — Clears on death, logout, teleport, world exit
- **Configurable Denial** — CANCEL, VELOCITY, TELEPORT, KNOCKBACK
- **Sound & Title Notifications** — Clear feedback when denied entry
- **Zero Compile Dependencies** — Only Paper API; WorldGuard/MythicMobs via reflection

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
4. Start server → generates `plugins/DungeonGates/config.yml`

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
| `/dg add <region> <kills>` | `dungeongates.admin` | Register a room |
| `/dg remove <region>` | `dungeongates.admin` | Remove a room |
| `/dg list` | `dungeongates.admin` | List rooms |
| `/dg status [player]` | `dungeongates.status` / `.others` | Check progress |
| `/dg reset <player> [region]` | `dungeongates.reset` | Reset progress |
| `/dg reload` | `dungeongates.admin` | Reload config |

---

## How It Works

1. **Register rooms** with `/dg add <region> <kills>` (order = registration order)
2. **Players kill MythicMobs** in the WorldGuard region
3. **On region entry**, plugin checks if previous room is complete
4. **If not complete** → deny entry:
   - **Title & Subtitle** — "ROOM LOCKED! Kill X more MythicMobs to proceed."
   - **Sound** — Configurable (default: VILLAGER_NO)
   - **Chat Message** — "You need X more MythicMob kills to enter region!"
   - **Teleport Back** — Returns to last valid location in previous room
5. **If complete** → Allow entry, update last valid location

### Room Access Rules
- **First room** — Always accessible
- **Next room in sequence** — Requires previous room completion
- **Previously completed rooms** — Always accessible (free return)
- **Non-sequential rooms** — Blocked unless already completed

### Progress Reset Conditions
Progress is **fully cleared** when:
- Player dies in dungeon
- Player logs out
- Player teleports out of dungeon
- Player exits the dungeon world

After reset, player must re-complete all rooms.

---

## Configuration

`plugins/DungeonGates/config.yml`

```yaml
denial:
  action: CANCEL              # CANCEL, VELOCITY, TELEPORT, KNOCKBACK
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

---

## Build

```bash
git clone https://github.com/evnrca/DungeonGates.git
cd DungeonGates
./gradlew shadowJar
# Output: build/libs/DungeonGates-1.0.0.jar
```

Requires JDK 21. Only dependency: `paper-api` (WorldGuard/MythicMobs via reflection at runtime).

---

## License

MIT License

---

## Credits

- [PaperMC](https://papermc.io/) — Server API
- [WorldGuard](https://worldguard.enginehub.org/) — Region protection
- [MythicMobs](https://www.mythicmobs.net/) — Custom mobs