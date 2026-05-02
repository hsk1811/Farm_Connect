package com.farmconnect.app.ui.screens.listings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.ui.components.StatusChip
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.ui.viewmodels.ListingViewModel
import com.farmconnect.app.ui.viewmodels.NegotiationViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listingId: Int,
    onNavigateBack: () -> Unit,
    onStartNegotiation: (Int) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    listingViewModel: ListingViewModel = hiltViewModel(),
    negotiationViewModel: NegotiationViewModel = hiltViewModel()
) {
    val userRole by authViewModel.userRole.collectAsState(initial = "buyer")
    val listingState by listingViewModel.uiState.collectAsState()
    val negotiationState by negotiationViewModel.uiState.collectAsState()
    val listing = listingState.selectedListing
    
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var showNegotiationDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var proposedPrice by remember { mutableStateOf("") }
    var proposedQuantity by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    LaunchedEffect(listingId) {
        listingViewModel.loadListingDetail(listingId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        if (listingState.isLoading || listing == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image - Crop Specific
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Crop Background Image
                    Image(
                        painter = painterResource(id = com.farmconnect.app.utils.CropImageMapper.getCropImage(listing.cropType)),
                        contentDescription = listing.cropType,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.5f
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF66BB6A).copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    // Crop Emoji Icon
                    Text(
                        text = com.farmconnect.app.utils.CropImageMapper.getCropEmoji(listing.cropType),
                        fontSize = 80.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Photo Gallery - Full Pager
                listing.photos?.takeIf { it.isNotEmpty() }?.let { photos ->
                    val baseUrl = remember { 
                        com.farmconnect.app.BuildConfig.API_BASE_URL.replace("/api/", "") 
                    }
                    val photoUrls = remember(photos) {
                        photos.map { photo ->
                            if (photo.photoUrl.startsWith("http")) {
                                photo.photoUrl
                            } else {
                                "$baseUrl${photo.photoUrl}"
                            }
                        }
                    }
                    
                    PhotoGallery(photos = photoUrls)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title and Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = listing.cropType,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (listing.variety != null) {
                                Text(listing.variety, color = Color.Gray)
                            }
                        }
                        StatusChip(listing.status)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Price Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Price Range", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "₹${listing.minPrice} - ₹${listing.maxPrice} per ${listing.unit}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailCard(
                            icon = Icons.Default.Scale,
                            label = "Quantity",
                            value = "${listing.quantity} ${listing.unit}",
                            modifier = Modifier.weight(1f)
                        )
                        DetailCard(
                            icon = Icons.Default.Star,
                            label = "Grade",
                            value = listing.qualityGrade ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailCard(
                            icon = Icons.Default.CalendarToday,
                            label = "Harvest Start",
                            value = listing.harvestStartDate,
                            modifier = Modifier.weight(1f)
                        )
                        DetailCard(
                            icon = Icons.Default.Event,
                            label = "Harvest End",
                            value = listing.harvestEndDate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Location
                    if (listing.locationAddress != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Location", fontSize = 12.sp, color = Color.Gray)
                                    Text(listing.locationAddress, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    
                    // Farmer Info
                    if (listing.farmer != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Farmer", fontSize = 12.sp, color = Color.Gray)
                                    Text(listing.farmer.fullName, fontWeight = FontWeight.Medium)
                                    if (listing.farmer.city != null) {
                                        Text(
                                            "${listing.farmer.city}, ${listing.farmer.state ?: ""}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Description
                    if (listing.description != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(listing.description, color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Status Control Buttons (for farmers viewing their own listing)
                    // Note: Farmers only see their own listings via /my endpoint, so if role=farmer, they own it
                    
                    if (userRole == "farmer") {
                        Text(
                            "Listing Actions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        when (listing.status) {
                            "active" -> {
                                // Active listing: can mark sold or delete
                                OutlinedButton(
                                    onClick = {
                                        listingViewModel.updateListingStatus(listing.id, "sold") {
                                            // Reload to show updated status
                                            listingViewModel.loadListingDetail(listingId)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF2E7D32)
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Sold", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFD32F2F)
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Listing")
                                }
                            }
                            "sold", "inactive", "paused" -> {
                                // Sold/Inactive listing: can only reactivate
                                Button(
                                    onClick = {
                                        listingViewModel.updateListingStatus(listing.id, "active") {
                                            // Reload the listing to show updated status
                                            listingViewModel.loadListingDetail(listingId)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reactivate Listing", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Action Button (for buyers)
                    if (userRole == "buyer" && listing.status == "active") {
                        Button(
                            onClick = { showNegotiationDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Negotiation", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Negotiation Dialog
    if (showNegotiationDialog) {
        AlertDialog(
            onDismissRequest = { showNegotiationDialog = false },
            title = { Text("Start Negotiation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = proposedPrice,
                        onValueChange = { proposedPrice = it },
                        label = { Text("Proposed Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = proposedQuantity,
                        onValueChange = { proposedQuantity = it },
                        label = { Text("Quantity (${listing?.unit ?: ""})") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        listing?.let {
                            negotiationViewModel.createNegotiation(
                                listingId = it.id,
                                price = proposedPrice.toDoubleOrNull() ?: 0.0,
                                quantity = proposedQuantity.toDoubleOrNull() ?: 0.0,
                                message = message.ifBlank { null }
                            ) { negotiationId ->
                                showNegotiationDialog = false
                                onStartNegotiation(negotiationId)
                            }
                        }
                    },
                    enabled = !negotiationState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (negotiationState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Submit Proposal")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegotiationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Delete Listing?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete this listing? This action cannot be undone.\n\n" +
                    "Note: You cannot delete listings with active negotiations or contracts.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        listing?.let {
                            listingViewModel.deleteListing(it.id) {
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        }
                    },
                    enabled = !listingState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    if (listingState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Error Snackbar
    listingState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
        }
    }
}

@Composable
fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoGallery(photos: List<String>) {
    if (photos.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (photos.size == 1) {
                // Single photo
                AsyncImage(
                    model = photos[0],
                    contentDescription = "Listing photo",
                    contentScale = ContentScale.Fit,  // Show full image without cropping
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),  // Black background for letterboxing
                    error = painterResource(id = com.farmconnect.app.R.drawable.ic_launcher_logo)
                )
            } else {
                // Multiple photos with pager
                val pagerState = rememberPagerState { photos.size }
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { page ->
                    AsyncImage(
                        model = photos[page],
                        contentDescription = "Listing photo ${page + 1}",
                        contentScale = ContentScale.Fit,  // Show full image without cropping
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),  // Black background for letterboxing
                        error = painterResource(id = com.farmconnect.app.R.drawable.ic_launcher_logo)
                    )
                }
                
                // Page indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(photos.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .background(
                                    color = if (index == pagerState.currentPage) 
                                        Color(0xFF2E7D32) 
                                    else 
                                        Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
