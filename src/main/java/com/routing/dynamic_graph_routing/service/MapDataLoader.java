package com.routing.dynamic_graph_routing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routing.dynamic_graph_routing.dto.MapDataWrapper;
import com.routing.dynamic_graph_routing.model.IntersectionNode;
import com.routing.dynamic_graph_routing.model.RoadEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MapDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MapDataLoader.class);

    private final GraphManager graphManager;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public MapDataLoader(GraphManager graphManager, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.graphManager = graphManager;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Bootstrapping Mumbai road network topology from classpath...");

        Resource resource = resourceLoader.getResource("classpath:data/map-data.json");

        if (!resource.exists()) {
            throw new IllegalStateException("Critical data file missing: classpath:data/map-data.json");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            MapDataWrapper mapData = objectMapper.readValue(inputStream, MapDataWrapper.class);

            // 1. Ingest Intersections (Vertices) matching IntersectionNode(id, latitude, longitude)
            for (MapDataWrapper.NodeData nodeData : mapData.intersections()) {
                IntersectionNode node = new IntersectionNode(
                        nodeData.id(),
                        nodeData.latitude(),
                        nodeData.longitude()
                );
                graphManager.addNode(node);
            }

            int forwardEdgeCount = 0;
            int reverseEdgeCount = 0;

            // 2. Ingest Roads matching RoadEdge(targetNodeId, baseDistance) and GraphManager.addEdge(sourceNodeId, edge)
            for (MapDataWrapper.RoadData roadData : mapData.roads()) {
                // Forward directed edge: source -> target
                RoadEdge forwardEdge = new RoadEdge(
                        roadData.targetNodeId(),
                        roadData.baseDistanceKm()
                );
                graphManager.addEdge(roadData.sourceNodeId(), forwardEdge);
                forwardEdgeCount++;

                // Bidirectional handling: target -> source
                if (roadData.bidirectional()) {
                    RoadEdge reverseEdge = new RoadEdge(
                            roadData.sourceNodeId(),
                            roadData.baseDistanceKm()
                    );
                    graphManager.addEdge(roadData.targetNodeId(), reverseEdge);
                    reverseEdgeCount++;
                } else {
                    log.info("Registered strictly unidirectional corridor: {} ({} -> {})",
                            roadData.id(), roadData.sourceNodeId(), roadData.targetNodeId());
                }
            }

            log.info("Graph ingestion complete. Loaded {} nodes and {} directed edges ({} FWD, {} REV).",
                    graphManager.getAllNodes().size(),
                    (forwardEdgeCount + reverseEdgeCount),
                    forwardEdgeCount,
                    reverseEdgeCount);

        } catch (Exception e) {
            log.error("Fatal exception during spatial graph initialization", e);
            throw e;
        }
    }
}