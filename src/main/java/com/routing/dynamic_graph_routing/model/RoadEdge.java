package com.routing.dynamic_graph_routing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class RoadEdge {

    private final String targetNodeId;
    private final double baseDistance;
    
    // Dynamic fields modified by real-time events
    private double trafficMultiplier;
    private boolean isBlocked;

    /**
     * Standard constructor for initializing an open road segment.
     */
    public RoadEdge(String targetNodeId, double baseDistance) {
        this.targetNodeId = targetNodeId;
        this.baseDistance = baseDistance;
        this.trafficMultiplier = 1.0;
        this.isBlocked = false;
    }

    /**
     * Computes the dynamic traversal cost g(u, v) for the A* search path.
     * Returns infinity if the road is physically closed.
     */
    public double getEffectiveCost() {
        if (isBlocked) {
            return Double.POSITIVE_INFINITY;    // if road blocked, returns infinity to indicate that this path is not traversable, thus A* will not consider this edge in its pathfinding.
        }
        return baseDistance * trafficMultiplier;
    }
}