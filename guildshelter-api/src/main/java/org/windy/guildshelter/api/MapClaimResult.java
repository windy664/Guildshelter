package org.windy.guildshelter.api;

/**
 * Result of a map-driven chunk claim/unlock request.
 */
public record MapClaimResult(MapClaimStatus status, boolean success, String messageKey) {

    public static MapClaimResult of(MapClaimStatus status) {
        return new MapClaimResult(status, status.success(), "map.claim_" + status.messageSuffix());
    }
}
