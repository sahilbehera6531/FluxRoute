package com.routing.dynamic_graph_routing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class Vehicle {

    private final String vehicleId;
    private String currentNodeId;
    private final String destinationNodeId;
    
    // Ordered sequence of node IDs representing the active path
    private List<String> currentRoute;

    public Vehicle(String vehicleId, String currentNodeId, String destinationNodeId) {
        this.vehicleId = vehicleId;
        this.currentNodeId = currentNodeId;
        this.destinationNodeId = destinationNodeId;
        this.currentRoute = Collections.emptyList();
    }

    /**
     * Atomically replaces the current active path when a reroute event completes.
     */
    public synchronized void updateRoute(List<String> newRoute) {
        this.currentRoute = List.copyOf(newRoute);
    }

    /**
     * Retrieves an unmodifiable view of the current route to prevent external mutations.
     */
    public synchronized List<String> getCurrentRoute() {
        return currentRoute;
    }
}