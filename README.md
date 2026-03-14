# PvP Auto Entity Hider

RuneLite plugin that automatically hides non-relevant players while you are being attacked in dangerous PvP areas.

When active, it keeps your screen focused on your fight by showing only players currently attacking you (or who very recently attacked you within the grace period) and hiding everyone else.

## Features

- Auto-hides non-attackers while you are under attack
- Works in dangerous PvP contexts:
  - Wilderness
  - PvP-enabled danger areas (`PVP spec orb` context)
- Supports multi-combat by keeping all active attackers visible
- Keeps recent attackers visible for a configurable grace period to prevent flicker between hits
- Ignores simple follow/right-click interactions (does not trigger on follow alone)
- Lightweight render-level filtering (no menu or interaction rewriting)

## Behavior Summary

The plugin only enters hide mode when all of the following are true:

1. Plugin is enabled
2. You are in a dangerous PvP area
3. A player is actively attacking you

While hide mode is active:

- You remain visible
- Active attacker(s) remain visible
- Recent attacker(s) remain visible during grace period
- Other players are hidden

When grace expires (and no one is actively attacking), all players are shown again.

## Configuration

Current plugin settings:

- `Enable Auto Hider` (default: `true`)
- `Grace Period (seconds)` (range: `5` to `8`, default: `7`)

## Installation

### Option A: Local external plugin development

1. Clone this repository
2. Build or run via Gradle
3. Launch RuneLite in developer mode from this project

```bash
gradlew.bat run
```

## Development

### Requirements

- Java 11+ (JDK)
- Gradle wrapper (included)

### Useful commands

```bash
# Compile
gradlew.bat compileJava

# Run RuneLite with this plugin loaded
gradlew.bat run

# Build fat jar
gradlew.bat shadowJar
```

## Project Structure

```text
src/main/java/com/logicalsolutions/pvpentityhider/
  PvpEntityHiderPlugin.java
  PvpEntityHiderConfig.java
  PvpEntityHiderLauncher.java
```
