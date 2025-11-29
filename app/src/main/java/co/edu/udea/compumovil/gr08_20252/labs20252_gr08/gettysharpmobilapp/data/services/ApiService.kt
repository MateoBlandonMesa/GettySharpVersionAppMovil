package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAppointmentRequest
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/api/professionals/{professionalId}/schedule/availability")
    suspend fun getAvailability(
        @Path("professionalId") professionalId: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): Response<List<AvailabilityBlock>>
    
    @POST("/api/appointments")
    suspend fun createAppointment(@Body request: CreateAppointmentRequest): Response<Appointment>
    
    @GET("/api/appointments/by-client/{clientId}")
    suspend fun getAppointmentsByClient(@Path("clientId") clientId: String): Response<List<Appointment>>
    
    @GET("/api/appointments/by-professional/{professionalId}")
    suspend fun getAppointmentsByProfessional(@Path("professionalId") professionalId: String): Response<List<Appointment>>
    
    @PATCH("/api/appointments/{id}/status")
    suspend fun updateAppointmentStatus(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<Appointment>
    
    @DELETE("/api/appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: String): Response<Unit>
    
    @GET("/api/appointments/statuses")
    suspend fun getAppointmentStatuses(): Response<List<Map<String, String>>>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:5000" // Android emulator localhost
    // For physical device, use: "http://YOUR_IP:5000"
    
    val service: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}


