# Death Log

A small Fabric mod: every time you die, it records when, where, and the
vanilla death message ("Steve was slain by Zombie", etc.), so you can look
back at your last several deaths with `/deathlog`. No gravestone, no
teleporting, no map — just a history you can read.

**AI-assisted development disclosure**: this mod's code was written with
AI assistance (Claude). It does not reuse code or assets from any other
mod.

## What it does

- Listens for the vanilla death event server-side and appends one entry
  per death to that player's history: a wall-clock timestamp, the
  dimension, the block position where they died, and the death message
  text, rendered in the **server's** language (English on a dedicated
  server unless its `server.properties`/JVM locale says otherwise; on a
  singleplayer world, whatever the client's language was at the moment of
  death). This is not necessarily the language an individual player's own
  client shows the same death in chat — see Known limitations below.
- `/deathlog` prints your own most recent deaths (up to 10), newest
  first, each numbered and formatted like:
  ```
  #1  2026-09-30 12:34 UTC  the_nether (12, 40, -8)  — Steve was pricked to death by a Cactus
  ```
  Timestamps are always shown in UTC regardless of the server's local
  timezone, so the log reads the same for every player.
- Each player's history is capped at their 20 most recent deaths; older
  ones are dropped automatically.
- History is stored per world/server, in a `deathlog.json` file inside
  that world's own save folder — copying, backing up, or deleting the
  world takes its death log with it. A missing or corrupted file is
  treated as an empty log rather than crashing the server; a write
  failure is logged and the mod keeps working in memory for the rest of
  the session.
- Only reads data the server already computes for the vanilla death
  message and death screen; performs no automation and does not bypass
  anything a server could reasonably consider a cheat.

## Which side does the recording?

Deaths are server-authoritative, same as the vanilla death message
itself:

- **Singleplayer**: the integrated server runs in the same process as
  your client, so the mod works exactly as described above as soon as
  it's installed — nothing else to do.
- **Dedicated multiplayer server**: install this mod on the **server**;
  `/deathlog` is a normal server command, so it also needs the mod on
  the server to exist at all (players do not need it client-side).

## Known limitations

- **Death messages are recorded in the server's language, not each
  viewer's own.** Vanilla normally sends death messages as untranslated
  keys so every client's chat shows them in that player's own language;
  this mod instead resolves the message to plain text once, server-side,
  before storing it, so `/deathlog` always shows that one language
  (typically English on a dedicated server) no matter who runs the
  command or what language their client is set to. A death caused by
  another mod's damage type without a translation registered on the
  server will show as a raw translation key (e.g. `death.attack.foo`)
  instead of readable text.

### Not yet verified on real hardware

This mod was built and tested in a headless container: `./gradlew build`
(including the unit tests below) passes, and both a headless dedicated
server (`./gradlew runServer`) and a headless client (`./gradlew
runClient` under Xvfb) start up without errors — `/deathlog` was
confirmed, from the server console, to register correctly and reply with
"can only be used by a player" when run by a non-player source (the
server console). What has **not** been exercised end-to-end is an actual
player death: no live player connected in this environment to die and
run `/deathlog` afterwards. Specifically unverified:

- The exact formatting of `/deathlog`'s output as seen in a real chat
  window (line wrapping, color).
- That the recorded death message text matches what appears on the
  vanilla death screen/chat in every case (e.g. deaths with no attacker,
  falling, drowning, fire).
- Multiplayer with more than one player, and a dedicated server
  restart/crash between deaths (the persistence logic itself is
  unit-tested against real temp-directory files, including a
  reload-after-restart test, a full-vs-capped history test, and
  corrupt/missing-file recovery, but not against an actual dedicated
  server process restarting mid-session).

Requires Fabric Loader and Fabric API for Minecraft 1.21.11.
