# Coordinate HUD

A small Fabric client-side mod: a single HUD line at the top of the
screen showing facing direction (8-point compass + bearing in degrees),
block coordinates, and distance to the world spawn point. No minimap
rendering, so it doesn't compete or conflict with map mods like Xaero's.

**AI-assisted development disclosure**: this mod's code was written with
AI assistance (Claude). It does not reuse code or assets from any other
mod.

## What it does

- Draws `<direction> <bearing>°  X:<x> Y:<y> Z:<z>  Spawn:<distance>m`
  centered at the top of the screen. `<bearing>` is a navigational
  compass bearing (0°=north, 90°=east, 180°=south, 270°=west), not raw
  Minecraft yaw. `<distance>` is the **horizontal** (X/Z-plane) distance
  to spawn — height (Y) is not counted, so standing directly above or
  below spawn always reads as close, since the line is meant to answer
  "how far do I need to walk".
- Reads only the local player's own position/orientation and the
  world's spawn point — the same information already visible via
  Minecraft's own F3 debug screen, just always-on and compact.
- If the world spawn point is in a different dimension than the one
  you're currently in (e.g. you're in the Nether or the End), the
  distance is shown as `Spawn:--` instead of a meaningless number.
- Hides itself along with the rest of the HUD when F1 (hide HUD) is
  toggled, and while any other screen is open (inventory, chest, death
  screen, menus, ...) — except while chatting, so you can still read
  your coordinates while typing a message.
- 100% client-side: no server component, no network calls, no
  automation of any kind.

## Not included (yet)

- Player-settable custom waypoints (currently uses world spawn as the
  "destination" reference point; a settable waypoint is a candidate for
  a future unit of work).

## Requirements

- Minecraft Java Edition 1.21.11 (Fabric)
- Fabric Loader >= 0.19.5
- Fabric API

Build toolchain follows the pattern established by
`mods/sound-subtitle-colors` in this repo: a bundled Gradle 9.7.1 +
Fabric Loom 1.17.21 wrapper, since the system Gradle (8.14.3) can't
resolve a Loom release new enough for Minecraft 1.21.11.

## Building

```
./gradlew build
```

## License

MIT — see [`LICENSE`](LICENSE).
