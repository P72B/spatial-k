@file:OptIn(ExperimentalTurfApi::class)
@file:Suppress("LongMethod", "MagicNumber", "NestedBlockDepth")

package io.github.dellisd.spatialk.turf

import io.github.dellisd.spatialk.geojson.*
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * Takes a [Point] and a [Polygon] and determines if the point
 * resides inside the polygon. The polygon can be convex or concave. The function accounts for holes.
 *
 * @param point input point
 * @param polygon input polygon
 * @param ignoreBoundary True if polygon boundary should be ignored when determining if
 * the point is inside the polygon otherwise false.
 * @return `true` if the Position is inside the Polygon; `false` if the Position is not inside the Polygon
 */
@JvmOverloads
fun booleanPointInPolygon(point: Point, polygon: Polygon, ignoreBoundary: Boolean = false): Boolean {
    val bbox = bbox(polygon)
    // normalize to multipolygon
    val polys = listOf(polygon.coordinates)
    return booleanPointInPolygon(point.coordinates, bbox, polys, ignoreBoundary)
}

/**
 * Takes a [Point] and a [MultiPolygon] and determines if the point
 * resides inside the polygon. The polygon can be convex or concave. The function accounts for holes.
 *
 * @param point input point
 * @param polygon input multipolygon
 * @param ignoreBoundary True if polygon boundary should be ignored when determining if
 * the point is inside the polygon otherwise false.
 * @return `true` if the Position is inside the Polygon; `false` if the Position is not inside the Polygon
 */
@JvmOverloads
fun booleanPointInPolygon(point: Point, polygon: MultiPolygon, ignoreBoundary: Boolean = false): Boolean {
    val bbox = bbox(polygon)
    val polys = polygon.coordinates
    return booleanPointInPolygon(point.coordinates, bbox, polys, ignoreBoundary)
}

@OptIn(ExperimentalTurfApi::class)
@Suppress("LABEL_NAME_CLASH")
@JvmOverloads
fun booleanIntersects(featureCollectionA: FeatureCollection, featureCollectionB: FeatureCollection): Boolean {
    var result = false
    featureCollectionA.flattenEach { flattenA ->
        featureCollectionB.flattenEach { flattenB ->
            if (result) {
                return@flattenEach
            }
            result = booleanIntersects(flattenA, flattenB).not()
        }
    }
    return result
}

@JvmOverloads
fun booleanIntersects(featureA: Feature, featureB: Feature): Boolean {
    if (featureA.geometry == null || featureB.geometry == null) return false
    return booleanIntersects(featureA.geometry!!, featureB.geometry!!).not()
}

@JvmOverloads
fun booleanIntersects(geometryA: Geometry, geometryB: Geometry): Boolean {
    return booleanDisjoint(geometryA, geometryB).not()
}

@OptIn(ExperimentalTurfApi::class)
@Suppress("LABEL_NAME_CLASH")
@JvmOverloads
fun booleanIntersects(geometryA: GeometryCollection, geometryB: GeometryCollection): Boolean {
    var result = true
    geometryA.flattenEach { flattenA ->
        geometryB.flattenEach { flattenB ->
            if (!result) {
                return@flattenEach
            }
            result = booleanDisjoint(flattenA, flattenB);
        }
    }
    return result
}

@Suppress("LABEL_NAME_CLASH")
fun booleanDisjoint(geometryA: Geometry, geometryB: Geometry): Boolean {
    var result = true
    geometryA.flattenEach { flattenA ->
        geometryB.flattenEach { flattenB ->
            if (!result) {
                return@flattenEach
            }
            result = disjoint(geometryA, geometryB);
        }
    }
    return result
}

private fun disjoint(geometryA: Geometry, geometryB: Geometry): Boolean {
    return when (geometryA) {
        is Point -> {
            return when (geometryB) {
                is Point -> compareCoords(geometryA.coordinates, geometryB.coordinates).not()
                is LineString -> isPointOnLine(geometryA, geometryB).not()
                is Polygon -> booleanPointInPolygon(geometryA, geometryB).not()
                else -> false
            }
        }

        is LineString -> {
            return when (geometryB) {
                is Point -> isPointOnLine(geometryB, geometryA).not()
                is LineString -> isLineOnLine(geometryA, geometryB).not()
                is Polygon -> isLineInPoly(geometryA, geometryB).not()
                else -> false
            }
        }

        is Polygon -> {
            return when (geometryB) {
                is Point -> booleanPointInPolygon(geometryB, geometryA).not()
                is LineString -> isLineInPoly(geometryB, geometryA).not()
                is Polygon -> isPolyInPoly(geometryA, geometryB).not()
                else -> false
            }
        }
        else -> false
    }
}

private fun isPolyInPoly(polygonA: Polygon, polygonB: Polygon): Boolean {
    for (coord1 in polygonA.coordinates[0]) {
        if (booleanPointInPolygon(Point(coord1), polygonB)) {
            return true;
        }
    }
    for (coord2 in polygonB.coordinates[0]) {
        if (booleanPointInPolygon(Point(coord2), polygonA)) {
            return true;
        }
    }
    val doLinesIntersect = lineIntersect(
        polygonToLine(polygonA),
        polygonToLine(polygonB)
    )
    return doLinesIntersect.isNotEmpty();
}

