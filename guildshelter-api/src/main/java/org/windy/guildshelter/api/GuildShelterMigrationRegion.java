package org.windy.guildshelter.api;

import java.util.UUID;

/**
 * A GuildShelter manor region that can be used by migration addons.
 *
 * @param owner        manor owner
 * @param guild        owning guild
 * @param worldName    Bukkit world name
 * @param slot         manor slot in the guild camp
 * @param minChunkX    minimum chunk X, inclusive
 * @param minChunkZ    minimum chunk Z, inclusive
 * @param maxChunkX    maximum chunk X, inclusive
 * @param maxChunkZ    maximum chunk Z, inclusive
 * @param sideChunks   square side length in chunks
 * @param manorLevel   manor level
 * @param source       human-readable source label for diagnostics
 */
public record GuildShelterMigrationRegion(UUID owner, GuildRef guild, String worldName, int slot,
                                          int minChunkX, int minChunkZ,
                                          int maxChunkX, int maxChunkZ,
                                          int sideChunks, int manorLevel,
                                          String source) {
}
