package com.rakshax.app.ui.screens.map

import com.google.android.gms.maps.model.MapStyleOptions

object MapStyleUtils {

    // Clean, high-contrast dark theme for night and AMOLED safety monitoring
    val DARK_MAP_STYLE = MapStyleOptions(
        """
        [
          {"elementType": "geometry", "stylers": [{"color": "#171a21"}]},
          {"elementType": "labels.text.fill", "stylers": [{"color": "#8c95a6"}]},
          {"elementType": "labels.text.stroke", "stylers": [{"color": "#171a21"}]},
          {"featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{"color": "#2c3240"}]},
          {"featureType": "administrative.land_parcel", "elementType": "labels.text.fill", "stylers": [{"color": "#64748b"}]},
          {"featureType": "poi", "elementType": "geometry", "stylers": [{"color": "#1e222d"}]},
          {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#6b7280"}]},
          {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#16231d"}]},
          {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#4ade80"}]},
          {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#252b38"}]},
          {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#1a1f29"}]},
          {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca3af"}]},
          {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#333d4e"}]},
          {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#1e2531"}]},
          {"featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{"color": "#e2e8f0"}]},
          {"featureType": "transit", "elementType": "geometry", "stylers": [{"color": "#212836"}]},
          {"featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{"color": "#94a3b8"}]},
          {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0d1b2a"}]},
          {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#38bdf8"}]}
        ]
        """.trimIndent()
    )

    // Crisp high-legibility light theme
    val LIGHT_MAP_STYLE = MapStyleOptions(
        """
        [
          {"elementType": "geometry", "stylers": [{"color": "#f8fafc"}]},
          {"elementType": "labels.text.fill", "stylers": [{"color": "#334155"}]},
          {"elementType": "labels.text.stroke", "stylers": [{"color": "#ffffff"}]},
          {"featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{"color": "#cbd5e1"}]},
          {"featureType": "poi", "elementType": "geometry", "stylers": [{"color": "#f1f5f9"}]},
          {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#dcfce7"}]},
          {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#ffffff"}]},
          {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#e2e8f0"}]},
          {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#fde68a"}]},
          {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#f59e0b"}]},
          {"featureType": "transit", "elementType": "geometry", "stylers": [{"color": "#e2e8f0"}]},
          {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#bae6fd"}]}
        ]
        """.trimIndent()
    )
}
