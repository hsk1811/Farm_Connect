package com.farmconnect.app.ui.screens.contracts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.ui.viewmodels.ContractViewModel
import com.farmconnect.app.ui.utils.PreventScreenCapture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    contractId: Int,
    onNavigateBack: () -> Unit,
    onRaiseDispute: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ContractViewModel = hiltViewModel()
) {
    val userRole by authViewModel.userRole.collectAsState(initial = "buyer")
    val uiState by viewModel.uiState.collectAsState()
    val contract = uiState.selectedContract
    
    LaunchedEffect(contractId) {
        viewModel.loadContractDetail(contractId)
    }
    
    // Prevent screenshots/screen recordings for confirmed contracts (security feature)
    val isContractConfirmed = contract?.status in listOf("active", "in_progress", "completed")
    if (isContractConfirmed) {
        PreventScreenCapture()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Contract Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (contract != null) {
                            Text(contract.contractNumber, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
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
        if (uiState.isLoading || contract == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF9800))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Status Banner
                val statusColor = when(contract.status) {
                    "pending" -> Color(0xFFFF9800)
                    "active" -> Color(0xFF4CAF50)
                    "in_progress" -> Color(0xFF2196F3)
                    "completed" -> Color(0xFF9C27B0)
                    else -> Color.Gray
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Status", fontSize = 12.sp, color = Color.Gray)
                            Text(contract.status.uppercase().replace("_", " "), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = statusColor)
                        }
                        Icon(Icons.Default.Description, contentDescription = null, tint = statusColor, modifier = Modifier.size(40.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Security Info Banner (for confirmed contracts)
                if (isContractConfirmed) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security, 
                                contentDescription = null, 
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "This contract is protected. Screenshots and screen recordings are disabled.",
                                fontSize = 11.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Crop Details
                Text("Crop Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Crop Type", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.cropType, fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Variety", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.variety ?: "N/A", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quantity", fontSize = 12.sp, color = Color.Gray)
                                Text("${contract.quantity} ${contract.unit}", fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Agreed Price", fontSize = 12.sp, color = Color.Gray)
                                Text("₹${contract.agreedPrice}/${contract.unit}", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Total Value
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Contract Value", fontWeight = FontWeight.Medium)
                        Text("₹${contract.totalValue}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFFF9800))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Terms & Conditions
                if (contract.qualityGrade != null || contract.paymentTerms != null || contract.transportResponsibility != null) {
                    Text("Terms & Conditions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (contract.qualityGrade != null) {
                                Text("Quality Grade", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.qualityGrade, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            if (contract.paymentTerms != null) {
                                Text("Payment Terms", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.paymentTerms, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            if (contract.transportResponsibility != null) {
                                Text("Transport Responsibility", fontSize = 12.sp, color = Color.Gray)
                                val responsibility = if (contract.transportResponsibility == "buyer") "Buyer Pick-up" else "Farmer Delivery"
                                Text(responsibility, fontWeight = FontWeight.SemiBold)
                            }
                            
                            if (contract.additionalTerms != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Additional Terms", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.additionalTerms, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Parties
                Text("Contract Parties", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Agriculture, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Farmer", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.farmerName ?: "Farmer", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (contract.farmerConfirmed) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF2196F3))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Buyer", fontSize = 12.sp, color = Color.Gray)
                                Text(contract.buyerName ?: "Buyer", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (contract.buyerConfirmed) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
                
                // Delivery
                if (contract.deliveryAddress != null || contract.deliveryDate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Delivery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (contract.deliveryAddress != null) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Address", fontSize = 12.sp, color = Color.Gray)
                                        Text(contract.deliveryAddress)
                                    }
                                }
                            }
                            if (contract.deliveryDate != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Delivery Date", fontSize = 12.sp, color = Color.Gray)
                                        Text(contract.deliveryDate)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Actions
                if (contract.status == "pending") {
                    Button(
                        onClick = { viewModel.confirmContract(contractId) {} },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Contract", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                if (contract.status == "active" || contract.status == "in_progress") {
                    OutlinedButton(
                        onClick = onRaiseDispute,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Raise Dispute", fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