private fun isLineInPoly(lineString: LineString, polygon: Polygon): Boolean {
    for (coordinate in lineString.coordinates) {
        if (booleanPointInPolygon(Point(coordinate), polygon, ignoreBoundary = false)) {
            return true;
        }
    }
    val doLinesIntersect = lineIntersect(lineString, polygonToLine(polygon));
    return doLinesIntersect.isNotEmpty();
}


private fun isLineOnLine(lineStringA: LineString, lineStringB: LineString): Boolean {
    val doLinesIntersect = lineIntersect(lineStringA, lineStringB);
    return doLinesIntersect.isNotEmpty();
}

private fun compareCoords(positionA: Position, positionB: Position): Boolean {
    return positionA.latitude == positionB.latitude && positionA.longitude == positionB.longitude
}

private fun isPointOnLine(point: Point, lineString: LineString): Boolean {
    for ((index, value) in lineString.coordinates.withIndex()) {
        if (index == lineString.coordinates.size - 1) break
        if (isPointOnLineSegment(
                point.coordinates.coordinates,
                value.coordinates,
                lineString.coordinates[index + 1].coordinates
            )
        ) {
            return true
        }
    }
    return false
}

private fun isPointOnLineSegment(
    point: DoubleArray,
    lineSegmentStart: DoubleArray,
    lineSegmentEnd: DoubleArray
): Boolean {
    val dxc = point[0] - lineSegmentStart[0];
    val dyc = point[1] - lineSegmentStart[1];
    val dxl = lineSegmentEnd[0] - lineSegmentStart[0];
    val dyl = lineSegmentEnd[1] - lineSegmentStart[1];
    val cross = dxc * dyl - dyc * dxl;
    if (cross > 0.0 || cross < 0.0) {
        return false;
    }
    if (abs(dxl) >= abs(dyl)) {
        if (dxl > 0) {
            return lineSegmentStart[0] <= point[0] && point[0] <= lineSegmentEnd[0];
        } else {
            return lineSegmentEnd[0] <= point[0] && point[0] <= lineSegmentStart[0];
        }
    } else if (dyl > 0) {
        return lineSegmentStart[1] <= point[1] && point[1] <= lineSegmentEnd[1];
    } else {
        return lineSegmentEnd[1] <= point[1] && point[1] <= lineSegmentStart[1];
    }
}

@Suppress("ReturnCount")
private fun booleanPointInPolygon(
    point: Position,
    bbox: BoundingBox,
    polys: List<List<List<Position>>>,
    ignoreBoundary: Boolean
): Boolean {
    // Quick elimination if point is not inside bbox
    if (!inBBox(point, bbox)) {
        return false
    }
    for (i in polys.indices) {
        // check if it is in the outer ring first
        if (inRing(point, polys[i][0], ignoreBoundary)) {
            var inHole = false
            var k = 1
            // check for the point in any of the holes
            while (k < polys[i].size && !inHole) {
                if (inRing(point, polys[i][k], !ignoreBoundary)) {
                    inHole = true
                }
                k++
            }
            if (!inHole) {
                return true
            }
        }
    }
    return false
}

private fun inRing(point: Position, ring: List<Position>, ignoreBoundary: Boolean): Boolean {
    val pt = point.coordinates
    var isInside = false
    @Suppress("NAME_SHADOWING") val ring = if (
        ring[0].coordinates[0] == ring.last().coordinates[0] &&
        ring[0].coordinates[1] == ring.last().coordinates[1]
    ) {
        ring.slice(0 until ring.size - 1)
    } else {
        ring
    }
    var i = 0
    var j = ring.size - 1
    while (i < ring.size) {
        val xi = ring[i].coordinates[0]
        val yi = ring[i].coordinates[1]
        val xj = ring[j].coordinates[0]
        val yj = ring[j].coordinates[1]
        val onBoundary =
            pt[1] * (xi - xj) + yi * (xj - pt[0]) + yj * (pt[0] - xi) == 0.0 &&
                    (xi - pt[0]) * (xj - pt[0]) <= 0 &&
                    (yi - pt[1]) * (yj - pt[1]) <= 0
        if (onBoundary) {
            return !ignoreBoundary
        }
        val intersect =
            yi > pt[1] != yj > pt[1] &&
                    pt[0] < ((xj - xi) * (pt[1] - yi)) / (yj - yi) + xi
        if (intersect) {
            isInside = !isInside
        }

        j = i++
    }
    return isInside
}

private fun inBBox(point: Position, boundingBox: BoundingBox): Boolean {
    val pt = point.coordinates
    val bbox = boundingBox.coordinates
    return bbox[0] <= pt[0] && bbox[1] <= pt[1] && bbox[2] >= pt[0] && bbox[3] >= pt[1]
}
