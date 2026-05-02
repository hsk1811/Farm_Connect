package com.farmconnect.app.data.repository

import com.farmconnect.app.data.api.ApiService
import com.farmconnect.app.data.local.AuthPreferences
import com.farmconnect.app.data.models.*
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences
) {
    suspend fun login(email: String, password: String): Result<AuthData> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data!!
                authPreferences.saveAuthData(
                    token = authData.token,
                    userId = authData.userId,
                    email = authData.email,
                    role = authData.role,
                    name = authData.profile?.fullName ?: ""
                )
                Result.Success(authData)
            } else {
                Result.Error(response.body()?.error ?: "Login failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(
        email: String,
        password: String,
        role: String,
        fullName: String,
        phone: String?
    ): Result<AuthData> {
        return try {
            val response = apiService.register(
                RegisterRequest(email, password, role, fullName, phone)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()?.data!!
                authPreferences.saveAuthData(
                    token = authData.token,
                    userId = authData.userId,
                    email = authData.email,
                    role = authData.role,
                    name = fullName
                )
                Result.Success(authData)
            } else {
                Result.Error(response.body()?.error ?: "Registration failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        authPreferences.clearAuthData()
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String? = null,
        address: String? = null,
        city: String? = null
    ): Result<UserProfile> {
        return try {
            val request = UpdateProfileRequest(
                fullName = fullName,
                phone = phone,
                address = address,
                city = city
            )
            
            val response = apiService.updateProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val profile = response.body()?.data!!
                // Update saved name in preferences
                authPreferences.saveUserName(fullName)
                Result.Success(profile)
            } else {
                Result.Error(response.body()?.error ?: "Profile update failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<String> {
        return try {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            val response = apiService.changePassword(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success("Password changed successfully")
            } else {
                Result.Error(response.body()?.error ?: "Password change failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun updateFcmToken(token: String): Result<String> {
        return try {
            val request = FcmTokenRequest(token)
            val response = apiService.updateFcmToken(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success("FCM token updated")
            } else {
                Result.Error(response.body()?.error ?: "Failed to update FCM token")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    fun isLoggedIn() = authPreferences.isLoggedIn
    fun getUserRole() = authPreferences.userRole
    fun getUserName() = authPreferences.userName
}

@Singleton
class ListingRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getListings(
        cropType: String? = null,
        variety: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minQuantity: Double? = null,
        maxQuantity: Double? = null,
        qualityGrade: String? = null,
        city: String? = null,
        state: String? = null,
        harvestStartAfter: String? = null,
        harvestEndBefore: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        page: Int = 1
    ): Result<PaginatedData<Listing>> {
        return try {
            val response = apiService.getListings(
                cropType = cropType,
                variety = variety,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minQuantity = minQuantity,
                maxQuantity = maxQuantity,
                qualityGrade = qualityGrade,
                city = city,
                state = state,
                harvestStartAfter = harvestStartAfter,
                harvestEndBefore = harvestEndBefore,
                status = "active",
                sortBy = sortBy,
                sortOrder = sortOrder,
                page = page
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch listings")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun uploadImages(parts: List<MultipartBody.Part>): Result<List<String>> {
        return try {
            val response = apiService.uploadImages(parts)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data?.urls ?: emptyList())
            } else {
                Result.Error(response.body()?.error ?: "Upload failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyListings(status: String? = null, page: Int = 1): Result<PaginatedData<Listing>> {
        return try {
            val response = apiService.getMyListings(status, page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch listings")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getListingById(id: Int): Result<Listing> {
        return try {
            val response = apiService.getListingById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch listing")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createListing(request: CreateListingRequest): Result<Listing> {
        return try {
            val response = apiService.createListing(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to create listing")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateListingStatus(id: Int, status: String): Result<Listing> {
        return try {
            val response = apiService.updateListingStatus(id, UpdateStatusRequest(status))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to update listing")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteListing(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteListing(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(Unit)
            } else {
                Result.Error(response.body()?.error ?: "Failed to delete listing")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class NegotiationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNegotiations(status: String? = null, page: Int = 1): Result<PaginatedData<Negotiation>> {
        return try {
            val response = apiService.getNegotiations(status, page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch negotiations")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getNegotiationById(id: Int): Result<Negotiation> {
        return try {
            val response = apiService.getNegotiationById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch negotiation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createNegotiation(request: CreateNegotiationRequest): Result<Negotiation> {
        return try {
            val response = apiService.createNegotiation(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to create negotiation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMessage(id: Int, request: SendMessageRequest): Result<NegotiationMessage> {
        return try {
            val response = apiService.sendMessage(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun acceptNegotiation(id: Int, request: AcceptNegotiationRequest): Result<Map<String, Any>> {
        return try {
            val response = apiService.acceptNegotiation(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to accept negotiation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun rejectNegotiation(id: Int, reason: String?): Result<Negotiation> {
        return try {
            val response = apiService.rejectNegotiation(id, mapOf("reason" to (reason ?: "")))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to reject negotiation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

@Singleton 
class ContractRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getContracts(status: String? = null, page: Int = 1): Result<PaginatedData<Contract>> {
        return try {
            val response = apiService.getContracts(status, page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch contracts")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getContractById(id: Int): Result<Contract> {
        return try {
            val response = apiService.getContractById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch contract")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun confirmContract(id: Int): Result<Contract> {
        return try {
            val response = apiService.confirmContract(id, mapOf("confirmed" to true))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to confirm contract")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createMilestone(contractId: Int, request: CreateMilestoneRequest): Result<Milestone> {
        return try {
            val response = apiService.createMilestone(contractId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to create milestone")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun recordPayment(contractId: Int, request: RecordPaymentRequest): Result<Payment> {
        return try {
            val response = apiService.recordPayment(contractId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to record payment")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class DisputeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getDisputes(status: String? = null, page: Int = 1): Result<PaginatedData<Dispute>> {
        return try {
            val response = apiService.getDisputes(status, page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch disputes")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun raiseDispute(request: RaiseDisputeRequest): Result<Dispute> {
        return try {
            val response = apiService.raiseDispute(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to raise dispute")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

@Singleton
class FavoriteRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getFavorites(page: Int = 1): Result<PaginatedData<Favorite>> {
        return try {
            val response = apiService.getFavorites(page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to fetch favorites")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun addFavorite(listingId: Int): Result<Favorite> {
        return try {
            val response = apiService.addFavorite(AddFavoriteRequest(listingId))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to add favorite")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun removeFavorite(listingId: Int): Result<Unit> {
        return try {
            val response = apiService.removeFavorite(listingId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to remove favorite")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
