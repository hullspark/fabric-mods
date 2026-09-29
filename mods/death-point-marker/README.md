# Death Point Marker

A small Fabric client-side mod: after you die, a HUD line appears
showing the bearing, distance and coordinates back to where you died,
so you can walk back and recover your dropped items without guessing.

**AI-assisted development disclosure**: this mod's code was written with
AI assistance (Claude). It does not reuse code or assets from any other
mod.

## What it does

- Reads the vanilla `getLastDeathPos()` value the server already tracks
  per player — the same data the vanilla Recovery Compass points at —
  instead of guessing from client-side events. This means it also works
  when the `doImmediateRespawn` gamerule is on (which skips the death
  screen entirely), and survives relogging.
- Once a death point exists, draws
  `Death: <direction> <bearing>° <distance>m  X:<x> Y:<y> Z:<z>` in the
  top-left corner. `<bearing>` is a navigational compass bearing
  (0°=north, 90°=east, 180°=south, 270°=west) pointing from your current
  position towards the death point. If the line is long enough that its
  right edge would reach the top-center strip the vanilla boss bar and
  Hullspark Coordinate HUD use, it drops below wherever
  Coordinate HUD's line would land instead of overlapping it (Coordinate
  HUD's own line drops further down the more boss bars are active).
- Once you walk within 3 blocks of the death point (you've arrived),
  this mod remembers that you recovered it and the line stays hidden
  even after you wander off again — it only reappears once the server
  reports a different death point (your next death). This is remembered
  in a small file in the Fabric config directory, so it survives
  restarting the game. The record is tagged with which world/server it
  was recovered in (server address for multiplayer, or the save's
  folder name under `saves/` for singleplayer — not the display name
  shown in the world list, since that isn't unique: new worlds default
  to "New World", and copies/backups keep their original name), so a
  different world/server happening to have a death at the same
  coordinates is never wrongly treated as already-recovered. Only the
  single most recent recovery is remembered, though (see Known
  limitations below for what that means when moving between worlds). A
  read/write failure there is not fatal, it just falls back to tracking
  the recovered point in memory only for that session.
- Also hidden while you're in a different dimension than the one you
  died in (the coordinates would be meaningless there), and while any
  other screen is open (inventory, chest, death screen, menus, ...) —
  except while chatting, so you can still read the line while typing a
  message.
- Hides itself along with the rest of the HUD when F1 (hide HUD) is
  toggled.
- 100% client-side: no server component, no network calls, no
  automation of any kind. It only reads information the vanilla client
  already receives from the server for the Recovery Compass feature.

## Known limitations

- Only the single most recent death is tracked (matching vanilla's own
  `getLastDeathPos()`), not a history. The same applies to the recovered
  point remembered locally: only the single most recently recovered
  point is kept, across all worlds/servers, not one per world. If you
  recover a death in world A and then recover a different death in
  world B, the record for A is overwritten; going back to world A, its
  (already-recovered) death point will show as not-yet-recovered again
  until you walk back to it (walking within 3 blocks hides it again, as
  usual — this never causes a death to wrongly stay hidden, only a
  harmless re-showing of one you'd already dealt with).
- If you die again at the exact same block coordinates, in the same
  dimension, in the same world/server as a death point you'd already
  recovered, this mod can't tell that apart from the old one (they look
  identical: same world + dimension + X/Y/Z), so the line stays hidden
  instead of reappearing for the new death. This should be rare in
  practice, but is more likely than it sounds in one case: deleting a
  singleplayer world and creating a new one with the same name reuses
  that same folder-name identity.
- On a multiplayer server, the "same world" identity is the server
  address you connected with; joining the same server through a
  different address/port (e.g. a different IP that resolves to the same
  server) would be treated as a different world, so a death recovered
  under one address wouldn't carry over to the other. Conversely, a
  proxy setup (e.g. Velocity/BungeeCord) where one address fronts
  multiple distinct backend servers would treat all of them as the same
  world, so the same-coordinates edge case above becomes more likely
  there.

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
