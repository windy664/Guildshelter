package org.windy.guildshelter.api;

/**
 * Stable public result codes for map-driven claim attempts.
 *
 * <p>The Xaero map bridge sends these codes on the wire by ordinal. Keep the
 * existing order stable; append new values at the end.
 */
public enum MapClaimStatus {
    SUCCESS(true, "success"),
    NO_MANOR(false, "no_manor"),
    NOT_YOUR_PLOT(false, "not_your_plot"),
    ALREADY_UNLOCKED(false, "already_unlocked"),
    NO_QUOTA(false, "no_quota"),
    NOT_ADJACENT(false, "not_adjacent"),
    CITY_LEADER_ONLY(false, "city_leader_only"),
    NOT_CLAIMABLE(false, "not_claimable"),
    NOT_IN_GUILD_WORLD(false, "not_in_guild_world"),
    INVALID_PLAYER(false, "invalid_player");

    private final boolean success;
    private final String messageSuffix;

    MapClaimStatus(boolean success, String messageSuffix) {
        this.success = success;
        this.messageSuffix = messageSuffix;
    }

    public boolean success() {
        return success;
    }

    public String messageSuffix() {
        return messageSuffix;
    }
}
