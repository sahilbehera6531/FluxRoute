package com.routing.dynamic_graph_routing.model;

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

    // Calculate the Euclidean distance between this node and another node. Euclidean distance is a straight-line distance between two points in Euclidean space. Euclidean distance acts as ADMISSIBLE h(n) in A* algorithm.
    public double distanceTo(IntersectionNode other) { 
        double deltaLat = this.latitude - other.latitude;  
        double deltaLon = this.longitude - other.longitude; 
        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon); // heuristic value h(n) 
    }
}