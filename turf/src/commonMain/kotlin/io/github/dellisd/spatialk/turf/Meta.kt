@file:JvmName("TurfMeta")

package io.github.dellisd.spatialk.turf

import io.github.dellisd.spatialk.geojson.*
import kotlin.jvm.JvmName

@ExperimentalTurfApi
fun Geometry.coordAll(): List<Position> = when (this) {
    is Point -> this.coordAll()
    is MultiPoint -> this.coordAll()
    is LineString -> this.coordAll()
    is MultiLineString -> this.coordAll()
    is Polygon -> this.coordAll()
    is MultiPolygon -> this.coordAll()
    is GeometryCollection -> this.coordAll()
}

@ExperimentalTurfApi
fun Point.coordAll() = listOf(coordinates)

@ExperimentalTurfApi
fun MultiPoint.coordAll() = coordinates

@ExperimentalTurfApi
fun LineString.coordAll() = coordinates

@ExperimentalTurfApi
fun MultiLineString.coordAll() = coordinates.reduce { acc, list -> acc + list }

@ExperimentalTurfApi
fun Polygon.coordAll() = coordinates.reduce { acc, list -> acc + list }

@ExperimentalTurfApi
fun MultiPolygon.coordAll() =
    coordinates.fold(emptyList<Position>()) { acc, list ->
        list.reduce { innerAcc, innerList -> innerAcc + innerList } + acc
    }

@ExperimentalTurfApi
fun GeometryCollection.coordAll() =
    geometries.fold(emptyList<Position>()) { acc, geometry -> acc + geometry.coordAll() }

@ExperimentalTurfApi
fun Feature.coordAll() = geometry?.coordAll()

@ExperimentalTurfApi
fun FeatureCollection.coordAll() =
    features.fold(emptyList<Position>()) { acc, feature -> acc + (feature.coordAll() ?: emptyList()) }

@ExperimentalTurfApi
fun FeatureCollection.flattenEach(callback: (Feature) -> Unit) {
    for (feature in features) {
        callback(feature)
    }
}

@ExperimentalTurfApi
fun GeometryCollection.flattenEach(callback: (Geometry) -> Unit) {
    for (geometry in geometries) {
        callback(geometry)
    }
}

fun Geometry.flattenEach(callback: (Geometry) -> Unit) {
    when (this) {
        is GeometryCollection -> {
            for (geometry in geometries) {
                geometry.flattenEach(callback)
            }
        }
        is LineString -> callback(this)
        is MultiLineString -> {
            for (lineString in coordinates) {
                callback(LineString(lineString))
            }
        }
        is MultiPoint -> {
            for (point in coordinates) {
                callback(Point(point))
            }
        }
        is MultiPolygon -> {
            for (polygon in coordinates) {
                callback(Polygon(polygon))
            }
        }
        is Point -> callback(this)
        is Polygon -> callback(this)
    }
}
