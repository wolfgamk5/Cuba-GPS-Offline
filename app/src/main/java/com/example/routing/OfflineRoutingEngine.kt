package com.example.routing

import com.example.data.CubaGeographyData
import com.example.data.GeoPoint
import com.example.data.RoadSegment
import com.example.data.RouteResult
import com.example.data.RouteStep
import com.example.data.TurnType
import java.util.PriorityQueue
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

class OfflineRoutingEngine {

    private val roadSegments = CubaGeographyData.TRUNK_ROADS

    // Graph representation for Dijkstra / A* routing
    private class Node(val point: GeoPoint, val roadName: String, val speedLimit: Int)
    private class Edge(val targetIndex: Int, val distanceMeters: Double, val roadName: String, val speedLimit: Int, val pathPoints: List<GeoPoint>)

    private val nodes = mutableListOf<Node>()
    private val adjacencyList = mutableListOf<MutableList<Edge>>()

    init {
        buildGraph()
    }

    private fun buildGraph() {
        nodes.clear()
        adjacencyList.clear()

        // Index points from all road segments
        for (segment in roadSegments) {
            for (i in 0 until segment.points.size) {
                val pt = segment.points[i]
                var nodeIdx = findExistingNode(pt)
                if (nodeIdx == -1) {
                    nodeIdx = nodes.size
                    nodes.add(Node(pt, segment.name, segment.speedLimit))
                    adjacencyList.add(mutableListOf())
                }

                // Connect with previous point in segment
                if (i > 0) {
                    val prevPt = segment.points[i - 1]
                    val prevIdx = findExistingNode(prevPt)
                    if (prevIdx != -1 && prevIdx != nodeIdx) {
                        val dist = pt.distanceTo(prevPt)
                        // Bidirectional connection on typical Cuban highways
                        adjacencyList[prevIdx].add(Edge(nodeIdx, dist, segment.name, segment.speedLimit, listOf(prevPt, pt)))
                        adjacencyList[nodeIdx].add(Edge(prevIdx, dist, segment.name, segment.speedLimit, listOf(pt, prevPt)))
                    }
                }
            }
        }

        // Add interconnection links between nearby highway junctions (e.g. Autopista Nacional <-> Carretera Central)
        connectIntersections()
    }

    private fun findExistingNode(pt: GeoPoint): Int {
        for (i in nodes.indices) {
            if (nodes[i].point.distanceTo(pt) < 1500.0) { // within 1.5km considers same junction
                return i
            }
        }
        return -1
    }

    private fun connectIntersections() {
        for (i in 0 until nodes.size) {
            for (j in i + 1 until nodes.size) {
                val dist = nodes[i].point.distanceTo(nodes[j].point)
                if (dist < 15000.0) { // Connect junctions within 15 km via local links
                    adjacencyList[i].add(Edge(j, dist, "Enlace Interprovincial", 60, listOf(nodes[i].point, nodes[j].point)))
                    adjacencyList[j].add(Edge(i, dist, "Enlace Interprovincial", 60, listOf(nodes[j].point, nodes[i].point)))
                }
            }
        }
    }

    /**
     * Finds nearest graph node to given coordinates
     */
    private fun findNearestNodeIndex(target: GeoPoint): Int {
        var minDistance = Double.MAX_VALUE
        var nearestIdx = 0
        for (i in nodes.indices) {
            val dist = nodes[i].point.distanceTo(target)
            if (dist < minDistance) {
                minDistance = dist
                nearestIdx = i
            }
        }
        return nearestIdx
    }

