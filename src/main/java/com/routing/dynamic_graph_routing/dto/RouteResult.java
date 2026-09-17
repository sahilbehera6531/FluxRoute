package com.routing.dynamic_graph_routing.dto;

import java.util.List;

/**
 * Immutable representation of a calculated path across the road network.
 *
 * @param path             Ordered sequence of intersection node IDs from origin to destination
 * @param totalDistanceKm  Cumulative geographical distance along the edges in kilometers
 * @param totalDurationMin Estimated travel duration accounting for base speed and congestion multipliers
 * @param nodesEvaluated   Total number of vertices polled from the priority queue (efficiency metric)
 */
public record RouteResult(
        List<String> path,
        double totalDistanceKm,
        double totalDurationMin,
        int nodesEvaluated
) {
    public static RouteResult empty() {
        return new RouteResult(List.of(), 0.0, 0.0, 0);
    }

    public boolean isFound() {
        return path != null && !path.isEmpty();
    }
}