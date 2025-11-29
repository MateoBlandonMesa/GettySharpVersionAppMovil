package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BarbersUiState(
    val isLoading: Boolean = false,
    val barbers: List<Barber> = emptyList(),
    val errorMessage: String? = null
)

class BarbersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BarbersUiState())
    val uiState: StateFlow<BarbersUiState> = _uiState.asStateFlow()
    
    init {
        loadBarbers()
    }
    
    fun loadBarbers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = SupabaseRestClient.getVerifiedBarbers()
            result.fold(
                onSuccess = { barbers ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        barbers = barbers,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        barbers = emptyList(),
                        errorMessage = error.message ?: "Error desconocido"
                    )
                }
            )
        }
    }
    
    fun refreshBarbers() {
        loadBarbers()
    }
}

