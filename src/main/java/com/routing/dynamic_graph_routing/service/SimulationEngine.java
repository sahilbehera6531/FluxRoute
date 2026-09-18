package com.routing.dynamic_graph_routing.service;

import com.routing.dynamic_graph_routing.dto.DisruptionEvent;
import com.routing.dynamic_graph_routing.dto.RouteResult;
import com.routing.dynamic_graph_routing.model.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationEngine {

    private final PathfindingService pathfindingService;
    private final FleetManager fleetManager;
    private final DynamicRerouteEngine rerouteEngine;

    /**
     * Spawns a test fleet across the road network and computes their initial routes.
     *
     * @param vehicleCount Total vehicles to deploy
     * @param nodeIds      Pool of valid intersection IDs to choose start/destinations from
     */
    public void initializeFleet(int vehicleCount, List<String> nodeIds) {
        if (nodeIds == null || nodeIds.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 distinct intersections to initialize a fleet.");
        }

        fleetManager.clearFleet();
        Random random = new Random(42); // deterministic seed for reproducibility

        log.info("Spawning {} vehicles across {} intersections...", vehicleCount, nodeIds.size());

        for (int i = 1; i <= vehicleCount; i++) {
            String vehicleId = "VEHICLE-" + i;
            String origin = nodeIds.get(random.nextInt(nodeIds.size()));
            String destination;
            do {
                destination = nodeIds.get(random.nextInt(nodeIds.size()));
            } while (destination.equals(origin));

            RouteResult initialRoute = pathfindingService.findShortestPath(origin, destination);
            List<String> path = initialRoute.isFound() ? initialRoute.path() : List.of(origin);

            Vehicle vehicle = new Vehicle(vehicleId, origin, destination, path);
            fleetManager.registerVehicle(vehicle);
        }

        log.info("Fleet initialized. Registered {} vehicles.", fleetManager.getFleetSize());
    }

    /**
     * Simulates one discrete time tick across the city.
     * Every vehicle advances to the next intersection along its planned path.
     */
    public void stepSimulation() {
        Collection<Vehicle> vehicles = fleetManager.getAllVehicles();

        for (Vehicle vehicle : vehicles) {
            List<String> currentRoute = vehicle.getCurrentRoute();
            if (currentRoute.size() <= 1) {
                // Vehicle has reached its destination or has no valid route
                continue;
            }

            // Current node is route[0], next node is route[1]
            String nextNode = currentRoute.get(1);
            List<String> remainingRoute = currentRoute.subList(1, currentRoute.size());

            // Move vehicle physically forward and adjust index
            vehicle.setCurrentNodeId(nextNode);
            fleetManager.updateVehicleRoute(vehicle.getVehicleId(), remainingRoute);
        }
    }

    /**
     * Triggers a disruption and benchmarks the rerouting response time.
     *
     * @param event The road incident to apply
     * @return Execution latency in milliseconds
     */
    public long triggerDisruptionBenchmark(DisruptionEvent event) {
        long startTime = System.nanoTime();
        int affectedCount = rerouteEngine.handleDisruption(event);
        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;

        log.info("Disruption benchmark finished: rerouted {} vehicles in {} ms", affectedCount, elapsedMillis);
        return elapsedMillis;
    }
}