package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models

data class UserProfile(
    val id: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val gender: String = "",
    val address: String = "",
    val fotoPerfil: String? = null,
    val isBarber: Boolean = false,
    val professionalId: String? = null,
    val username: String? = null,
    val specialty: String? = null,
    val workLocation: String? = null,
    val description: String? = null,
    val verified: Boolean = false,
    val verificationStatus: String? = null,
    val rating: Double = 0.0,
    val ratingsCount: Int = 0,
    val isApprover: Boolean = false
)

data class Barber(
    val id: String,
    val nombre: String?,
    val apellido: String?,
    val fotoPerfil: String?,
    val latitud: Double?,
    val longitud: Double?,
    val direccion: String?,
    val ubicacionId: String?,
    val verificationStatusId: String?,
    val lugarDeTrabajo: Int?,
    val ratingAverage: Double?,
    val ratingsCount: Int?
)


