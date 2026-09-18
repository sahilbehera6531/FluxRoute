package com.routing.dynamic_graph_routing;

import com.routing.dynamic_graph_routing.dto.DisruptionEvent;
import com.routing.dynamic_graph_routing.model.Vehicle;
import com.routing.dynamic_graph_routing.service.FleetManager;
import com.routing.dynamic_graph_routing.service.GraphManager;
import com.routing.dynamic_graph_routing.service.SimulationEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SimulationEngineTest {

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private FleetManager fleetManager;

    @Autowired
    private GraphManager graphManager;

    @Test
    @DisplayName("Verify parallel rerouting clears affected vehicles from disrupted edge")
    void testDisruptionAndConcurrentReroute() {
        // Grab all loaded intersection IDs
        List<String> nodeIds = new ArrayList<>(graphManager.getAllNodes().keySet());
        assertTrue(nodeIds.size() >= 2, "Graph must contain nodes from map-data.json");

        // =========================================================================
        // STRESS TEST KNOB 1: FLEET SIZE
        // Adjust the first parameter (50) to 500, 2000, or 10000 for heavy volume.
        // =========================================================================
        int totalVehicles = 5000000; // 5 million vehicles for stress testing
        simulationEngine.initializeFleet(totalVehicles, nodeIds);
        assertEquals(totalVehicles, fleetManager.getFleetSize());

        // Find a populated road corridor from the inverted index
        String sourceNode = null;
        String targetNode = null;
        Set<String> affectedVehicles = Set.of();

        // Check primary test corridor (JVLR West -> Powai)
        Set<String> vehiclesOnCorridor = fleetManager.getVehiclesOnEdge("NODE_JVLR_WEST", "NODE_POWAI");
        if (!vehiclesOnCorridor.isEmpty()) {
            sourceNode = "NODE_JVLR_WEST";
            targetNode = "NODE_POWAI";
            affectedVehicles = vehiclesOnCorridor;
        } else {
            // Dynamic fallback: locate the first active edge that has assigned vehicles
            for (String u : nodeIds) {
                for (String v : nodeIds) {
                    Set<String> onRoad = fleetManager.getVehiclesOnEdge(u, v);
                    if (!onRoad.isEmpty()) {
                        sourceNode = u;
                        targetNode = v;
                        affectedVehicles = onRoad;
                        break;
                    }
                }
                if (!affectedVehicles.isEmpty()) {
                    break;
                }
            }
        }

        assertNotNull(sourceNode, "Expected at least one active edge with assigned vehicles");
        assertFalse(affectedVehicles.isEmpty(), "Target edge must have assigned vehicles to test rerouting");

        // Inject complete road blockage disruption
        DisruptionEvent blockage = DisruptionEvent.blockRoad(
                sourceNode,
                targetNode,
                "Monsoon Flooding Simulation"
        );

        long latencyMs = simulationEngine.triggerDisruptionBenchmark(blockage);
        assertTrue(latencyMs >= 0, "Benchmark execution latency should be non-negative");

        // Verify Inverted Index: the disrupted edge must have 0 vehicles assigned
        Set<String> vehiclesAfterReroute = fleetManager.getVehiclesOnEdge(sourceNode, targetNode);
        assertEquals(0, vehiclesAfterReroute.size(),
                "Inverted index must show 0 vehicles scheduled on blocked road after rerouting");

        // Verify that all rerouted vehicles have valid detour routes without the blocked edge
        for (String vehicleId : affectedVehicles) {
            Vehicle vehicle = fleetManager.getVehicle(vehicleId);
            assertNotNull(vehicle);
            List<String> newRoute = vehicle.getCurrentRoute();

            for (int i = 0; i < newRoute.size() - 1; i++) {
                boolean usesBlockedEdge = newRoute.get(i).equals(sourceNode)
                        && newRoute.get(i + 1).equals(targetNode);
                assertFalse(usesBlockedEdge,
                        "Vehicle " + vehicleId + " route still contains the blocked edge!");
            }
        }
    }
}