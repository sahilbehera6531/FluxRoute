package com.routing.dynamic_graph_routing.service;

import com.routing.dynamic_graph_routing.model.IntersectionNode;
import com.routing.dynamic_graph_routing.model.RoadEdge;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class GraphManager {

    // Node lookup table: Node ID -> IntersectionNode
    private final Map<String, IntersectionNode> nodes = new HashMap<>();

    // Adjacency List: Source Node ID -> Outbound RoadEdges
    private final Map<String, List<RoadEdge>> adjacencyList = new HashMap<>();

    // Concurrency lock: Shared for readers, exclusive for writers
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void addNode(IntersectionNode node) {
        rwLock.writeLock().lock();
        try {
            nodes.put(node.getId(), node);
            adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void addEdge(String sourceNodeId, RoadEdge edge) {
        rwLock.writeLock().lock();
        try {
            adjacencyList.computeIfAbsent(sourceNodeId, k -> new ArrayList<>()).add(edge);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public IntersectionNode getNode(String nodeId) {
        rwLock.readLock().lock();
        try {
            return nodes.get(nodeId);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<String, IntersectionNode> getAllNodes() {
        rwLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(nodes));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<RoadEdge> getNeighbors(String nodeId) {
        rwLock.readLock().lock();
        try {
            List<RoadEdge> edges = adjacencyList.get(nodeId);
            if (edges == null) {
                return Collections.emptyList();
            }
            // Return a shallow copy to prevent concurrent iteration issues
            return new ArrayList<>(edges);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void updateEdgeTraffic(String sourceNodeId, String targetNodeId, double multiplier) {
        rwLock.writeLock().lock();
        try {
            List<RoadEdge> edges = adjacencyList.get(sourceNodeId);
            if (edges != null) {
                for (RoadEdge edge : edges) {
                    if (edge.getTargetNodeId().equals(targetNodeId)) {
                        edge.setTrafficMultiplier(multiplier);
                        break;
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void updateRoadClosure(String sourceNodeId, String targetNodeId, boolean isBlocked) {
        rwLock.writeLock().lock();
        try {
            List<RoadEdge> edges = adjacencyList.get(sourceNodeId);
            if (edges != null) {
                for (RoadEdge edge : edges) {
                    if (edge.getTargetNodeId().equals(targetNodeId)) {
                        edge.setBlocked(isBlocked); // lombok generated setter in model.RoadEdge.java  ->  name convention - setBlocked kyu why not setisBlocked? see doc
                        break;
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}