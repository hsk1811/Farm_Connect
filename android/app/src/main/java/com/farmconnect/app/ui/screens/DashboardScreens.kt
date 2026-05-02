package com.farmconnect.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.ui.components.*
import com.farmconnect.app.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerDashboardScreen(
    onNavigateToListings: () -> Unit,
    onNavigateToNegotiations: () -> Unit,
    onNavigateToContracts: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    contractViewModel: ContractViewModel = hiltViewModel(),
    negotiationViewModel: NegotiationViewModel = hiltViewModel()
) {
    val listingState by listingViewModel.uiState.collectAsState()
    val contractState by contractViewModel.uiState.collectAsState()
    val negotiationState by negotiationViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        listingViewModel.loadMyListings()
        contractViewModel.loadContracts()
        negotiationViewModel.loadNegotiations("open")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("FarmConnect", fontWeight = FontWeight.Bold)
                        Text(
                            "Farmer Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateListing,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Listing") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Active Listings",
                        value = listingState.listings.count { it.status == "active" }.toString(),
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Open Negotiations",
                        value = negotiationState.negotiations.size.toString(),
                        icon = Icons.Default.Chat,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Contracts",
                        value = contractState.contracts.size.toString(),
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "My Listings",
                        icon = Icons.Default.Inventory,
                        onClick = onNavigateToListings,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Negotiations",
                        icon = Icons.Default.Chat,
                        onClick = onNavigateToNegotiations,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Contracts",
                        icon = Icons.Default.Description,
                        onClick = onNavigateToContracts,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Negotiations
            if (negotiationState.negotiations.isNotEmpty()) {
                item {
                    SectionHeader(title = "Pending Negotiations", onViewAll = onNavigateToNegotiations)
                }

                items(negotiationState.negotiations.take(3)) { negotiation ->
                    NegotiationCard(
                        cropType = negotiation.cropType ?: "Unknown",
                        proposedPrice = negotiation.proposedPrice,
                        proposedQuantity = negotiation.proposedQuantity,
                        counterpartyName = negotiation.buyerName,
                        status = negotiation.status,
                        onClick = { }
                    )
                }
            }

            // Active Contracts
            if (contractState.contracts.isNotEmpty()) {
                item {
                    SectionHeader(title = "Active Contracts", onViewAll = onNavigateToContracts)
                }

                items(contractState.contracts.filter { it.status in listOf("active", "in_progress") }.take(3)) { contract ->
                    ContractCard(
                        contractNumber = contract.contractNumber,
                        cropType = contract.cropType,
                        quantity = contract.quantity,
                        unit = contract.unit,
                        totalValue = contract.totalValue,
                        counterpartyName = contract.buyerName,
                        status = contract.status,
                        onClick = { }
                    )
                }
            }

            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerDashboardScreen(
    onNavigateToListings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToNegotiations: () -> Unit,
    onNavigateToContracts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    contractViewModel: ContractViewModel = hiltViewModel(),
    negotiationViewModel: NegotiationViewModel = hiltViewModel()
) {
    val listingState by listingViewModel.uiState.collectAsState()
    val contractState by contractViewModel.uiState.collectAsState()
    val negotiationState by negotiationViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        listingViewModel.loadListings()
        contractViewModel.loadContracts()
        negotiationViewModel.loadNegotiations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("FarmConnect", fontWeight = FontWeight.Bold)
                        Text(
                            "Buyer Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Negotiations",
                        value = negotiationState.negotiations.size.toString(),
                        icon = Icons.Default.Chat,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Contracts",
                        value = contractState.contracts.size.toString(),
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Browse",
                        icon = Icons.Default.Search,
                        onClick = onNavigateToListings,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Negotiations",
                        icon = Icons.Default.Chat,
                        onClick = onNavigateToNegotiations,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Contracts",
                        icon = Icons.Default.Description,
                        onClick = onNavigateToContracts,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Available Listings
            item {
                SectionHeader(title = "Available Listings", onViewAll = onNavigateToListings)
            }

            if (listingState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (listingState.listings.isEmpty()) {
                item {
                    Text(
                        text = "No listings available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(listingState.listings.take(5)) { listing ->
                    ListingCard(
                        cropType = listing.cropType,
                        variety = listing.variety,
                        quantity = listing.quantity,
                        unit = listing.unit,
                        minPrice = listing.minPrice,
                        maxPrice = listing.maxPrice,
                        farmerName = listing.farmerName,
                        status = listing.status,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onViewAll) {
            Text("View All")
        }
    }
}
