package com.farmconnect.app.ui.screens.disputes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.farmconnect.app.ui.viewmodels.DisputeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseDisputeScreen(
    contractId: Int,
    onNavigateBack: () -> Unit,
    onDisputeRaised: () -> Unit,
    viewModel: DisputeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }
    
    val disputeReasons = listOf(
        "Quality Issue" to "Crop quality doesn't match agreed grade",
        "Quantity Mismatch" to "Delivered quantity differs from contract",
        "Late Delivery" to "Delivery was delayed beyond agreed date",
        "Payment Issue" to "Payment not received or incomplete",
        "Contract Breach" to "Other party violated contract terms",
        "Other" to "Other issue not listed above"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raise Dispute", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF44336),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Before raising a dispute", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Try to resolve the issue directly with the other party first. Disputes should be a last resort.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Contract Reference
            Text("Contract ID: #$contractId", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reason Dropdown
            Text("Reason for Dispute *", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedReason,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Select a reason") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    disputeReasons.forEach { (reason, hint) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(reason, fontWeight = FontWeight.Medium)
                                    Text(hint, fontSize = 12.sp, color = Color.Gray)
                                }
                            },
                            onClick = {
                                selectedReason = reason
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Description
            Text("Describe the issue *", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Provide details about the issue. Include dates, quantities, or any relevant information...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )
            
            // Error display
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.error ?: "", color = Color.Red, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Submit Button
            Button(
                onClick = {
                    viewModel.raiseDispute(contractId, selectedReason, description) {
                        onDisputeRaised()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                enabled = selectedReason.isNotBlank() && description.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Dispute", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
