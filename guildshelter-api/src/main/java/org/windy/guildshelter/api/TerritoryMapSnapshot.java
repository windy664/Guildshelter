package org.windy.guildshelter.api;

import java.util.List;

/**
 * Territory chunks visible in one GuildShelter world for one viewer.
 */
public record TerritoryMapSnapshot(String worldName, int originChunkX, int originChunkZ,
                                   List<TerritoryMapChunk> chunks) {

    public TerritoryMapSnapshot {
        chunks = List.copyOf(chunks);
    }
}
