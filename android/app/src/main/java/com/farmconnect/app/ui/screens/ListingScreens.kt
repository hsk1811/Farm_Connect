package com.farmconnect.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.CreateListingRequest
import com.farmconnect.app.ui.components.*
import com.farmconnect.app.viewmodel.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onListingClick: (Int) -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf("All", "Active", "Draft", "Paused", "Closed")
    
    LaunchedEffect(selectedTab) {
        val status = when (selectedTab) {
            1 -> "active"
            2 -> "draft"
            3 -> "paused"
            4 -> "closed"
            else -> null
        }
        viewModel.loadMyListings(status)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Listing")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.listings.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Inventory,
                    title = "No Listings",
                    message = "Create your first listing to start selling"
                ) {
                    Button(onClick = onNavigateToCreate) {
                        Text("Create Listing")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.listings) { listing ->
                        ListingCard(
                            cropType = listing.cropType,
                            variety = listing.variety,
                            quantity = listing.quantity,
                            unit = listing.unit,
                            minPrice = listing.minPrice,
                            maxPrice = listing.maxPrice,
                            farmerName = null,
                            status = listing.status,
                            onClick = { onListingClick(listing.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var cropType by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("quintal") }
    var qualityGrade by remember { mutableStateOf("A") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var harvestStartDate by remember { mutableStateOf("") }
    var harvestEndDate by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val units = listOf("kg", "ton", "quintal")
    val grades = listOf("A", "B", "C", "Premium")
    
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            viewModel.resetActionSuccess()
            onSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crop Type
            OutlinedTextField(
                value = cropType,
                onValueChange = { cropType = it },
                label = { Text("Crop Type *") },
                placeholder = { Text("e.g., Wheat, Rice, Cotton") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Variety
            OutlinedTextField(
                value = variety,
                onValueChange = { variety = it },
                label = { Text("Variety") },
                placeholder = { Text("e.g., HD-2967") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Quantity and Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = { unit = u; unitExpanded = false }
                            )
                        }
                    }
                }
            }
            
            // Quality Grade
            var gradeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = gradeExpanded,
                onExpandedChange = { gradeExpanded = it }
            ) {
                OutlinedTextField(
                    value = qualityGrade,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Quality Grade") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = gradeExpanded,
                    onDismissRequest = { gradeExpanded = false }
                ) {
                    grades.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g) },
                            onClick = { qualityGrade = g; gradeExpanded = false }
                        )
                    }
                }
            }
            
            // Price Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    label = { Text("Min Price (₹) *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    label = { Text("Max Price (₹) *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            
            // Harvest Dates
            OutlinedTextField(
                value = harvestStartDate,
                onValueChange = { harvestStartDate = it },
                label = { Text("Harvest Start Date *") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = harvestEndDate,
                onValueChange = { harvestEndDate = it },
                label = { Text("Harvest End Date *") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Location
            OutlinedTextField(
                value = locationAddress,
                onValueChange = { locationAddress = it },
                label = { Text("Location Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // Error Message
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Submit Button
            val isValid = cropType.isNotBlank() && quantity.isNotBlank() && 
                         minPrice.isNotBlank() && maxPrice.isNotBlank() &&
                         harvestStartDate.isNotBlank() && harvestEndDate.isNotBlank()
            
            PrimaryButton(
                text = "Create Listing",
                onClick = {
                    viewModel.createListing(
                        CreateListingRequest(
                            cropType = cropType,
                            variety = variety.ifBlank { null },
                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                            unit = unit,
                            qualityGrade = qualityGrade,
                            minPrice = minPrice.toDoubleOrNull() ?: 0.0,
                            maxPrice = maxPrice.toDoubleOrNull() ?: 0.0,
                            harvestStartDate = harvestStartDate,
                            harvestEndDate = harvestEndDate,
                            locationAddress = locationAddress.ifBlank { null },
                            description = description.ifBlank { null }
                        )
                    )
                },
                enabled = isValid,
                isLoading = uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseListingsScreen(
    onNavigateBack: () -> Unit,
    onListingClick: (Int) -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Listings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search crops...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.listings.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Listings Found",
                    message = "Check back later for new listings"
                )
            } else {
                val filteredListings = if (searchQuery.isBlank()) {
                    uiState.listings
                } else {
                    uiState.listings.filter { 
                        it.cropType.contains(searchQuery, ignoreCase = true) ||
                        (it.variety?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }
                
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredListings) { listing ->
                        ListingCard(
                            cropType = listing.cropType,
                            variety = listing.variety,
                            quantity = listing.quantity,
                            unit = listing.unit,
                            minPrice = listing.minPrice,
                            maxPrice = listing.maxPrice,
                            farmerName = listing.farmerName,
                            status = listing.status,
                            onClick = { onListingClick(listing.id) }
                        )
                    }
                }
            }
        }
    }
}
