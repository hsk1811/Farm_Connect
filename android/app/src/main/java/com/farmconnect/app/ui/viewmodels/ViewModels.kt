package com.farmconnect.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmconnect.app.data.models.*
import com.farmconnect.app.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

// ==================== Listing ViewModel ====================
data class ListingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val listings: List<Listing> = emptyList(),
    val myListings: List<Listing> = emptyList(),
    val selectedListing: Listing? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    // Search & Filters
    val searchQuery: String = "",
    val variety: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minQuantity: Double? = null,
    val maxQuantity: Double? = null,
    val qualityGrade: String? = null,
    val city: String? = null,
    val state: String? = null,
    val sortBy: String = "date",
    val sortOrder: String = "desc"
)

@HiltViewModel
class ListingViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val searchPreferences: com.farmconnect.app.data.local.SearchPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ListingsUiState())
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()
    
    // Debounce job for search
    private var searchJob: kotlinx.coroutines.Job? = null
    
    fun loadListings(refresh: Boolean = false) {
        val page = if (refresh) 1 else _uiState.value.currentPage
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val state = _uiState.value
            when (val result = listingRepository.getListings(
                cropType = state.searchQuery.ifEmpty { null },
                variety = state.variety,
                minPrice = state.minPrice,
                maxPrice = state.maxPrice,
                minQuantity = state.minQuantity,
                maxQuantity = state.maxQuantity,
                qualityGrade = state.qualityGrade,
                city = state.city,
                state = state.state,
                sortBy = state.sortBy,
                sortOrder = state.sortOrder,
                page = page
            )) {
                is Result.Success -> {
                    val newListings = result.data.listings ?: result.data.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        listings = if (refresh) newListings else _uiState.value.listings + newListings,
                        hasMore = newListings.isNotEmpty(),
                        currentPage = page + 1
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun loadMyListings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.getMyListings()) {
                is Result.Success -> {
                    val listings = result.data.listings ?: result.data.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, myListings = listings)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateSearchQuery(query: String) {
        // Update UI immediately for responsive typing
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchPreferences.saveSearchQuery(query)
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Debounce: wait 500ms after user stops typing before searching
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            loadListings(refresh = true)
        }
    }

    fun applyFilters(filters: com.farmconnect.app.data.local.SearchFilters) {
        _uiState.value = _uiState.value.copy(
            variety = filters.variety,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            minQuantity = filters.minQuantity,
            maxQuantity = filters.maxQuantity,
            qualityGrade = filters.qualityGrade,
            city = filters.city,
            state = filters.state,
            sortBy = filters.sortBy ?: "date",
            sortOrder = filters.sortOrder ?: "desc"
        )
        searchPreferences.saveFilters(
            variety = filters.variety,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            minQuantity = filters.minQuantity,
            maxQuantity = filters.maxQuantity,
            qualityGrade = filters.qualityGrade,
            city = filters.city,
            state = filters.state,
            sortBy = filters.sortBy,
            sortOrder = filters.sortOrder
        )
        loadListings(refresh = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            variety = null,
            minPrice = null,
            maxPrice = null,
            minQuantity = null,
            maxQuantity = null,
            qualityGrade = null,
            city = null,
            state = null,
            sortBy = "date",
            sortOrder = "desc"
        )
        searchPreferences.clearFilters()
        searchPreferences.saveSearchQuery("")
        loadListings(refresh = true)
    }

    fun loadSavedFilters() {
        val filters = searchPreferences.getSavedFilters()
        val query = searchPreferences.getSearchQuery()
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            variety = filters.variety,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            minQuantity = filters.minQuantity,
            maxQuantity = filters.maxQuantity,
            qualityGrade = filters.qualityGrade,
            city = filters.city,
            state = filters.state,
            sortBy = filters.sortBy ?: "date",
            sortOrder = filters.sortOrder ?: "desc"
        )
    }
    
    fun loadListingDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.getListingById(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, selectedListing = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun createListing(request: CreateListingRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.createListing(request)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun uploadPhotos(parts: List<MultipartBody.Part>, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.uploadImages(parts)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onResult(result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateListingStatus(id: Int, status: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.updateListingStatus(id, status)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteListing(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = listingRepository.deleteListing(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
}

// ==================== Negotiation ViewModel ====================
data class NegotiationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val negotiations: List<Negotiation> = emptyList(),
    val selectedNegotiation: Negotiation? = null
)

@HiltViewModel
class NegotiationViewModel @Inject constructor(
    private val negotiationRepository: NegotiationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NegotiationsUiState())
    val uiState: StateFlow<NegotiationsUiState> = _uiState.asStateFlow()
    
    fun loadNegotiations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = negotiationRepository.getNegotiations()) {
                is Result.Success -> {
                    val items = result.data.negotiations ?: result.data.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, negotiations = items)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun loadNegotiationDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = negotiationRepository.getNegotiationById(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, selectedNegotiation = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun createNegotiation(listingId: Int, price: Double, quantity: Double, message: String?, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = negotiationRepository.createNegotiation(
                CreateNegotiationRequest(listingId, price, quantity, message)
            )) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(result.data.id)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun sendMessage(id: Int, messageType: String, message: String?, price: Double?, quantity: Double?) {
        viewModelScope.launch {
            negotiationRepository.sendMessage(id, SendMessageRequest(messageType, message, price, quantity))
            loadNegotiationDetail(id)
        }
    }
    
    fun acceptNegotiation(
        id: Int, 
        finalPrice: Double, 
        finalQuantity: Double,
        qualityGrade: String?,
        paymentTerms: String?,
        transportResponsibility: String?,
        additionalTerms: String?,
        onSuccess: (Int) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = negotiationRepository.acceptNegotiation(
                id, 
                AcceptNegotiationRequest(
                    finalPrice, 
                    finalQuantity,
                    qualityGrade,
                    paymentTerms,
                    transportResponsibility,
                    additionalTerms
                )
            )) {
                is Result.Success -> {
                    // Backend returns: { negotiation: {...}, contract: {id: N, ...} }
                    val contractMap = result.data["contract"] as? Map<*, *>
                    val contractId = (contractMap?.get("id") as? Number)?.toInt() ?: 0
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(contractId)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
}

// ==================== Contract ViewModel ====================
data class ContractsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val contracts: List<Contract> = emptyList(),
    val selectedContract: Contract? = null
)

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val contractRepository: ContractRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ContractsUiState())
    val uiState: StateFlow<ContractsUiState> = _uiState.asStateFlow()
    
    fun loadContracts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = contractRepository.getContracts()) {
                is Result.Success -> {
                    val items = result.data.contracts ?: result.data.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, contracts = items)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun loadContractDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = contractRepository.getContractById(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, selectedContract = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun confirmContract(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = contractRepository.confirmContract(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, selectedContract = result.data)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
}

// ==================== Dispute ViewModel ====================
data class DisputesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val disputes: List<Dispute> = emptyList()
)

@HiltViewModel
class DisputeViewModel @Inject constructor(
    private val disputeRepository: DisputeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DisputesUiState())
    val uiState: StateFlow<DisputesUiState> = _uiState.asStateFlow()
    
    fun loadDisputes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = disputeRepository.getDisputes()) {
                is Result.Success -> {
                    val items = result.data.disputes ?: result.data.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, disputes = items)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun raiseDispute(contractId: Int, reason: String, description: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = disputeRepository.raiseDispute(RaiseDisputeRequest(contractId, reason, description))) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }
}
