package com.routing.dynamic_graph_routing.service;

import com.routing.dynamic_graph_routing.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FleetManager {

    // Master vehicle registry: vehicleId -> Vehicle
    private final Map<String, Vehicle> activeVehicles = new ConcurrentHashMap<>();

    // Inverted index: "fromNode->toNode" -> Set of vehicleIds currently traversing that edge
    private final Map<String, Set<String>> edgeToVehicles = new ConcurrentHashMap<>();

    /**
     * Registers a new vehicle into the fleet.
     */
    public void registerVehicle(Vehicle vehicle) {
        activeVehicles.put(vehicle.getVehicleId(), vehicle);
        indexVehicleRoute(vehicle.getVehicleId(), vehicle.getCurrentRoute());
    }

    /**
     * Atomically updates a vehicle's route and adjusts the inverted index.
     */
    public void updateVehicleRoute(String vehicleId, List<String> newRoute) {
        Vehicle vehicle = activeVehicles.get(vehicleId);
        if (vehicle == null) {
            return;
        }

        // Remove old edge bindings before updating
        List<String> oldRoute = vehicle.getCurrentRoute();
        deindexVehicleRoute(vehicleId, oldRoute);

        // Update the vehicle's internal state
        vehicle.updateRoute(newRoute);

        // Register new edge bindings
        indexVehicleRoute(vehicleId, newRoute);
    }

    /**
     * Retrieves all vehicle IDs currently scheduled to traverse a given directed edge in O(1) time.
     *
     * @param fromNode Origin intersection ID
     * @param toNode Destination intersection ID
     * @return Set of affected vehicle IDs
     */
    public Set<String> getVehiclesOnEdge(String fromNode, String toNode) {
        String edgeKey = buildEdgeKey(fromNode, toNode);
        Set<String> vehicles = edgeToVehicles.get(edgeKey);
        if (vehicles == null || vehicles.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(vehicles));
    }

    public Vehicle getVehicle(String vehicleId) {
        return activeVehicles.get(vehicleId);
    }

    public Collection<Vehicle> getAllVehicles() {
        return Collections.unmodifiableCollection(activeVehicles.values());
    }

    public int getFleetSize() {
        return activeVehicles.size();
    }

    public void clearFleet() {
        activeVehicles.clear();
        edgeToVehicles.clear();
    }

    // --- Internal Inverted Index Helpers ---

    private void indexVehicleRoute(String vehicleId, List<String> route) {
        if (route == null || route.size() < 2) {
            return;
        }
        for (int i = 0; i < route.size() - 1; i++) {
            String edgeKey = buildEdgeKey(route.get(i), route.get(i + 1));
            edgeToVehicles
                    .computeIfAbsent(edgeKey, k -> ConcurrentHashMap.newKeySet())
                    .add(vehicleId);
        }
    }

    private void deindexVehicleRoute(String vehicleId, List<String> route) {
        if (route == null || route.size() < 2) {
            return;
        }
        for (int i = 0; i < route.size() - 1; i++) {
            String edgeKey = buildEdgeKey(route.get(i), route.get(i + 1));
            Set<String> vehicles = edgeToVehicles.get(edgeKey);
            if (vehicles != null) {
                vehicles.remove(vehicleId);
                if (vehicles.isEmpty()) {
                    edgeToVehicles.remove(edgeKey, Collections.emptySet());
                }
            }
        }
    }

    private String buildEdgeKey(String fromNode, String toNode) {
        return fromNode + "->" + toNode;                            // eg "nodeA->nodeB" eg "BANDRA->ANDHERI"
    }
}