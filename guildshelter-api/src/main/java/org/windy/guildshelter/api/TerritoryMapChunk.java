package org.windy.guildshelter.api;

/**
 * One chunk entry for map overlays.
 */
public record TerritoryMapChunk(int chunkX, int chunkZ, TerritoryMapKind kind, String label) {
}
