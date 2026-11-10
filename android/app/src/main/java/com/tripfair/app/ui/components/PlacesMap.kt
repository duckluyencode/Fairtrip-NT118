package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tripfair.app.BuildConfig
import com.tripfair.app.R
import com.tripfair.app.data.places.LocationDto

/**
 * A place on the map. [number] draws a numbered coral marker (stops of a day, in order);
 * without it the standard pin is used. [title] and [detail] show when the pin is tapped.
 */
data class MapPin(
    val id: String,
    val position: LocationDto,
    val title: String,
    val detail: String? = null,
    val number: Int? = null,
)

private const val SINGLE_PIN_ZOOM = 15f
private const val BOUNDS_PADDING_PX = 120

// Shown before any pin is known: Việt Nam as a whole.
private val Vietnam = CameraPosition.fromLatLngZoom(LatLng(16.05, 108.2), 5f)

private fun LocationDto.latLng() = LatLng(lat, lng)

/**
 * Google map of [pins], framed to show all of them. [route] joins the pins in order;
 * [showMyLocation] adds the blue dot (pass true only once the user granted location).
 * Tapping a pin shows its name; tapping the name calls [onPinClick].
 */
@Composable
fun PlacesMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    route: Boolean = false,
    showMyLocation: Boolean = false,
    onPinClick: ((MapPin) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    // Previews cannot run Google Play services, and without a key the SDK draws nothing at
    // all (not even markers): a box stands in for the map, saying why when the key is missing.
    if (LocalInspectionMode.current || !BuildConfig.HAS_MAPS_KEY) {
        Column(
            modifier = modifier.background(colors.surfaceContainerHigh),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(painter = painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
            if (!BuildConfig.HAS_MAPS_KEY) {
                Text(
                    text = stringResource(R.string.map_no_key),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        return
    }

    val camera = rememberCameraPositionState { position = Vietnam }
    var loaded by remember { mutableStateOf(false) }

    // Frames the pins once the map has a size, and again whenever they change.
    LaunchedEffect(loaded, pins) {
        if (!loaded || pins.isEmpty()) return@LaunchedEffect
        val update = if (pins.size == 1) {
            CameraUpdateFactory.newLatLngZoom(pins.first().position.latLng(), SINGLE_PIN_ZOOM)
        } else {
            val bounds = LatLngBounds.builder().apply { pins.forEach { include(it.position.latLng()) } }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX)
        }
        camera.animate(update)
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = showMyLocation),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = showMyLocation,
            ),
            onMapLoaded = { loaded = true },
        ) {
            if (route && pins.size > 1) {
                Polyline(points = pins.map { it.position.latLng() }, color = colors.primary, width = 10f)
            }
            pins.forEach { pin ->
                val state = remember(pin.id, pin.position) { MarkerState(position = pin.position.latLng()) }
                val onInfoClick: (com.google.android.gms.maps.model.Marker) -> Unit = { onPinClick?.invoke(pin) }
                if (pin.number != null) {
                    MarkerComposable(
                        pin.number,
                        state = state,
                        title = pin.title,
                        snippet = pin.detail,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        onInfoWindowClick = onInfoClick,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(colors.tertiary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = pin.number.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = colors.onTertiary,
                            )
                        }
                    }
                } else {
                    Marker(state = state, title = pin.title, snippet = pin.detail, onInfoWindowClick = onInfoClick)
                }
            }
        }
    }
}
