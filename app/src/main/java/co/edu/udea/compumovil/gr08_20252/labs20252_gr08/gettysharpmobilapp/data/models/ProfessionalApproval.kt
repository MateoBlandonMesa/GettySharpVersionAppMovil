package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models

data class ProfessionalApproval(
    val id: String,
    val userId: String,
    val fullName: String? = null,
    val email: String? = null,
    val verificationStatusId: String? = null,
    val verificationStatusName: String? = null,
    val publicUsername: String? = null,
    val documentation: String? = null
)

data class VerificationStatus(
    val id: String,
    val nombre: String
)

