# Chest Search

A small Fabric client-side mod: press **Ctrl+F** while any vanilla
container is open (chest, barrel, shulker box, ender chest, furnace,
hopper, dispenser, the crafting table, your own inventory, ...) to bring
up a one-line search box. Items whose name matches get a green
highlight; everything else with an item in it gets dimmed, so you can
spot what you're looking for at a glance instead of reading every
tooltip by hand.

**AI-assisted development disclosure**: this mod's code was written with
AI assistance (Claude). It does not reuse code or assets from any other
mod.

## What it does

- Hooks the single vanilla `HandledScreen` base class that every
  inventory-style screen extends, so it works uniformly across chests,
  double chests, barrels, shulker boxes, furnaces, hoppers, dispensers/
  droppers, the crafting table and the player's own inventory, without
  needing a separate mixin per container type.
- **Ctrl+F** toggles the search box on/off. While it's on, typing
  filters live: matching stacks get a translucent green highlight
  behind them, non-empty non-matching stacks get dimmed. Toggling off
  (Ctrl+F again) or pressing **Escape** clears the query and turns
  highlighting off.
- Matching is a case-insensitive substring match against the item's
  display name (so `"apple"` matches "Golden Apple" and "Enchanted
  Golden Apple"), and also against the item's registry id (e.g.
  `iron_ingot`). This means a plain English word like `"iron"` still
  finds Iron Ingot even when the game's language is set to something
  else and the display name isn't in English or ASCII. A query typed
  with spaces is also matched against the id with those spaces treated
  as `_` (so `"iron ingot"` matches the id `iron_ingot` too).
- While the search box is active, keys are consumed by the search box
  instead of triggering vanilla hotbar-swap/quick-move, so typing digits
  to search doesn't also move items around.
- 100% client-side: no server component, no network calls, no
  automation. It never moves, takes or affects any item — it only reads
  what's already displayed and draws an overlay, so it isn't a cheat any
  server could reasonably flag.

## Known limitations

- The Creative inventory is intentionally left alone: it already has
  its own vanilla search tab, and its own hotkeys (e.g. "t" to switch
  tabs) would otherwise collide with typing into this mod's search box.
- Text entry uses the real character-input event, so it should follow
  whatever keyboard layout the OS is set to (not just US-QWERTY). IME
  composition input (e.g. Japanese) has not been tested in this
  headless build environment; typing an actual Japanese query has not
  been verified. Real-machine testing on 0.1.1 found that with the
  game's display language set to Japanese, a plain English query like
  `"iron"` matched nothing, because only the localized display name was
  checked. 0.1.2 adds matching against the item's id, so the same query
  now finds it regardless of display language (checked in the running
  game on 2026-09-29 with the language set to Japanese).
- Only Ctrl+F toggles the search box; macOS's Cmd+F is not recognized.

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
