package com.routing.dynamic_graph_routing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DisruptionEvent {

    private String fromNodeId;
    private String toNodeId;

    // true if the road is completely closed/blocked
    private boolean blocked;

    // Traffic congestion multiplier: 1.0 = normal, 2.5 = heavy traffic, etc.
    private double congestionFactor;

    // Short descriptive reason (e.g., "Waterlogging", "Accident", "VIP Movement")
    private String description;

    /**
     * Factory method for creating a complete road blockage event.
     */
    public static DisruptionEvent blockRoad(String fromNodeId, String toNodeId, String description) {
        return new DisruptionEvent(fromNodeId, toNodeId, true, Double.POSITIVE_INFINITY, description);
    }

    /**
     * Factory method for creating a traffic congestion event.
     */
    public static DisruptionEvent congestion(String fromNodeId, String toNodeId, double factor, String description) {
        return new DisruptionEvent(fromNodeId, toNodeId, false, factor, description);
    }

    /**
     * Factory method for clearing a disruption and restoring normal flow.
     */
    public static DisruptionEvent clear(String fromNodeId, String toNodeId) {
        return new DisruptionEvent(fromNodeId, toNodeId, false, 1.0, "Cleared");
    }
}