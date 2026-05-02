package com.farmconnect.app.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.Listing
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.ui.viewmodels.ListingViewModel
import com.farmconnect.app.ui.viewmodels.NotificationViewModel
import androidx.compose.ui.res.stringResource
import com.farmconnect.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToListings: () -> Unit,
    onNavigateToNegotiations: () -> Unit,
    onNavigateToContracts: () -> Unit,
    onNavigateToDisputes: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToListingDetail: (Int) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    listingViewModel: ListingViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val userName by authViewModel.userName.collectAsState(initial = "User")
    val userRole by authViewModel.userRole.collectAsState(initial = "farmer")
    val listingsState by listingViewModel.uiState.collectAsState()
    val notificationState by notificationViewModel.uiState.collectAsState()
    
    LaunchedEffect(userRole) {
        if (userRole == "farmer") {
            listingViewModel.loadMyListings()
        } else {
            listingViewModel.loadListings(refresh = true)
        }
    }
    
    // Optimized polling - 20 seconds for better performance (was 10s)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(20000) // 20 seconds
            notificationViewModel.loadNotifications("all")
        }
    }
    
    val gradientColors = listOf(
        Color(0xFF1B5E20),
        Color(0xFF2E7D32)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    // Notification Bell with Badge
                    Box {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                        if (notificationState.unreadCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = if (notificationState.unreadCount > 99) "99+" else notificationState.unreadCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = "Profile",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { authViewModel.logout(onLogout) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20)
                )
            )
        },
        floatingActionButton = {
            if (userRole == "farmer") {
                FloatingActionButton(
                    onClick = onNavigateToCreateListing,
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Listing")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(gradientColors))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.hello, userName ?: "User") + " 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (userRole == "farmer") stringResource(R.string.manage_farm_listings)
                                   else stringResource(R.string.find_fresh_produce),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Quick Actions Grid
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.quick_actions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Outlined.Grass,
                            title = stringResource(R.string.listings),
                            subtitle = if (userRole == "farmer") stringResource(R.string.my_crops) else stringResource(R.string.browse),
                            color = Color(0xFF4CAF50),
                            onClick = onNavigateToListings,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = stringResource(R.string.negotiations),
                            subtitle = stringResource(R.string.active_deals),
                            color = Color(0xFF2196F3),
                            onClick = onNavigateToNegotiations,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Outlined.Description,
                            title = stringResource(R.string.contracts),
                            subtitle = stringResource(R.string.your_agreements),
                            color = Color(0xFFFF9800),
                            onClick = onNavigateToContracts,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Outlined.Warning,
                            title = stringResource(R.string.disputes),
                            subtitle = stringResource(R.string.issues),
                            color = Color(0xFFF44336),
                            onClick = onNavigateToDisputes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Recent Listings Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userRole == "farmer") stringResource(R.string.my_listings) else stringResource(R.string.recent_listings),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToListings) {
                            Text(stringResource(R.string.see_all), color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
            
            item {
                if (listingsState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                } else {
                    val listings = if (userRole == "farmer") listingsState.myListings else listingsState.listings
                    if (listings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Grass,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (userRole == "farmer") "No listings yet" else "No listings available",
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = listings.take(5),
                                key = { it.id }
                            ) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onNavigateToListingDetail(listing.id) }
                                )
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Crop Background Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Image(
                    painter = painterResource(id = com.farmconnect.app.utils.CropImageMapper.getCropImage(listing.cropType)),
                    contentDescription = listing.cropType,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                    fontSize = 40.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = listing.cropType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                
                if (listing.variety != null) {
                    Text(
                        text = listing.variety,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${listing.quantity} ${listing.unit}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "₹${listing.minPrice} - ₹${listing.maxPrice}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status chip
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when(listing.status) {
                        "active" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "draft" -> Color.Gray.copy(alpha = 0.1f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = listing.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = when(listing.status) {
                            "active" -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}
