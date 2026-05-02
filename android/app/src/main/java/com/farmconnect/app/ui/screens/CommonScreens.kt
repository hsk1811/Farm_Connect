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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.ui.components.*
import com.farmconnect.app.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationsListScreen(
    onNavigateBack: () -> Unit,
    onNegotiationClick: (Int) -> Unit,
    viewModel: NegotiationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Open", "Accepted", "Rejected")
    
    LaunchedEffect(selectedTab) {
        val status = when (selectedTab) {
            1 -> "open"
            2 -> "accepted"
            3 -> "rejected"
            else -> null
        }
        viewModel.loadNegotiations(status)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Negotiations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.negotiations.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Chat,
                    title = "No Negotiations",
                    message = "Your negotiations will appear here"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.negotiations) { negotiation ->
                        NegotiationCard(
                            cropType = negotiation.cropType ?: "Negotiation",
                            proposedPrice = negotiation.proposedPrice,
                            proposedQuantity = negotiation.proposedQuantity,
                            counterpartyName = negotiation.farmerName ?: negotiation.buyerName,
                            status = negotiation.status,
                            onClick = { onNegotiationClick(negotiation.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsListScreen(
    onNavigateBack: () -> Unit,
    onContractClick: (Int) -> Unit,
    viewModel: ContractViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Pending", "Active", "Completed")
    
    LaunchedEffect(selectedTab) {
        val status = when (selectedTab) {
            1 -> "pending"
            2 -> "active"
            3 -> "completed"
            else -> null
        }
        viewModel.loadContracts(status)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contracts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.contracts.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Description,
                    title = "No Contracts",
                    message = "Your contracts will appear here"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.contracts) { contract ->
                        ContractCard(
                            contractNumber = contract.contractNumber,
                            cropType = contract.cropType,
                            quantity = contract.quantity,
                            unit = contract.unit,
                            totalValue = contract.totalValue,
                            counterpartyName = contract.farmerName ?: contract.buyerName,
                            status = contract.status,
                            onClick = { onContractClick(contract.id) }
                        )
                    }
                }
            }
        }
    }
}
