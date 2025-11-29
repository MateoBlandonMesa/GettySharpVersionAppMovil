package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.ClientHistoryItem
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Rating
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RatingUiState(
    val isLoading: Boolean = false,
    val history: List<ClientHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false
)

class RatingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RatingUiState())
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()
    
    fun loadClientHistory(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response = ApiClient.service.getClientHistory(clientId)
                
                if (response.isSuccessful) {
                    val historyItems = response.body()?.mapNotNull { item ->
                        try {
                            ClientHistoryItem(
                                appointmentId = item["appointmentId"]?.toString() ?: return@mapNotNull null,
                                professionalId = item["professionalId"]?.toString() ?: "",
                                professionalName = item["professionalName"]?.toString(),
                                professionalPublicName = item["professionalPublicName"]?.toString(),
                                start = item["start"]?.toString() ?: "",
                                end = item["end"]?.toString() ?: "",
                                statusName = item["statusName"]?.toString(),
                                rating = (item["rating"] as? Map<*, *>)?.let { ratingMap ->
                                    Rating(
                                        id = ratingMap["id"]?.toString() ?: "",
                                        professionalId = ratingMap["professionalId"]?.toString() ?: "",
                                        score = (ratingMap["score"] as? Number)?.toInt() ?: 0,
                                        comment = ratingMap["comment"]?.toString(),
                                        createdAt = ratingMap["createdAt"]?.toString()
                                    )
                                },
                                canRate = (item["canRate"] as? Boolean) ?: false
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("RatingViewModel", "Error parsing history item: ${e.message}")
                            null
                        }
                    } ?: emptyList()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        history = historyItems,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        history = emptyList(),
                        errorMessage = "Error al cargar historial: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Failed to connect") == true || 
                    e.message?.contains("timeout") == true ||
                    e.message?.contains("Unable to resolve host") == true ->
                        "No se pudo conectar al servidor. Verifica que el backend esté corriendo."
                    else -> e.message ?: "Error desconocido al cargar el historial"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    history = emptyList(),
                    errorMessage = errorMsg
                )
            }
        }
    }
    
    fun submitRating(
        appointmentId: String,
        clientId: String,
        score: Int,
        comment: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, submitSuccess = false)
            
            try {
                val request = mutableMapOf<String, Any>(
                    "clientId" to clientId,
                    "score" to score
                )
                comment?.takeIf { it.isNotBlank() }?.let {
                    request["comment"] = it.trim()
                }
                
                val response = ApiClient.service.submitRating(appointmentId, request)
                
                if (response.isSuccessful) {
                    val ratingMap = response.body()
                    val rating = ratingMap?.let {
                        Rating(
                            id = it["id"]?.toString() ?: "",
                            professionalId = it["professionalId"]?.toString() ?: "",
                            score = (it["score"] as? Number)?.toInt() ?: score,
                            comment = it["comment"]?.toString() ?: comment,
                            createdAt = it["createdAt"]?.toString()
                        )
                    }
                    
                    // Update the history item with the new rating
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        history = _uiState.value.history.map { item ->
                            if (item.appointmentId == appointmentId) {
                                item.copy(
                                    rating = rating,
                                    canRate = false
                                )
                            } else {
                                item
                            }
                        },
                        errorMessage = null
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Error al guardar calificación: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun resetSubmitSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

