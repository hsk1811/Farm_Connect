package com.farmconnect.app.ui.screens.disputes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.Dispute
import com.farmconnect.app.ui.viewmodels.DisputeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputesScreen(
    onNavigateBack: () -> Unit,
    viewModel: DisputeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDisputes()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disputes", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading && uiState.disputes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF44336))
            }
        } else if (uiState.disputes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No disputes", fontSize = 18.sp, color = Color.Gray)
                    Text("That's a good thing! 🎉", fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.disputes) { dispute ->
                    DisputeItem(dispute = dispute)
                }
            }
        }
    }
}

@Composable
fun DisputeItem(dispute: Dispute) {
    val statusColor = when(dispute.status) {
        "open" -> Color(0xFFF44336)
        "under_review" -> Color(0xFFFF9800)
        "resolved" -> Color(0xFF4CAF50)
        "closed" -> Color.Gray
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(dispute.reason, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Contract: ${dispute.contractNumber ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        dispute.status.uppercase().replace("_", " "),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(dispute.description, fontSize = 13.sp, color = Color.Gray, maxLines = 2)
            
            if (dispute.resolutionNotes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resolution: ${dispute.resolutionNotes}", fontSize = 12.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}