    /**
     * Calculates the optimal route offline using Dijkstra / A*
     */
    fun calculateRoute(origin: GeoPoint, destination: GeoPoint): RouteResult {
        if (nodes.isEmpty()) {
            buildGraph()
        }

        val startNodeIdx = findNearestNodeIndex(origin)
        val endNodeIdx = findNearestNodeIndex(destination)

        val distances = DoubleArray(nodes.size) { Double.MAX_VALUE }
        val previousNode = IntArray(nodes.size) { -1 }
        val previousEdge = Array<Edge?>(nodes.size) { null }

        distances[startNodeIdx] = 0.0

        // Priority queue for Dijkstra (cost, nodeIndex)
        val pq = PriorityQueue<Pair<Double, Int>>(compareBy { it.first })
        pq.add(Pair(0.0, startNodeIdx))

        while (pq.isNotEmpty()) {
            val (currentDist, u) = pq.poll() ?: break

            if (u == endNodeIdx) break
            if (currentDist > distances[u]) continue

            for (edge in adjacencyList[u]) {
                val v = edge.targetIndex
                val weight = edge.distanceMeters
                if (distances[u] + weight < distances[v]) {
                    distances[v] = distances[u] + weight
                    previousNode[v] = u
                    previousEdge[v] = edge
                    // A* heuristic: distance to end node
                    val heuristic = nodes[v].point.distanceTo(nodes[endNodeIdx].point)
                    pq.add(Pair(distances[v] + heuristic * 0.4, v))
                }
            }
        }

        // Reconstruct polyline path
        val fullPolyline = mutableListOf<GeoPoint>()
        fullPolyline.add(origin)

        val pathNodeIndices = mutableListOf<Int>()
        var curr = endNodeIdx
        while (curr != -1) {
            pathNodeIndices.add(curr)
            curr = previousNode[curr]
        }
        pathNodeIndices.reverse()

        val rawSteps = mutableListOf<RouteStep>()
        val primaryRoads = mutableSetOf<String>()

        if (pathNodeIndices.size > 1) {
            for (i in 0 until pathNodeIndices.size - 1) {
                val u = pathNodeIndices[i]
                val v = pathNodeIndices[i + 1]
                val edge = adjacencyList[u].find { it.targetIndex == v }
                if (edge != null) {
                    fullPolyline.addAll(edge.pathPoints)
                    primaryRoads.add(edge.roadName)
                } else {
                    fullPolyline.add(nodes[v].point)
                }
            }
        } else {
            // Direct interpolated path if start and end are in the same local zone
            fullPolyline.addAll(interpolateDirectRoute(origin, destination))
        }

        fullPolyline.add(destination)

        // Clean consecutive duplicates in polyline
        val cleanedPolyline = mutableListOf<GeoPoint>()
        for (pt in fullPolyline) {
            if (cleanedPolyline.isEmpty() || cleanedPolyline.last().distanceTo(pt) > 50.0) {
                cleanedPolyline.add(pt)
            }
        }

        // Calculate total distance
        var totalDistMeters = 0.0
        for (i in 0 until cleanedPolyline.size - 1) {
            totalDistMeters += cleanedPolyline[i].distanceTo(cleanedPolyline[i + 1])
        }

        // Generate Turn-by-Turn navigation steps
        val steps = generateTurnSteps(cleanedPolyline, origin, destination, primaryRoads.toList())

        // Calculate estimated travel time (average 75 km/h in Cuba interprovincial travel)
        val distanceKm = totalDistMeters / 1000.0
        val estimatedMinutes = ((distanceKm / 75.0) * 60.0).roundToInt().coerceAtLeast(3)

        val routeTitle = if (primaryRoads.isNotEmpty()) {
            "Por ${primaryRoads.joinToString(" y ")}"
        } else {
            "Ruta directa hacia ${destination.name.ifEmpty { "Destino" }}"
        }

        return RouteResult(
            origin = origin,
            destination = destination,
            totalDistanceKm = (distanceKm * 10).roundToInt() / 10.0,
            estimatedTimeMinutes = estimatedMinutes,
            polyline = cleanedPolyline,
            steps = steps,
            routeName = routeTitle,
            primaryRoads = primaryRoads.toList()
        )
    }

    private fun interpolateDirectRoute(p1: GeoPoint, p2: GeoPoint): List<GeoPoint> {
        val count = 5
        val list = mutableListOf<GeoPoint>()
        for (i in 1..count) {
            val ratio = i.toDouble() / (count + 1)
            val lat = p1.lat + (p2.lat - p1.lat) * ratio
            val lon = p1.lon + (p2.lon - p1.lon) * ratio
            list.add(GeoPoint(lat, lon, "Tramo"))
        }
        return list
    }

