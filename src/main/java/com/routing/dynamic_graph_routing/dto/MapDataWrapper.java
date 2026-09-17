package com.routing.dynamic_graph_routing.dto;

import java.util.List;

/**
 * Top-level container representing the entire map-data.json structure.
 */
public record MapDataWrapper(
        List<NodeData> intersections,
        List<RoadData> roads
) {
    /**
     * Maps to each element in the "intersections" array.
     */
    public record NodeData(
            String id,
            String name,
            double latitude,
            double longitude
    ) {}

    /**
     * Maps to each element in the "roads" array.
     * Note: "bidirectional" defaults to true if omitted or not explicitly set.
     */
    public record RoadData(
            String id,
            String sourceNodeId,
            String targetNodeId,
            double baseDistanceKm,
            double speedLimitKmh,
            boolean bidirectional
    ) {}
}