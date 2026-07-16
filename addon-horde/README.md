# GuildShelter Horde

Optional Bukkit-side addon for guild camp horde defense.

## MVP

- Manual start with `/gshorde start` while standing in a guild camp.
- Admin start with `/gshorde start <worldName>` from console or in game.
- Optional weekly queue that auto-assigns GuildShelter camps to trigger windows.
- Wave-based mob spawning on roads around unlocked camp chunks.
- Boss bar shows wave, spawn progress, and wave timer.
- Success rewards nearby players with XP.
- Camp dissolve stops the active session.

## Direction

This addon is intentionally server-only. No client mod is required.
The first version should stay simple:

- one active horde globally
- one camp selected per trigger window
- fixed waves, configurable mob pool
- no block destruction, no custom client UI
- all gameplay state lives in this addon

## Architecture Notes

Horde gameplay uses real server entities, not fake client packets. Packet-only
monsters are useful for visuals, but they do not provide authoritative combat,
pathfinding, death events, Bukkit integration, or anti-cheat friendly hit
detection.

The addon keeps the expensive parts bounded:

- `HordeSession` owns one camp world's wave state and spawned entity UUIDs.
- `HordeEntityMarker` tags horde mobs with both a scoreboard tag and persistent
  data, so future listeners can identify them without depending on session
  internals.
- `HordeCombatListener` is the compatibility layer for GuildShelter combat
  flags. It can let tagged horde mobs bypass `pve`, `pve-monster`, and
  `pve-player` while still respecting `invincible` by default.
- `max-alive-mobs` caps simultaneously alive horde entities. Increasing total
  wave size should not mean unbounded live entities.

This keeps future work separated: wave rules, mob abilities, rewards, and
territory/flag compatibility can evolve independently.

## Triggers

Camp creation does not start a horde. The supported triggers are:

- Manual: `/gshorde start` while standing in a guild camp.
- Manual by world: `/gshorde start <worldName>`.
- Weekly schedule in `config.yml`, based on GuildShelter camps.

Weekly scheduling does not use the backing guild plugin's guild list. It calls
`GuildShelterAPI#guildCamps()`, sorts those camp refs by `id/worldName`, and
uses a round-robin pointer to pick one camp for each configured weekly trigger
window.

The scheduler is serialized:

- one trigger window queues at most one camp
- the manager allows only one active horde globally
- if a horde is already active, the queued camp waits until a later scheduler tick
- newly created camps enter the rotation on future windows
- dissolved or unloaded camp worlds are dropped before they start

Example:

```yaml
weekly:
  enabled: true
  timezone: Asia/Shanghai
  slots:
    - label: saturday-prime
      day: SATURDAY
      time: "20:00"
    - label: sunday-prime
      day: SUNDAY
      time: "20:00"
```

With two slots, the addon can start at most two camp hordes per week. If there
are more camps than slots, the rotation continues across later weeks instead of
starting multiple camps at the same time.

## Difficulty Handling

Minecraft refuses to spawn hostile mobs in Peaceful worlds. By default this
addon treats difficulty as horde session state: when a horde starts it records
the camp world's current difficulty, temporarily switches the world to the
configured difficulty, and restores the previous difficulty when the session
ends.

This behavior is controlled by:

```yaml
difficulty:
  force-during-horde: true
  forced-difficulty: HARD
  restore-after-horde: true
```

If `force-during-horde` is disabled, Peaceful camp worlds will reject horde
startup instead of being changed automatically.

## Next steps

- Add loot tables or reward commands.
- Add stronger mobs for higher guild levels.
- Add optional camp-core defense instead of pure survival.
