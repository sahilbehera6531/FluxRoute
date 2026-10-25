# Dynamic Graph Routing Engine

A routing system for dynamically changing road networks that efficiently reroutes affected vehicles when roads become unavailable during active trips.

## Problem

Consider a logistics network with thousands of vehicles travelling simultaneously across a road network.

Each vehicle has an active route from its current location to its destination.

Now suppose a road suddenly becomes unavailable because of a:

- Landslide
- Flood
- Damaged bridge
- Accident
- Road closure
- Other temporary disruption

A shortest-path algorithm can calculate a new route for a vehicle.

The harder problem is managing this when **the road network and thousands of active vehicle routes are changing at the same time**.

A naive approach would recalculate the route for every vehicle whenever the road network changes.

That creates unnecessary computation.

If a closed road is not part of a vehicle's route, that vehicle does not need to be rerouted at all.

At the same time, vehicles continuously send location and telemetry updates, creating a large number of concurrent I/O operations.

The core problem is therefore:

> **How can a routing system efficiently handle dynamic road-network changes and reroute only the vehicles that are actually affected, while continuing to serve a large number of active vehicles?**





## Solution

This project builds a routing engine around a **dynamic in-memory road graph** and a set of active vehicle routes.

When a road becomes unavailable, the system:

1. Updates the road network.
2. Determines which active routes are affected.
3. Selects only those vehicles for rerouting.
4. Recalculates their paths through the updated graph.
5. Makes the updated routes available to the affected vehicles.

Conceptually:
```
                 Road Disruption
                        │
                        ▼
              Update Road Network
                        │
                        ▼
              Find Affected Routes
                        │
                        ▼
               Recalculate Paths
                        │
                        ▼
               Updated Vehicle Routes

```

The important part is that **a network change does not automatically trigger a complete fleet-wide recomputation**.

---

## Example

Suppose the current road network is:
```
A ─── B ─── C ─── D
      │
      E ─── F

```

A vehicle is currently following:
```
A → B → C → D

```

Now suppose the road between `B` and `C` becomes unavailable:
```
A ─── B     C ─── D
      │
      E ─── F

```

The existing route is no longer valid.

The system identifies that the vehicle's route contains the affected road and recalculates a new path through the available network.

A different vehicle whose route does not contain the blocked road does **not** need to be recalculated.
```
Road X becomes unavailable
          │
          ▼
   ┌─────────────────┐
   │ Check active    │
   │ vehicle routes  │
   └────────┬────────┘
            │
       ┌────┴────┐
       │         │
   Route uses   Route does not
   Road X       use Road X
       │         │
       ▼         ▼
   Reroute     No change

```

This selective rerouting is a central part of the system.





# Dynamic graph routing engine



A Java 21 and Spring Boot routing engine that models a changing road network, tracks active vehicle routes, and selectively reroutes only the vehicles affected by a road disruption.

## What the project implements

The application keeps an in-memory weighted directed graph:
```
IntersectionNode = road junction
RoadEdge         = directed road
Weight           = distance adjusted by traffic

```

The graph is loaded from [`src/main/resources/data/map-data.json`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/resources/data/map-data.json). Roads can be closed or assigned a traffic multiplier while the application is running.

When a disruption is received, the engine:

1. Updates the affected edge.
2. Uses an inverted index to find vehicles whose current routes use that edge.
3. Runs A\* for only those affected vehicles.
4. Stores the newly calculated routes.
5. Reports the rerouting latency.

Vehicles whose routes do not use the disrupted edge are not recalculated.

## Real-world interpretation

This represents a simplified fleet-routing backend. For example, if flooding closes one directed road segment, the service finds delivery vehicles currently scheduled to use that segment and calculates alternative routes for those vehicles. The project simulates vehicle state and REST requests; it does not connect to physical vehicles or receive live GPS telemetry.

## Routing algorithm

[`PathfindingService`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/service/PathfindingService.java) implements A\* search:
```
f(n) = g(n) + h(n)

```

- `g(n)` is the accumulated dynamic road cost.
- `h(n)` is a geographic-distance heuristic.
- Blocked edges are skipped.
- Traffic multipliers increase an edge's effective cost.

