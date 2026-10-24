# GuildShelter Xaero Map Bridge

Optional Bukkit-side addon for Xaero World Map territory highlights and
map-driven claiming.

## Runtime Shape

- Loads as an independent Bukkit plugin.
- Depends on the main `GuildShelter` plugin and its public `guildshelter-api`.
- Registers the `guildshelter:map` plugin-message channel.
- Sends territory snapshots with `GuildShelterAPI#mapSnapshot`.
- Handles map claim requests with `GuildShelterAPI#tryClaimMapChunk`.
- Refreshes players on join, world change, manor chunk unlock, and city chunk
  unlock events.

The paired client integration stays inside `guildshelter-client`; players should
not need a separate client mod per map integration.

## Wire Protocol

The protocol intentionally does not keep compatibility with the old
`CLAIM + chunkX + chunkZ` and `RESULT + ok + text` packets. This feature has not
shipped to production, so client and addon must both speak the structured v2
packets below.

All packets are Java `DataInputStream` / `DataOutputStream` byte order.

### Server To Client

`PLOTS = 0x01`

| Field | Type | Notes |
| --- | --- | --- |
| type | byte | `0x01` |
| worldName | UTF | Bukkit world name |
| originChunkX | int | Reserved by client, useful for debugging |
| originChunkZ | int | Reserved by client, useful for debugging |
| count | int | Number of following entries |
| chunkX | int | Repeated `count` times |
| chunkZ | int | Repeated `count` times |
| argb | int | Overlay color |
| kind | byte | `0` city, `1` own manor, `2` other manor |
| label | UTF | Display label |

`CLEAR = 0x02`

| Field | Type | Notes |
| --- | --- | --- |
| type | byte | `0x02` |
| worldName | UTF | Dimension/world to clear |

`CLAIM_RESULT = 0x20`

| Field | Type | Notes |
| --- | --- | --- |
| type | byte | `0x20` |
| action | byte | `0` claim |
| left | int | Requested chunk bounds |
| top | int | Requested chunk bounds |
| right | int | Requested chunk bounds |
| bottom | int | Requested chunk bounds |
| status | byte | `MapClaimStatus.ordinal()` |

### Client To Server

`CLAIM_REQUEST = 0x10`

| Field | Type | Notes |
| --- | --- | --- |
| type | byte | `0x10` |
| action | byte | `0` claim |
| left | int | Requested chunk bounds |
| top | int | Requested chunk bounds |
| right | int | Requested chunk bounds |
| bottom | int | Requested chunk bounds |

The request mirrors OpenPAC's claim-action model: action plus area bounds, with
the server doing authoritative validation and returning typed result codes.
GuildShelter currently accepts only one chunk per request, so `left == right`
and `top == bottom` are required. Multi-chunk area selection can be added later
without changing the packet shape.

### Validation Rules

- Drop packets for any channel other than `guildshelter:map`.
- Drop claim requests larger than 64 bytes.
- Drop unknown packet types and unknown actions.
- Drop inverted bounds where `left > right` or `top > bottom`.
- Return `NOT_CLAIMABLE` for multi-chunk areas until the server API supports
  area claims.
- Do not send localized server text in result packets; the client maps status
  codes to local display text.

### Status Order

The result `status` byte must match `MapClaimStatus.ordinal()`:

| Ordinal | Status |
| --- | --- |
| 0 | `SUCCESS` |
| 1 | `NO_MANOR` |
| 2 | `NOT_YOUR_PLOT` |
| 3 | `ALREADY_UNLOCKED` |
| 4 | `NO_QUOTA` |
| 5 | `NOT_ADJACENT` |
| 6 | `CITY_LEADER_ONLY` |
| 7 | `NOT_CLAIMABLE` |
| 8 | `NOT_IN_GUILD_WORLD` |
| 9 | `INVALID_PLAYER` |
