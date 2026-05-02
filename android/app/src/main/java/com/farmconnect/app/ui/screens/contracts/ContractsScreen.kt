package com.farmconnect.app.ui.screens.contracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.Contract
import com.farmconnect.app.ui.viewmodels.ContractViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ContractViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadContracts()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Contracts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.contracts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF9800))
            }
        } else if (uiState.contracts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No contracts yet", fontSize = 18.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.contracts,
                    key = { it.id }
                ) { contract ->
                    ContractItem(contract = contract, onClick = { onNavigateToDetail(contract.id) })
                }
            }
        }
    }
}

@Composable
fun ContractItem(contract: Contract, onClick: () -> Unit) {
    val statusColor = when(contract.status) {
        "pending" -> Color(0xFFFF9800)
        "active" -> Color(0xFF4CAF50)
        "in_progress" -> Color(0xFF2196F3)
        "completed" -> Color(0xFF9C27B0)
        "cancelled" -> Color(0xFF757575)
        "disputed" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(contract.contractNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text(contract.cropType, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (contract.variety != null) {
                        Text(contract.variety, fontSize = 13.sp, color = Color.Gray)
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        contract.status.uppercase().replace("_", " "),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quantity", fontSize = 11.sp, color = Color.Gray)
                    Text("${contract.quantity} ${contract.unit}", fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Agreed Price", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${contract.agreedPrice}/${contract.unit}", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Value", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${contract.totalValue}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
            
            // Confirmation Status
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (contract.farmerConfirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null, modifier = Modifier.size(16.dp),
                        tint = if (contract.farmerConfirmed) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Farmer", fontSize = 12.sp, color = if (contract.farmerConfirmed) Color(0xFF4CAF50) else Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (contract.buyerConfirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null, modifier = Modifier.size(16.dp),
                        tint = if (contract.buyerConfirmed) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buyer", fontSize = 12.sp, color = if (contract.buyerConfirmed) Color(0xFF4CAF50) else Color.Gray)
                }
            }
        }
    }
}
