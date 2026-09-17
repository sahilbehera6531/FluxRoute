package com.routing.dynamic_graph_routing.model;

import com.routing.dynamic_graph_routing.util.DistanceCalculator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IntersectionNode {

    @EqualsAndHashCode.Include
    private final String id;

    private final double latitude;
    private final double longitude;

    /**
     * Calculates (true) straight-line distance in kilometers using the HAVERSINE formula.
     * Acts as the admissible heuristic h(n) in the A* algorithm.
     */
    public double distanceTo(IntersectionNode other) {
        return DistanceCalculator.haversineDistanceKm(
                this.latitude, this.longitude,
                other.latitude, other.longitude
        );
    }
}