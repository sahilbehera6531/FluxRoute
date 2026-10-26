# ONLY FOR 1st TIME - start springboot app on vs code -> on postman, Create new collection -> rename it as Dynamic Graph Routing Engine -> Add request and play the game
## also check screenshots for vehicle id 92, 121 , 187 before and after disrupt how the edge which disrupted, is not there in vehicle-2 get or somewhere u see 2 means after u performed disrupt, u can also check in same vehicle but just for clear and better understanding, i created 2
# Postman testing flow

## 1. Initialize fleet

**Method:** `POST`

**URL:**

```text
http://localhost:8080/api/simulation/init-fleet?count=500
```

**Body:** None

Click **Send**.

---

## 2. View vehicles before disruption

**Method:** `GET`

**URL:**

```text
http://localhost:8080/api/simulation/vehicles
```

**Body:** None

Click **Send** and note some vehicle routes.

---

## 3. Check vehicles on an edge

**Method:** `GET`

**URL:**

```text
http://localhost:8080/api/simulation/edge-traffic?fromNodeId=NODE_JVLR_WEST&toNodeId=NODE_POWAI
```

**Body:** None

Click **Send**.

Note:

```text
vehicleCount
vehicles
```

These are the vehicles currently using that edge.

---

## 4. Trigger road disruption

**Method:** `POST`

**URL:**

```text
http://localhost:8080/api/simulation/disrupt
```

**Headers:**

```text
Content-Type: application/json
```

**Body → raw → JSON:**

```json
{
  "fromNodeId": "NODE_JVLR_WEST",
  "toNodeId": "NODE_POWAI",
  "blocked": true,
  "congestionFactor": 0.0,
  "description": "Postman road-closure test"
}
```

Click **Send**.

---

## 5. View vehicles after rerouting

**Method:** `GET`

**URL:**

```text
http://localhost:8080/api/simulation/vehicles
```

**Body:** None

Click **Send**.

Check the vehicle IDs noted in Step 3. Their new routes should no longer contain:

```text
NODE_JVLR_WEST → NODE_POWAI
```

---

## 6. Check the disrupted edge again

**Method:** `GET`

**URL:**

```text
http://localhost:8080/api/simulation/edge-traffic?fromNodeId=NODE_JVLR_WEST&toNodeId=NODE_POWAI
```

**Body:** None

Expected result:

```json
{
  "vehicleCount": 0,
  "vehicles": []
}
```

---

## 7. Move vehicles one simulation step

**Method:** `POST`

**URL:**

```text
http://localhost:8080/api/simulation/step
```

**Body:** None

Click **Send**.

Expected response:

```json
{
  "message": "Advanced simulation by 1 step"
}
```

The only request requiring a JSON body is the disruption request.
