package com.farmconnect.app.ui.screens.listings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.Listing
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.ui.viewmodels.ListingViewModel
import androidx.compose.ui.res.stringResource
import com.farmconnect.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToCreate: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    listingViewModel: ListingViewModel = hiltViewModel()
) {
    val userRole by authViewModel.userRole.collectAsState(initial = "farmer")
    val uiState by listingViewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(userRole) {
        if (userRole == "farmer") {
            listingViewModel.loadMyListings()
        } else {
            listingViewModel.loadSavedFilters()
            listingViewModel.loadListings(refresh = true)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (userRole == "farmer") stringResource(R.string.my_listings) else stringResource(R.string.listings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (userRole != "farmer") {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (userRole == "farmer") {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = Color(0xFF2E7D32)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White)
                }
            }
        }
    ) { padding ->
        val listings = if (userRole == "farmer") uiState.myListings else uiState.listings
        
        Column(modifier = Modifier.padding(padding)) {
            // Search bar for buyers
            if (userRole != "farmer") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    com.farmconnect.app.ui.components.SearchTextField(
                        value = uiState.searchQuery,
                        onValueChange = { listingViewModel.updateSearchQuery(it) }
                    )
                }
                
                // Filter chips
                com.farmconnect.app.ui.components.FilterChipsRow(
                    searchQuery = uiState.searchQuery,
                    variety = uiState.variety,
                    qualityGrade = uiState.qualityGrade,
                    minPrice = uiState.minPrice,
                    maxPrice = uiState.maxPrice,
                    minQuantity = uiState.minQuantity,
                    maxQuantity = uiState.maxQuantity,
                    city = uiState.city,
                    state = uiState.state,
                    sortLabel = getSortLabel(uiState.sortBy, uiState.sortOrder),
                    onClearSearch = { listingViewModel.updateSearchQuery("") },
                    onClearVariety = { 
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                qualityGrade = uiState.qualityGrade,
                                city = uiState.city,
                                state = uiState.state,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearQualityGrade = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                city = uiState.city,
                                state = uiState.state,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearPriceRange = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                qualityGrade = uiState.qualityGrade,
                                city = uiState.city,
                                state = uiState.state,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearQuantityRange = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                qualityGrade = uiState.qualityGrade,
                                city = uiState.city,
                                state = uiState.state,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearCity = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                qualityGrade = uiState.qualityGrade,
                                state = uiState.state,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearState = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                qualityGrade = uiState.qualityGrade,
                                city = uiState.city,
                                sortBy = uiState.sortBy,
                                sortOrder = uiState.sortOrder
                            )
                        )
                    },
                    onClearSort = {
                        listingViewModel.applyFilters(
                            com.farmconnect.app.data.local.SearchFilters(
                                variety = uiState.variety,
                                minPrice = uiState.minPrice,
                                maxPrice = uiState.maxPrice,
                                minQuantity = uiState.minQuantity,
                                maxQuantity = uiState.maxQuantity,
                                qualityGrade = uiState.qualityGrade,
                                city = uiState.city,
                                state = uiState.state
                            )
                        )
                    },
                    onClearAll = { listingViewModel.clearFilters() }
                )
            }
        
            if (uiState.isLoading && listings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (listings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Grass,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No listings found",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        if (userRole == "farmer") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Create Your First Listing")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = listings,
                        key = { it.id } // Performance: stable keys for better recomposition
                    ) { listing ->
                        ListingListItem(
                            listing = listing,
                            onClick = { onNavigateToDetail(listing.id) }
                        )
                    }
                }
            }
        }
        
        // Filter bottom sheet
        if (showFilterSheet) {
            com.farmconnect.app.ui.components.FilterBottomSheet(
                currentFilters = com.farmconnect.app.data.local.SearchFilters(
                    variety = uiState.variety,
                    minPrice = uiState.minPrice,
                    maxPrice = uiState.maxPrice,
                    minQuantity = uiState.minQuantity,
                    maxQuantity = uiState.maxQuantity,
                    qualityGrade = uiState.qualityGrade,
                    city = uiState.city,
                    state = uiState.state,
                    sortBy = uiState.sortBy,
                    sortOrder = uiState.sortOrder
                ),
                onApplyFilters = { listingViewModel.applyFilters(it) },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

private fun getSortLabel(sortBy: String, sortOrder: String): String? {
    return when (sortBy to sortOrder) {
        "price" to "asc" -> "Price: Low-High"
        "price" to "desc" -> "Price: High-Low"
        "quantity" to "asc" -> "Qty: Low-High"
        "quantity" to "desc" -> "Qty: High-Low"
        "date" to "desc" -> null // Default, don't show chip
        "date" to "asc" -> "Oldest First"
        else -> null
    }
}

@Composable
fun ListingListItem(listing: Listing, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Crop Image
            Box(
                modifier = Modifier
                    .size(80.dp)
            ) {
                Image(
                    painter = painterResource(id = com.farmconnect.app.utils.CropImageMapper.getCropImage(listing.cropType)),
                    contentDescription = listing.cropType,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF81C784).copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                
                // Crop emoji
                Text(
                    text = com.farmconnect.app.utils.CropImageMapper.getCropEmoji(listing.cropType),
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.cropType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    StatusChip(listing.status)
                }
                
                if (listing.variety != null) {
                    Text(
                        text = listing.variety,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${listing.quantity} ${listing.unit}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "₹${listing.minPrice} - ₹${listing.maxPrice} per ${listing.unit}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, bgColor) = when(status) {
        "active" -> Color(0xFF2E7D32) to Color(0xFF4CAF50).copy(alpha = 0.15f)
        "draft" -> Color.Gray to Color.Gray.copy(alpha = 0.15f)
        "paused" -> Color(0xFFF57C00) to Color(0xFFFF9800).copy(alpha = 0.15f)
        "closed" -> Color(0xFF757575) to Color.Gray.copy(alpha = 0.15f)
        "sold" -> Color(0xFF1976D2) to Color(0xFF2196F3).copy(alpha = 0.15f)
        else -> Color.Gray to Color.Gray.copy(alpha = 0.15f)
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
