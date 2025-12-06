package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models

data class RatingRecord(
    val id: String,
    val professionalId: String,
    val score: Int,
    val comment: String?,
    val createdAt: String?
)

data class ClientHistoryItem(
    val appointmentId: String,
    val professionalId: String,
    val professionalName: String?,
    val professionalPublicName: String?,
    val start: String,
    val end: String,
    val statusName: String?,
    val rating: RatingRecord?,
    val canRate: Boolean
)

data class AppointmentStatusOption(
    val id: String,
    val nombre: String
)

