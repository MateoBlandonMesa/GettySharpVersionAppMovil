package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models

data class ProfessionalRatingSummary(
    val professionalId: String,
    val averageScore: Double,
    val ratingsCount: Int
)

data class ProfessionalData(
    val id: String,
    val publicUsername: String?,
    val specialty: String?,
    val workLocation: String?,
    val description: String?,
    val verificationStatusName: String?,
    val ratingAverage: Double?,
    val ratingsCount: Int?
)

