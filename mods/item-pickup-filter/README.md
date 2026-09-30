# Item Pickup Filter

A small Fabric mod: items on a configurable blacklist are never
auto-picked-up when you walk over them, so your inventory doesn't fill up
with junk (rotten flesh, poisonous potatoes, spider eyes, ...) while you're
fighting or mining.

**AI-assisted development disclosure**: this mod's code was written with
AI assistance (Claude). It does not reuse code or assets from any other
mod.

## What it does

- Cancels the vanilla pickup of any item whose id is listed in
  `config/item-pickup-filter.json` (created on first run with a default
  list: `minecraft:rotten_flesh`, `minecraft:poisonous_potato`,
  `minecraft:spider_eye`). Everything else is picked up exactly as
  vanilla does.
- The blacklisted item stays on the ground; nothing is deleted or
  destroyed. Remove it from the config (and restart) and it can be picked
  up normally again.
- No keybinding, no HUD, no command: just edit the config file and
  restart to change the list (it's only read at startup).

## Configuration

Edit `config/item-pickup-filter.json`:

```json
{
  "blacklist": [
    "minecraft:rotten_flesh",
    "minecraft:poisonous_potato",
    "minecraft:spider_eye"
  ]
}
```

Add or remove item ids (the same ids used in `/give`, e.g.
`minecraft:gunpowder` or a modded item's id). A malformed entry is skipped
with a warning in the log; the rest of the list still applies. IDs are
matched by name, not by resolving them against the item registry at
startup, so a modded item's id works correctly regardless of mod load
order. If the whole file is missing or unreadable, the mod falls back to
the default list above rather than failing to start.

## Which side does the filtering?

Vanilla's item-pickup logic is authoritative on whichever side is running
the world logic:

- **Singleplayer**: the integrated server runs in the same process as your
  client, so the mod works exactly as described above as soon as it's
  installed — nothing else to do.
- **Dedicated multiplayer server**: install this mod on the **server**;
  players don't need it client-side for the filtering itself to work
  (though Fabric server mods are usually easiest to manage if every
  player also has it, so mod lists match).
- Installing it **only on the client** while connected to a server that
  doesn't have it has no effect on pickup (the actual pickup decision is
  made server-side); it is harmless either way, it just won't filter
  anything in that case.

Requires Fabric Loader for Minecraft 1.21.11 (no Fabric API dependency).