Dijkstra's algorithm is discussed as background in the original design, but a separate Dijkstra implementation is not currently included.

## Concurrency and thread safety

The implemented concurrency model separates shared-state protection from CPU parallelism:
```
Graph and fleet state
    -> ReentrantReadWriteLock and ConcurrentHashMap

Affected-vehicle A* calculations
    -> CompletableFuture and bounded fixed platform-thread pool

```

[`DynamicRerouteEngine`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/service/DynamicRerouteEngine.java) creates a fixed-size `ExecutorService`. Each affected vehicle is submitted as a `CompletableFuture` task, and the disruption request waits for all submitted reroutes to finish.

[`GraphManager`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/service/GraphManager.java) uses `ReentrantReadWriteLock` so graph reads can happen concurrently while edge updates are exclusive. [`FleetManager`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/service/FleetManager.java) uses concurrent collections for the active-vehicle registry and road-to-vehicle index.

The route calculations are CPU-bound and use platform threads. The project does not currently use virtual threads, WebSockets, GPS ingestion, or a ForkJoinPool.

The only explicit file I/O is the one-time startup load of `map-data.json` in [`MapDataLoader`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/service/MapDataLoader.java).

## REST simulation API

[`RoutingSimulationController`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/main/java/com/routing/dynamic_graph_routing/controller/RoutingSimulationController.java) exposes:

| **Method** | **Endpoint**                          | **Purpose**                                   |
| ---------- | ------------------------------------- | --------------------------------------------- |
| `POST`     | `/api/simulation/init-fleet?count=50` | Create simulated vehicles and initial routes  |
| `POST`     | `/api/simulation/disrupt`             | Close a road or change its traffic multiplier |
| `POST`     | `/api/simulation/step`                | Move each vehicle one hop along its route     |
| `GET`      | `/api/simulation/vehicles`            | Inspect active vehicle state                  |
| `GET`      | `/api/simulation/edge-traffic`        | Inspect vehicles assigned to an edge          |

Example disruption body:

{   "fromNodeId": "NODE_JVLR_WEST",   "toNodeId": "NODE_POWAI",   "blocked": true,   "congestionFactor": 0.0,   "description": "Monsoon flooding simulation" }

The API is request/response REST. It does not provide a persistent vehicle connection or server-pushed route updates.

## Benchmark

[`SimulationEngineTest`](https://github.com/sahilbehera6531/dynamic-graph-routing-engine/blob/main/src/test/java/com/routing/dynamic_graph_routing/SimulationEngineTest.java) contains the large-fleet disruption test. The test:

1. Creates 5,000,000 simulated vehicles.
2. Calculates an initial route for each vehicle.
3. Selects a directed edge that has assigned vehicles.
4. Blocks that single edge.
5. Reroutes the vehicles indexed on that edge.
6. Verifies that the blocked edge has no vehicles assigned afterward.
7. Verifies that rerouted routes do not still contain the blocked edge.

The test prefers `NODE_JVLR_WEST -> NODE_POWAI`. If no vehicles are assigned to that edge, it scans the node pairs and selects the first directed edge with at least one assigned vehicle.

One recorded run reported:
```
Fleet size:              5,000,000
Affected vehicles:       667,811
Rerouting latency:       3,480 ms
Test result:              passed

```

These numbers describe one local run and one single-edge disruption. They are not a universal throughput guarantee. The affected count is produced by the deterministic vehicle initialization seed, the graph topology, the generated origin/destination pairs, and the selected edge. It is not manually chosen.

##

## Technology

- Java 21
- Spring Boot 3.4.2
- Spring MVC REST
- A\* pathfinding
- Mutable adjacency-list graph
- `ReentrantReadWriteLock`
- `ConcurrentHashMap`
- `CompletableFuture`
- Bounded fixed platform-thread pool
- In-memory fleet simulation
- JUnit and Spring Boot testing

## Scope

The project focuses on dynamic road-network changes, selective rerouting, and bounded concurrent route computation. GIS visualization, live traffic prediction, weather prediction, business fleet operations, and external logistics integrations are outside the scope.

## Status

Core dynamic-routing and large-fleet simulation are implemented. The REST simulation and recorded benchmark are available; live telemetry and virtual-thread communication are intentionally outside the current scope.
