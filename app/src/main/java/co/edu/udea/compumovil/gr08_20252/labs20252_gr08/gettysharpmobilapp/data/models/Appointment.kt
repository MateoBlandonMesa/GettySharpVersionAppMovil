package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models

data class Appointment(
    val id: String,
    val idCliente: String,
    val idProfesional: String,
    val fechaInicioCita: String,
    val fechaFinCita: String,
    val idEstadoCita: String?,
    val idCalificacionCita: String?,
    val ubicacionCita: String?,
    val professionalName: String? = null,
    val professionalPublicName: String? = null,
    val professionalAddress: String? = null,
    val clientName: String? = null,
    val clientEmail: String? = null,
    val clientAddress: String? = null,
    val statusName: String? = null,
    val locationAddress: String? = null,
    val locationSource: String? = null
)

data class AvailabilityBlock(
    val id: String,
    val professionalId: String,
    val start: String,
    val end: String,
    val minimumSlotLength: Any?,
    val status: String?,
    val associatedAppointmentId: String? = null
)

data class CreateAppointmentRequest(
    val idCliente: String,
    val idProfesional: String,
    val fechaInicioCita: String,
    val fechaFinCita: String,
    val availabilityBlockId: String?,
    val ubicacionCita: String?
)

data class Rating(
    val id: String,
    val professionalId: String,
    val score: Int,
    val comment: String? = null,
    val createdAt: String? = null
)

data class ClientHistoryItem(
    val appointmentId: String,
    val professionalId: String,
    val professionalName: String? = null,
    val professionalPublicName: String? = null,
    val start: String,
    val end: String,
    val statusName: String? = null,
    val rating: Rating? = null,
    val canRate: Boolean = false
)


