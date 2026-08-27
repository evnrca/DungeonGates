# DungeonGates

Lightweight Minecraft Paper plugin for dungeon progression using WorldGuard regions and MythicMobs.

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![WorldGuard](https://img.shields.io/badge/WorldGuard-7.0+-green)](https://worldguard.enginehub.org/)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-5.6+-red)](https://www.mythicmobs.net/)

---

## Features

- **WorldGuard Integration** — Dungeon rooms = existing WorldGuard regions
- **MythicMobs Support** — Kill requirements via MythicMobs API (reflection, no compile dep)
- **Linear Progression** — Complete room requirements before advancing
- **Per-Player Progress** — Individual tracking, no interference
- **In-Game Setup** — Register rooms with `/dg add <region> <kills>`
- **Failed Entry Actions** — VELOCITY, TELEPORT, CANCEL, KNOCKBACK
- **Zero Dependencies** — Only Paper API at compile time; WorldGuard/MythicMobs via reflection
- **Performance** — Event-based, cached regions, no scanning

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

## Configuration

`plugins/DungeonGates/config.yml`

```yaml
failed-entry:
  action: VELOCITY          # VELOCITY, TELEPORT, CANCEL, KNOCKBACK
  velocity:
    horizontal: 1.5
    vertical: 0.4

messages:
  prefix: "&8[&6Dungeon Gates&8] "
  requirement-not-met: "&cYou need {remaining} more MythicMob kills!"
  progress: "&eProgress: {current}/{required}"
  completed: "&aRoom requirement completed!"

rooms:
  # region: kills (order = registration order)
  # room1: 10
  # room2: 15
```

---

## How It Works

1. **Register rooms** with `/dg add <region> <kills>` (order = registration order)
2. **Players kill MythicMobs** in the WorldGuard region
3. **On region entry**, plugin checks if previous room is complete
4. **If not complete** → deny entry + configured action (velocity/teleport/cancel/knockback)

**Kill counts only if:**
- Entity is a valid MythicMob (via MythicMobs API)
- Player gets kill credit
- Kill happens inside the registered region

---

## Failed Entry Actions

| Action | Behavior |
|--------|----------|
| **VELOCITY** | Launch backward (configurable force) |
| **TELEPORT** | Teleport to previous room center |
| **CANCEL** | Block movement at boundary |
| **KNOCKBACK** | Apply knockback toward previous room |

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

Requires JDK 21. Only dependency: `paper-api` (WorldGuard/MythicMobs via reflection).

---

## License

MIT License

---

## Credits

- [PaperMC](https://papermc.io/) — Server API
- [WorldGuard](https://worldguard.enginehub.org/) — Region protection
- [MythicMobs](https://www.mythicmobs.net/) — Custom mobs