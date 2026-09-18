package com.routing.dynamic_graph_routing.controller;

import com.routing.dynamic_graph_routing.dto.DisruptionEvent;
import com.routing.dynamic_graph_routing.model.Vehicle;
import com.routing.dynamic_graph_routing.service.FleetManager;
import com.routing.dynamic_graph_routing.service.GraphManager;
import com.routing.dynamic_graph_routing.service.SimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class RoutingSimulationController {

    private final SimulationEngine simulationEngine;
    private final FleetManager fleetManager;
    private final GraphManager graphManager;

    /**
     * Spawns a fleet of test vehicles across known graph intersections.
     * Example: POST /api/simulation/init-fleet?count=100
     */
    @PostMapping("/init-fleet")
    public ResponseEntity<Map<String, Object>> initializeFleet(@RequestParam(defaultValue = "50") int count) {
        List<String> nodeIds = new ArrayList<>(graphManager.getAllNodes().keySet());
        
        if (nodeIds.size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient nodes in graph to initialize fleet."));
        }

        simulationEngine.initializeFleet(count, nodeIds);

        return ResponseEntity.ok(Map.of(
                "message", "Fleet initialized successfully",
                "fleetSize", fleetManager.getFleetSize(),
                "nodePoolSize", nodeIds.size()
        ));
    }

    /**
     * Injects a road disruption (closure or traffic multiplier) and triggers parallel rerouting.
     * Example Body:
     * {
     *   "fromNodeId": "NODE_JVLR_WEST",
     *   "toNodeId": "NODE_POWAI",
     *   "blocked": true,
     *   "congestionFactor": 0.0,
     *   "description": "Monsoon waterlogging near Powai Lake"
     * }
     */
    @PostMapping("/disrupt")
    public ResponseEntity<Map<String, Object>> injectDisruption(@RequestBody DisruptionEvent event) {
        long executionTimeMs = simulationEngine.triggerDisruptionBenchmark(event);

        return ResponseEntity.ok(Map.of(
                "disruptedEdge", event.getFromNodeId() + " -> " + event.getToNodeId(),
                "blocked", event.isBlocked(),
                "congestionFactor", event.getCongestionFactor(),
                "rerouteExecutionTimeMs", executionTimeMs
        ));
    }

    /**
     * Advances all active vehicles one hop along their assigned route.
     */
    @PostMapping("/step")
    public ResponseEntity<Map<String, Object>> stepSimulation() {
        simulationEngine.stepSimulation();
        return ResponseEntity.ok(Map.of("message", "Advanced simulation by 1 step"));
    }

    /**
     * Inspects the current state of all active vehicles.
     */
    @GetMapping("/vehicles")
    public ResponseEntity<Collection<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(fleetManager.getAllVehicles());
    }

    /**
     * Checks how many vehicles are currently scheduled across a specific road segment.
     */
    @GetMapping("/edge-traffic")
    public ResponseEntity<Map<String, Object>> getEdgeTraffic(
            @RequestParam String fromNodeId,
            @RequestParam String toNodeId) {
        Set<String> vehiclesOnRoad = fleetManager.getVehiclesOnEdge(fromNodeId, toNodeId);
        return ResponseEntity.ok(Map.of(
                "edge", fromNodeId + " -> " + toNodeId,
                "vehicleCount", vehiclesOnRoad.size(),
                "vehicles", vehiclesOnRoad
        ));
    }
}