package io.github.dellisd.spatialk.turf

import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position

fun polygonToLine(polygon: Polygon): LineString {
    return coordsToLine(polygon.coordinates)
}

fun coordsToLine(coordinates: List<List<Position>>): LineString {
    if (coordinates.size > 1) {
        throw NotImplementedError("MultiLine is currently unsupported")
    }
    return LineString(coordinates[0])
}