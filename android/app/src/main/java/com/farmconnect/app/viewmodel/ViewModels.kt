package com.farmconnect.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmconnect.app.data.models.*
import com.farmconnect.app.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ===== Listing ViewModel =====
data class ListingUiState(
    val isLoading: Boolean = false,
    val listings: List<Listing> = emptyList(),
    val myListings: List<Listing> = emptyList(),
    val selectedListing: Listing? = null,
    val pagination: Pagination? = null,
    val error: String? = null,
    val actionSuccess: Boolean = false,
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

    private val _uiState = MutableStateFlow(ListingUiState())
    val uiState: StateFlow<ListingUiState> = _uiState.asStateFlow()

    fun loadListings(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
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
                sortOrder = state.sortOrder
            )) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            listings = result.data.listings ?: emptyList(),
                            pagination = result.data.pagination
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun loadMyListings(status: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = listingRepository.getMyListings(status)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            myListings = result.data.listings ?: emptyList(),
                            pagination = result.data.pagination
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchPreferences.saveSearchQuery(query)
        loadListings()
    }

    fun applyFilters(filters: com.farmconnect.app.data.local.SearchFilters) {
        _uiState.update { 
            it.copy(
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
        loadListings()
    }

    fun clearFilters() {
        _uiState.update { 
            it.copy(
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
        }
        searchPreferences.clearFilters()
        searchPreferences.saveSearchQuery("")
        loadListings()
    }

    fun loadSavedFilters() {
        val filters = searchPreferences.getSavedFilters()
        val query = searchPreferences.getSearchQuery()
        _uiState.update { 
            it.copy(
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
    }

    fun loadListingById(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = listingRepository.getListingById(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedListing = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun createListing(request: CreateListingRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = listingRepository.createListing(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun updateListingStatus(id: Int, status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = listingRepository.updateListingStatus(id, status)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                    loadMyListings()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun resetActionSuccess() {
        _uiState.update { it.copy(actionSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ===== Negotiation ViewModel =====
data class NegotiationUiState(
    val isLoading: Boolean = false,
    val negotiations: List<Negotiation> = emptyList(),
    val selectedNegotiation: Negotiation? = null,
    val pagination: Pagination? = null,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class NegotiationViewModel @Inject constructor(
    private val negotiationRepository: NegotiationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NegotiationUiState())
    val uiState: StateFlow<NegotiationUiState> = _uiState.asStateFlow()

    fun loadNegotiations(status: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = negotiationRepository.getNegotiations(status)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            negotiations = result.data.negotiations ?: emptyList(),
                            pagination = result.data.pagination
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun loadNegotiationById(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = negotiationRepository.getNegotiationById(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedNegotiation = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun createNegotiation(listingId: Int, price: Double, quantity: Double, message: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = CreateNegotiationRequest(listingId, price, quantity, message)
            when (val result = negotiationRepository.createNegotiation(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun sendMessage(id: Int, messageType: String, message: String?, price: Double?, quantity: Double?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = SendMessageRequest(messageType, message, price, quantity)
            when (val result = negotiationRepository.sendMessage(id, request)) {
                is Result.Success -> {
                    loadNegotiationById(id)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun acceptNegotiation(id: Int, price: Double, quantity: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = AcceptNegotiationRequest(price, quantity)
            when (val result = negotiationRepository.acceptNegotiation(id, request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun rejectNegotiation(id: Int, reason: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = negotiationRepository.rejectNegotiation(id, reason)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun resetActionSuccess() {
        _uiState.update { it.copy(actionSuccess = false) }
    }
}

// ===== Contract ViewModel =====
data class ContractUiState(
    val isLoading: Boolean = false,
    val contracts: List<Contract> = emptyList(),
    val selectedContract: Contract? = null,
    val pagination: Pagination? = null,
    val error: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val contractRepository: ContractRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractUiState())
    val uiState: StateFlow<ContractUiState> = _uiState.asStateFlow()

    fun loadContracts(status: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = contractRepository.getContracts(status)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            contracts = result.data.contracts ?: emptyList(),
                            pagination = result.data.pagination
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun loadContractById(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = contractRepository.getContractById(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedContract = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun confirmContract(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = contractRepository.confirmContract(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedContract = result.data, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun createMilestone(contractId: Int, type: String, date: String?, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = CreateMilestoneRequest(type, date, notes)
            when (val result = contractRepository.createMilestone(contractId, request)) {
                is Result.Success -> {
                    loadContractById(contractId)
                    _uiState.update { it.copy(actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun recordPayment(contractId: Int, amount: Double, method: String, status: String, transactionId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = RecordPaymentRequest(amount, method, status, transactionId)
            when (val result = contractRepository.recordPayment(contractId, request)) {
                is Result.Success -> {
                    loadContractById(contractId)
                    _uiState.update { it.copy(actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun resetActionSuccess() {
        _uiState.update { it.copy(actionSuccess = false) }
    }
}

// ===== Dispute ViewModel =====
data class DisputeUiState(
    val isLoading: Boolean = false,
    val disputes: List<Dispute> = emptyList(),
    val error: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class DisputeViewModel @Inject constructor(
    private val disputeRepository: DisputeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisputeUiState())
    val uiState: StateFlow<DisputeUiState> = _uiState.asStateFlow()

    fun loadDisputes(status: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = disputeRepository.getDisputes(status)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            disputes = result.data.disputes ?: emptyList()
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun raiseDispute(contractId: Int, reason: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = RaiseDisputeRequest(contractId, reason, description)
            when (val result = disputeRepository.raiseDispute(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun resetActionSuccess() {
        _uiState.update { it.copy(actionSuccess = false) }
    }
}