    /**
     * Generates human & TTS readable Turn-by-Turn instructions
     */
    private fun generateTurnSteps(
        polyline: List<GeoPoint>,
        origin: GeoPoint,
        destination: GeoPoint,
        roads: List<String>
    ): List<RouteStep> {
        val steps = mutableListOf<RouteStep>()

        if (polyline.size < 2) {
            steps.add(
                RouteStep(
                    id = UUID.randomUUID().toString(),
                    instruction = "Diríjase al destino: ${destination.name}",
                    roadName = "Vía local",
                    distanceMeters = origin.distanceTo(destination),
                    turnType = TurnType.DESTINATION,
                    startPoint = origin,
                    endPoint = destination
                )
            )
            return steps
        }

        val mainRoad = roads.firstOrNull() ?: "Carretera Principal"

        // Initial step
        steps.add(
            RouteStep(
                id = UUID.randomUUID().toString(),
                instruction = "Inicie su viaje hacia el ${getCardinalDirection(polyline[0].bearingTo(polyline[1]))} por $mainRoad",
                roadName = mainRoad,
                distanceMeters = polyline[0].distanceTo(polyline[1]),
                turnType = TurnType.START,
                startPoint = polyline[0],
                endPoint = polyline[1]
            )
        )

        // Intermediate steps based on bearing changes
        for (i in 1 until polyline.size - 1) {
            val pPrev = polyline[i - 1]
            val pCurr = polyline[i]
            val pNext = polyline[i + 1]

            val bearingIn = pPrev.bearingTo(pCurr)
            val bearingOut = pCurr.bearingTo(pNext)

            var angleDiff = bearingOut - bearingIn
            if (angleDiff > 180) angleDiff -= 360
            if (angleDiff < -180) angleDiff += 360

            val dist = pCurr.distanceTo(pNext)

            if (abs(angleDiff) > 25.0 || dist > 25000.0) {
                val (turnType, turnText) = classifyTurn(angleDiff)
                val assignedRoad = roads.getOrNull(i % roads.size.coerceAtLeast(1)) ?: mainRoad

                val instruction = if (abs(angleDiff) > 25.0) {
                    "$turnText hacia $assignedRoad y continúe ${formatDistance(dist)}"
                } else {
                    "Continúe recto por $assignedRoad durante ${formatDistance(dist)}"
                }

                steps.add(
                    RouteStep(
                        id = UUID.randomUUID().toString(),
                        instruction = instruction,
                        roadName = assignedRoad,
                        distanceMeters = dist,
                        turnType = turnType,
                        startPoint = pCurr,
                        endPoint = pNext
                    )
                )
            }
        }

        // Final Destination Step
        val lastPoint = polyline.last()
        val prevToLast = polyline[polyline.size - 2]
        steps.add(
            RouteStep(
                id = UUID.randomUUID().toString(),
                instruction = "Ha llegado a su destino: ${destination.name.ifEmpty { "Punto de llegada" }}",
                roadName = destination.name,
                distanceMeters = prevToLast.distanceTo(lastPoint),
                turnType = TurnType.DESTINATION,
                startPoint = prevToLast,
                endPoint = lastPoint
            )
        )

        return steps
    }

    private fun classifyTurn(angleDiff: Float): Pair<TurnType, String> {
        return when {
            angleDiff in -45.0..-20.0 -> Pair(TurnType.SLIGHT_LEFT, "Gire ligeramente a la izquierda")
            angleDiff in -120.0..-45.0 -> Pair(TurnType.LEFT, "Gire a la izquierda")
            angleDiff < -120.0 -> Pair(TurnType.SHARP_LEFT, "Gire pronunciadamente a la izquierda")
            angleDiff in 20.0..45.0 -> Pair(TurnType.SLIGHT_RIGHT, "Gire ligeramente a la derecha")
            angleDiff in 45.0..120.0 -> Pair(TurnType.RIGHT, "Gire a la derecha")
            angleDiff > 120.0 -> Pair(TurnType.SHARP_RIGHT, "Gire pronunciadamente a la derecha")
            else -> Pair(TurnType.STRAIGHT, "Continúe recto")
        }
    }

    private fun getCardinalDirection(bearing: Float): String {
        return when (bearing) {
            in 337.5..360.0, in 0.0..22.5 -> "Norte"
            in 22.5..67.5 -> "Noreste"
            in 67.5..112.5 -> "Este"
            in 112.5..157.5 -> "Sureste"
            in 157.5..202.5 -> "Sur"
            in 202.5..247.5 -> "Suroeste"
            in 247.5..292.5 -> "Oeste"
            else -> "Noroeste"
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            "${(meters / 1000.0 * 10).roundToInt() / 10.0} km"
        } else {
            "${meters.roundToInt()} m"
        }
    }
}
