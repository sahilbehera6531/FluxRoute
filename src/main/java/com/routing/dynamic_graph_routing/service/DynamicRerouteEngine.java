package com.routing.dynamic_graph_routing.service;

import com.routing.dynamic_graph_routing.dto.DisruptionEvent;
import com.routing.dynamic_graph_routing.dto.RouteResult;
import com.routing.dynamic_graph_routing.model.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRerouteEngine {

    private final GraphManager graphManager;
    private final PathfindingService pathfindingService;
    private final FleetManager fleetManager;

    private final ExecutorService rerouteExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    public int handleDisruption(DisruptionEvent event) {
        log.info("Processing disruption on edge {} -> {}: blocked={}, congestion={}",
                event.getFromNodeId(), event.getToNodeId(), event.isBlocked(), event.getCongestionFactor());

        // 1. Update graph in GraphManager
        if (event.isBlocked()) {
            graphManager.updateRoadClosure(event.getFromNodeId(), event.getToNodeId(), true);
        } else {
            graphManager.updateEdgeTraffic(event.getFromNodeId(), event.getToNodeId(), event.getCongestionFactor());
        }

        // 2. Fetch impacted vehicles via inverted index
        Set<String> affectedVehicleIds = fleetManager.getVehiclesOnEdge(event.getFromNodeId(), event.getToNodeId());
        if (affectedVehicleIds.isEmpty()) {
            log.info("Zero vehicles impacted by disruption on {} -> {}", event.getFromNodeId(), event.getToNodeId());
            return 0;
        }

        log.info("Rerouting {} impacted vehicles concurrently...", affectedVehicleIds.size());

        // 3. Parallel recalculations
        List<CompletableFuture<Void>> rerouteTasks = affectedVehicleIds.stream()
                .map(vehicleId -> CompletableFuture.runAsync(() -> rerouteVehicle(vehicleId), rerouteExecutor))
                .toList();

        // 4. Wait for all reroutes to complete
        CompletableFuture.allOf(rerouteTasks.toArray(new CompletableFuture[0])).join();

        log.info("Successfully rerouted all {} vehicles.", affectedVehicleIds.size());
        return affectedVehicleIds.size();
    }

    private void rerouteVehicle(String vehicleId) {
        Vehicle vehicle = fleetManager.getVehicle(vehicleId);
        if (vehicle == null) {
            return;
        }

        String start = vehicle.getCurrentNodeId();
        String destination = vehicle.getDestinationNodeId();

        RouteResult result = pathfindingService.findShortestPath(start, destination);

        // Uses your record's custom helper isFound() or direct path() accessor
        if (result == null || !result.isFound()) {
            log.warn("Vehicle {} has no viable alternate path to {}", vehicleId, destination);
            return;
        }

        // Java Record accessor is .path()
        List<String> newRoute = result.path();
        fleetManager.updateVehicleRoute(vehicleId, newRoute);
        log.debug("Vehicle {} rerouted: {}", vehicleId, newRoute);
    }
}