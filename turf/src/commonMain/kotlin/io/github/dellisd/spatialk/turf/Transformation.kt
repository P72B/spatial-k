package io.github.dellisd.spatialk.turf

import io.github.dellisd.spatialk.geojson.GeoJson


/**
 * Calculates a buffer for input features for a given radius. Units supported are miles, kilometers, and degrees.
 *
 * When using a negative radius, the resulting geometry may be invalid if
 * it's too small compared to the radius magnitude. If the input is a
 * FeatureCollection, only valid members will be returned in the output
 * FeatureCollection - i.e., the output collection may have fewer members than
 * the input, or even be empty.
 *
 * @param {FeatureCollection|Geometry|Feature<any>} geojson input to be buffered
 * @param {number} radius distance to draw the buffer (negative values are allowed)
 * @param {units} [Units] Optional parameters
 * @returns {FeatureCollection|Feature<Polygon|MultiPolygon>|undefined} buffered features
 */
@ExperimentalTurfApi
fun buffer(geojson: GeoJson, radius: Double, units: Units = Units.Kilometers): GeoJson? {
    val steps = 8
    return null
}