package com.example.ta_sales_outlet.data.api

import android.util.Log
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.ta_sales_outlet.BuildConfig

// Model Data Response
data class PlacesResponse(
    val predictions: List<PlacePrediction>,
    val status: String?, // Tambahan untuk cek error dari Google
    val error_message: String? // Tambahan info error
)
data class PlacePrediction(val description: String, val place_id: String)

data class PlaceDetailsResponse(val result: PlaceResult?, val status: String?)
data class PlaceResult(val geometry: Geometry)
data class Geometry(val location: LocationLatLng)
data class LocationLatLng(val lat: Double, val lng: Double)

interface GooglePlacesService {
    @GET("place/autocomplete/json")
    fun getPredictions(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:id"
    ): Call<PlacesResponse>

    @GET("place/details/json")
    fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): Call<PlaceDetailsResponse>
}

object GooglePlacesRepository {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

    private const val API_KEY = BuildConfig.MAPS_API_KEY

    private val service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GooglePlacesService::class.java)

    fun searchPlaces(query: String, callback: (List<PlacePrediction>) -> Unit) {
        Log.d("DEBUG_MAPS", "Mencari: $query | Key: $API_KEY")

        Thread {
            try {
                val response = service.getPredictions(query, API_KEY).execute()

                // Cek Response Code HTTP
                if (!response.isSuccessful) {
                    Log.e("DEBUG_MAPS", "HTTP Error: ${response.code()} - ${response.errorBody()?.string()}")
                    return@Thread
                }

                val body = response.body()
                Log.d("DEBUG_MAPS", "Status API: ${body?.status}")

                if (body?.status == "OK" || body?.status == "ZERO_RESULTS") {
                    val list = body.predictions
                    Log.d("DEBUG_MAPS", "Ditemukan ${list.size} lokasi")
                    callback(list)
                } else {
                    // Log Error Spesifik dari Google (Misal: REQUEST_DENIED, OVER_QUERY_LIMIT)
                    Log.e("DEBUG_MAPS", "Google API Error: ${body?.status} - ${body?.error_message}")
                }
            } catch (e: Exception) {
                Log.e("DEBUG_MAPS", "Exception: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    fun getPlaceCoordinates(placeId: String, callback: (Double, Double) -> Unit) {
        Log.d("DEBUG_MAPS", "Get Detail Place ID: $placeId")
        Thread {
            try {
                val response = service.getPlaceDetails(placeId, API_KEY).execute()
                val body = response.body()

                if (body?.status == "OK" && body.result != null) {
                    val loc = body.result.geometry.location
                    Log.d("DEBUG_MAPS", "Koordinat Ditemukan: ${loc.lat}, ${loc.lng}")
                    callback(loc.lat, loc.lng)
                } else {
                    Log.e("DEBUG_MAPS", "Gagal Detail: ${body?.status}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}