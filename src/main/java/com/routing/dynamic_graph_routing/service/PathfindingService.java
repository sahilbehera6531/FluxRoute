package com.routing.dynamic_graph_routing.service;

import com.routing.dynamic_graph_routing.dto.RouteResult;
import com.routing.dynamic_graph_routing.model.IntersectionNode;
import com.routing.dynamic_graph_routing.model.RoadEdge;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PathfindingService {

    private final GraphManager graphManager;

    public PathfindingService(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    /**
     * Executes A* search from source to target.
     *
     * @param sourceId Origin intersection ID
     * @param targetId Destination intersection ID
     * @return RouteResult containing path, total distance, duration/cost, and evaluated node count
     */
    public RouteResult findShortestPath(String sourceId, String targetId) {
        IntersectionNode source = graphManager.getNode(sourceId);    //source is an object reference variable holding the reference (memory pointer) to an instance of the IntersectionNode class. (tracing - see word doc)
        IntersectionNode target = graphManager.getNode(targetId);

        if (source == null || target == null) {
            return RouteResult.empty();
        }

        if (sourceId.equals(targetId)) {
            return new RouteResult(List.of(sourceId), 0.0, 0.0, 1); // source and target(destination) same
        }

        Map<String, Double> gScores = new HashMap<>();              //g(n)
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, RoadEdge> edgeUsed = new HashMap<>();

        PriorityQueue<SearchNode> openSet = new PriorityQueue<>();  //Evaluates nodes using a Min-Heap (PriorityQueue<SearchNode>) ordered strictly by f(n) = g(n) + h(n) in O(log V) time. Ordering/Sorting is done by SearchNode method
        Set<String> closedSet = new HashSet<>();                    //to avoid processing cycles or already-settled nodes.

        // Initialize the source node in the open set with g(n) = 0 and h(n) = heuristic estimate to target
        gScores.put(sourceId, 0.0);
        double initialH = calculateHeuristic(source, target);
        openSet.add(new SearchNode(sourceId, initialH));

        int nodesEvaluated = 0;

        while (!openSet.isEmpty()) {
            SearchNode current = openSet.poll();
            String currentId = current.nodeId();

            if (closedSet.contains(currentId)) {
                continue;
            }

            nodesEvaluated++;

            if (currentId.equals(targetId)) {
                return reconstructPath(targetId, cameFrom, edgeUsed, nodesEvaluated);
            }

            closedSet.add(currentId);
            double currentG = gScores.getOrDefault(currentId, Double.POSITIVE_INFINITY);

            for (RoadEdge edge : graphManager.getNeighbors(currentId)) {
                // If road is marked blocked, skip it entirely
                if (edge.isBlocked()) {
                    continue;
                }

                String neighborId = edge.getTargetNodeId();

                if (closedSet.contains(neighborId)) {
                    continue;
                }

                // Dynamic traversal cost using getEffectiveCost()
                double edgeCost = edge.getEffectiveCost();

                if (Double.isInfinite(edgeCost)) {
                    continue;
                }

                double tentativeG = currentG + edgeCost;

                if (tentativeG < gScores.getOrDefault(neighborId, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighborId, currentId);
                    edgeUsed.put(neighborId, edge);
                    gScores.put(neighborId, tentativeG);

                    IntersectionNode neighborNode = graphManager.getNode(neighborId);
                    double hScore = calculateHeuristic(neighborNode, target);
                    double fScore = tentativeG + hScore;

                    openSet.add(new SearchNode(neighborId, fScore));
                }
            }
        }

        //If openSet empties without ever popping targetId, no viable path exists across the road network (e.g., islands or blocked corridors), returning an empty RouteResult.
        return RouteResult.empty();
    }

    private double calculateHeuristic(IntersectionNode current, IntersectionNode target) {
        if (current == null || target == null) {
            return 0.0;
        }
        // Great-circle distance in kilometers via Haversine
        return current.distanceTo(target);
    }

    private RouteResult reconstructPath(
            String targetId,
            Map<String, String> cameFrom,
            Map<String, RoadEdge> edgeUsed,
            int nodesEvaluated
    ) {
        LinkedList<String> path = new LinkedList<>();
        String curr = targetId;
        path.addFirst(curr);

        double totalDistanceKm = 0.0;
        double totalEffectiveCost = 0.0;

        while (cameFrom.containsKey(curr)) {
            RoadEdge edge = edgeUsed.get(curr);
            if (edge != null) {
                totalDistanceKm += edge.getBaseDistance();      // getter method for baseDistance
                totalEffectiveCost += edge.getEffectiveCost();  
            }
            curr = cameFrom.get(curr);
            path.addFirst(curr);
        }

        return new RouteResult(path, totalDistanceKm, totalEffectiveCost, nodesEvaluated);
    }

    /**
     * Internal state wrapper for PriorityQueue ordering.
     */
    private record SearchNode(String nodeId, double fScore) implements Comparable<SearchNode> {
        @Override
        public int compareTo(SearchNode other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }
}