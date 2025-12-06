package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAppointmentRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAvailabilityBlockRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.ProfessionalApproval
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.VerificationStatus
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
import java.util.concurrent.TimeUnit

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
    
    @GET("/api/ratings/professionals/{professionalId}/summary")
    suspend fun getProfessionalRatingSummary(
        @Path("professionalId") professionalId: String
    ): Response<Map<String, Any>>
    
    @GET("/api/ratings/professionals/summary")
    suspend fun getProfessionalRatingSummaries(
        @Query("ids") ids: String
    ): Response<Map<String, Map<String, Any>>>
    
    @PATCH("/api/appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<Appointment>
    
    @POST("/api/ratings/appointments/{appointmentId}")
    suspend fun submitRating(
        @Path("appointmentId") appointmentId: String,
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>
    
    @GET("/api/ratings/clients/{clientId}/history")
    suspend fun getClientHistory(
        @Path("clientId") clientId: String
    ): Response<List<Map<String, Any>>>
    
    @POST("/api/professionals/{professionalId}/schedule/availability")
    suspend fun createAvailabilityBlock(
        @Path("professionalId") professionalId: String,
        @Body request: CreateAvailabilityBlockRequest
    ): Response<AvailabilityBlock>
    
    @DELETE("/api/professionals/{professionalId}/schedule/availability/{availabilityId}")
    suspend fun deleteAvailabilityBlock(
        @Path("professionalId") professionalId: String,
        @Path("availabilityId") availabilityId: String
    ): Response<Unit>
    
    @POST("/api/geocoding")
    suspend fun geocodeAddress(
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>
    
    @GET("/api/approvals/statuses")
    suspend fun getVerificationStatuses(): Response<List<VerificationStatus>>
    
    @GET("/api/approvals/professionals")
    suspend fun getProfessionalsForApproval(
        @Query("approverId") approverId: String,
        @Query("statusId") statusId: String? = null
    ): Response<List<ProfessionalApproval>>
    
    @PATCH("/api/approvals/professionals/{professionalId}/status")
    suspend fun updateProfessionalVerificationStatus(
        @Path("professionalId") professionalId: String,
        @Query("approverId") approverId: String,
        @Body request: Map<String, String>
    ): Response<ProfessionalApproval>
}

object ApiClient {
    private const val BASE_URL = "https://getty-sharp-hub.onrender.com"
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val service: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}


