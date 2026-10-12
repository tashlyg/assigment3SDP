package edu.hotel.gateway.notification;

/** Immutable results of the most recent batch call. */
public record BatchSummary(int total, int successful, int failed) {
    public BatchSummary {
        if (total < 0 || successful < 0 || failed < 0 || successful + failed != total) {
            throw new IllegalArgumentException("Batch counts must be nonnegative and add up to total");
        }
    }
}
