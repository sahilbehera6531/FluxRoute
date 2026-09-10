# Dynamic Graph Routing Engine

A routing system for dynamically changing road networks that efficiently reroutes affected vehicles when roads become unavailable during active trips.

## Problem

Consider a logistics network with thousands of vehicles travelling simultaneously across a road network.

Each vehicle has an active route from its current location to its destination.

Now suppose a road suddenly becomes unavailable because of a:

* Landslide
* Flood
* Damaged bridge
* Accident
* Road closure
* Other temporary disruption

A shortest-path algorithm can calculate a new route for a vehicle.

The harder problem is managing this when **the road network and thousands of active vehicle routes are changing at the same time**.

A naive approach would recalculate the route for every vehicle whenever the road network changes.

That creates unnecessary computation.

If a closed road is not part of a vehicle's route, that vehicle does not need to be rerouted at all.

At the same time, vehicles continuously send location and telemetry updates, creating a large number of concurrent I/O operations.

The core problem is therefore:

> **How can a routing system efficiently handle dynamic road-network changes and reroute only the vehicles that are actually affected, while continuing to serve a large number of active vehicles?**

---

## Solution

This project builds a routing engine around a **dynamic in-memory road graph** and a set of active vehicle routes.

When a road becomes unavailable, the system:

1. Updates the road network.
2. Determines which active routes are affected.
3. Selects only those vehicles for rerouting.
4. Recalculates their paths through the updated graph.
5. Makes the updated routes available to the affected vehicles.

Conceptually:

```text
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

```text
A ─── B ─── C ─── D
      │
      E ─── F
```

A vehicle is currently following:

```text
A → B → C → D
```

Now suppose the road between `B` and `C` becomes unavailable:

```text
A ─── B     C ─── D
      │
      E ─── F
```

The existing route is no longer valid.

The system identifies that the vehicle's route contains the affected road and recalculates a new path through the available network.

A different vehicle whose route does not contain the blocked road does **not** need to be recalculated.

```text
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

---

## Core Design

The system deals with two different types of workload.

### Vehicle Communication — I/O Bound

Vehicles continuously communicate with the routing system by sending location or telemetry information and receiving route updates.

The project uses **Java Virtual Threads** to support a large number of concurrent vehicle operations without requiring a heavyweight platform thread for every active vehicle.

The objective is to make the communication layer capable of handling many simultaneous connections efficiently.

---

### Route Calculation — CPU Bound

Finding a path through a graph is computational work.

When a disruption affects many vehicles, multiple route calculations may need to happen at the same time.

These calculations are therefore treated separately from the I/O workload and executed using **bounded CPU parallelism**.

The important design principle is:

```text
Vehicle Connections
        │
        │ I/O-bound
        ▼
   Virtual Threads


Route Calculations
        │
        │ CPU-bound
        ▼
 Bounded Parallelism
```

Virtual Threads are not used as a generic solution for CPU-bound work.

---

## Dynamic Road Network

The road network is represented as a weighted graph.

Conceptually:

```text
Node = Location / Junction

Edge = Road connecting two locations

Weight = Cost of travelling through that road
```

An edge can change its availability:

```text
AVAILABLE
    │
    │ disruption
    ▼
BLOCKED
    │
    │ road restored
    ▼
AVAILABLE
```

The routing algorithm must always operate on the current valid state of the network.

This makes the graph different from a conventional static shortest-path problem: **the graph itself can change while vehicles are using it.**

---

## Route Calculation

The project explores shortest-path algorithms for calculating routes through the dynamic graph.

### Dijkstra's Algorithm

Dijkstra's algorithm can be used to find the minimum-cost path between two nodes when edge weights are non-negative.

```text
Source
  │
  ▼
Explore lowest-cost node
  │
  ▼
Relax neighbouring edges
  │
  ▼
Continue until destination
  │
  ▼
Shortest path
```

### A* Search

A* can additionally use a heuristic to guide the search toward the destination.

```text
f(n) = g(n) + h(n)
```

where:

* `g(n)` is the cost from the source to node `n`
* `h(n)` estimates the remaining cost to the destination
* `f(n)` determines the search priority

The final choice of routing algorithm and its role in the system will depend on experimentation and benchmarking.

---

## Concurrent Fleet

To evaluate the system under realistic load, the project includes a fleet simulation component.

The simulator is intended to reproduce the behaviour of many active vehicles:

```text
Vehicle
   │
   ├── Connect
   │
   ├── Send location updates
   │
   ├── Receive route
   │
   ├── Continue travelling
   │
   └── Receive updated route after disruption
```

The purpose is not simply to demonstrate that one vehicle can be rerouted.

The goal is to study how the routing system behaves when **many vehicles are active simultaneously and a network disruption affects a subset of them**.

---

## Key Engineering Problems

### 1. Selective Rerouting

A road disruption should not cause unnecessary route calculations.

The system needs a way to determine which active routes are actually affected.

---

### 2. Dynamic Graph State

The road network can change while route calculations and vehicle updates are taking place.

The system therefore needs a well-defined approach to maintaining and accessing the current graph state.

---

### 3. Concurrent Route Calculation

A single disruption may affect many vehicles.

Independent route calculations can potentially execute concurrently, but the amount of CPU parallelism must remain bounded.

---

### 4. Concurrent Vehicle Communication

Vehicle telemetry continues while routing calculations are happening.

The communication workload should therefore not be unnecessarily blocked by CPU-intensive pathfinding.

---

### 5. Correctness Under Change

A route should be calculated against a valid representation of the road network.

The system must account for situations where network changes occur while previous rerouting work is still being processed.

---

## What This Project Is Really About

The shortest-path algorithm itself is not the main challenge.

The interesting problem is putting graph algorithms inside a continuously changing, concurrent system.

The project brings together:

```text
Dynamic Graphs
      +
Shortest-Path Algorithms
      +
Concurrent Networking
      +
CPU-Bound Computation
      +
Real-Time State Changes
```

The central question is:

> **How can a routing system remain responsive when both the road network and the fleet using it are continuously changing?**

---

## Performance Evaluation

The system will be evaluated experimentally rather than relying on theoretical claims.

The experiments will focus on questions such as:

* How does rerouting time change as the number of affected vehicles increases?
* How much unnecessary computation can selective rerouting avoid?
* How does concurrent route computation compare with sequential processing?
* How does the system behave while vehicle telemetry continues during rerouting?
* How does routing performance change as the road graph grows?
* What are the CPU and memory costs under increasing fleet sizes?

Benchmark results will be added here once they are measured.

---

## Technology

The implementation is being developed primarily with:

* **Java 21**
* **Spring Boot**
* **Java Virtual Threads**
* **Graph data structures and shortest-path algorithms**
* **Concurrent programming utilities**
* **In-memory state management**
* **Vehicle/fleet simulation**

The technology choices are driven by the requirements of the problem rather than being the objective of the project.

---

## Scope

The project focuses specifically on:

> **Dynamic road-network changes and efficient rerouting of active vehicles.**

It is not intended to be a complete commercial logistics platform.

Features such as large-scale GIS visualization, real-world traffic prediction, weather prediction, fleet business operations, and external logistics integrations are outside the core scope.

The objective is to build and measure the **routing and concurrency engine underneath such a system**.

---

## Future Work

Potential extensions after the core system is working include:

* More efficient route-impact detection
* Route caching and invalidation
* Incremental shortest-path algorithms
* Larger geographical road graphs
* ETA estimation
* Priority-based rerouting
* Fault-tolerant vehicle reconnection
* Distributed route computation

These will be considered only after the core routing engine has been implemented and benchmarked.

---

## Status

**ON IT**
