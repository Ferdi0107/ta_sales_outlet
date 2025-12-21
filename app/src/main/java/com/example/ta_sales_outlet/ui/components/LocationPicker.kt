package com.example.ta_sales_outlet.ui.components

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// --- IMPORT YANG DITAMBAHKAN ---
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
// ------------------------------
import com.example.ta_sales_outlet.data.api.GooglePlacesRepository
import com.example.ta_sales_outlet.data.api.PlacePrediction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPicker(
    modifier: Modifier = Modifier,
    initialLat: Double = -6.200000,
    initialLng: Double = 106.816666,
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current

    // 1. PERBAIKAN: DEFINISIKAN SCOPE DI SINI
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 15f)
    }

    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var currentAddress by remember { mutableStateOf("Mengambil alamat...") }
    var isMapMoving by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            isMapMoving = true
            currentAddress = "Mencari titik..."
        } else {
            isMapMoving = false
            val target = cameraPositionState.position.target
            val lat = target.latitude
            val lng = target.longitude

            try {
                // Pindah ke background thread untuk Geocoder agar UI tidak lag
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)

                    // Kembali ke Main Thread untuk update UI
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0].getAddressLine(0)
                            currentAddress = address
                            onLocationSelected(lat, lng, address)
                        } else {
                            currentAddress = "Alamat tidak ditemukan"
                            onLocationSelected(lat, lng, "")
                        }
                    }
                }
            } catch (e: Exception) {
                currentAddress = "Gagal memuat alamat"
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(400.dp)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        )

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 24.dp)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length > 3) {
                        isSearching = true
                        GooglePlacesRepository.searchPlaces(it) { list ->
                            predictions = list
                        }
                    } else {
                        predictions = emptyList()
                        isSearching = false
                    }
                },
                placeholder = { Text("Cari lokasi...") },
                modifier = Modifier.fillMaxWidth().background(Color.White),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            predictions = emptyList()
                        }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (predictions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .heightIn(max = 200.dp)
                ) {
                    items(predictions) { place ->
                        Text(
                            text = place.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = ""
                                    predictions = emptyList()
                                    isSearching = false

                                    GooglePlacesRepository.getPlaceCoordinates(place.place_id) { lat, lng ->
                                        // 2. PERBAIKAN: GUNAKAN SCOPE UNTUK ANIMASI
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f)
                                            )
                                        }
                                    }
                                }
                                .padding(16.dp)
                        )
                        Divider()
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            // 3. PERBAIKAN: FontWeight SUDAH DIIMPORT
            Text(
                text = currentAddress,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}