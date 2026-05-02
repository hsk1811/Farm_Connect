package com.farmconnect.app.ui.screens.negotiations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farmconnect.app.data.models.NegotiationMessage
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.ui.viewmodels.NegotiationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationChatScreen(
    negotiationId: Int,
    onNavigateBack: () -> Unit,
    onContractCreated: (Int) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: NegotiationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val negotiation = uiState.selectedNegotiation
    val currentUserId by authViewModel.currentUserId.collectAsState(initial = null)
    
    var messageText by remember { mutableStateOf("") }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var finalPrice by remember { mutableStateOf("") }
    var finalQuantity by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(negotiation?.messages?.size) {
        negotiation?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }
    
    // Optimized polling - 5 seconds
    LaunchedEffect(negotiationId) {
        viewModel.loadNegotiationDetail(negotiationId)
        while (true) {
            kotlinx.coroutines.delay(5000)
            viewModel.loadNegotiationDetail(negotiationId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(negotiation?.cropType ?: "Negotiation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (negotiation?.status != null) {
                            Text(
                                negotiation.status.uppercase(),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (negotiation?.status == "open") {
                        IconButton(onClick = { 
                            finalPrice = negotiation.proposedPrice.toString()
                            finalQuantity = negotiation.proposedQuantity.toString()
                            showAcceptDialog = true 
                        }) {
                            Icon(Icons.Default.Handshake, contentDescription = "Accept")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading && negotiation == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && negotiation == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error ?: "Failed to load chat", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadNegotiationDetail(negotiationId) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Negotiation Info Summary
                    if (negotiation != null) {
                        Surface(
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Current Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${negotiation.proposedPrice}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Quantity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${negotiation.proposedQuantity}", fontWeight = FontWeight.Bold)
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Val", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format("%.2f", negotiation.proposedPrice * negotiation.proposedQuantity)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                    
                    // Messages
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(negotiation?.messages ?: emptyList()) { message ->
                            val isMe = message.senderId == currentUserId
                            MessageBubble(message = message, isMe = isMe)
                        }
                    }
                    
                    // Input Bar
                    if (negotiation?.status == "open" || negotiation?.status == "accepted") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Type a message...") },
                                    maxLines = 4,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendMessage(negotiationId, "text", messageText, null, null)
                                            messageText = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Accept Dialog (Keep existing logic but improve styling slightly)
    if (showAcceptDialog) {
        var qualityGrade by remember { mutableStateOf("") }
        var paymentTerms by remember { mutableStateOf("") }
        var transportResponsibility by remember { mutableStateOf("buyer") }
        var additionalTerms by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showAcceptDialog = false },
            title = { Text("Finalize Contract Terms", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Confirm key details before creating the contract:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = finalPrice,
                        onValueChange = { finalPrice = it },
                        label = { Text("Final Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = finalQuantity,
                        onValueChange = { finalQuantity = it },
                        label = { Text("Final Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Quality & Logistics", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = qualityGrade,
                        onValueChange = { qualityGrade = it },
                        label = { Text("Quality Grade (e.g. Grade A)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = paymentTerms,
                        onValueChange = { paymentTerms = it },
                        label = { Text("Payment Terms (e.g. Net 30)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Transport Responsibility", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = transportResponsibility == "buyer",
                                onClick = { transportResponsibility = "buyer" },
                                enabled = !uiState.isLoading
                            )
                            Text("Buyer Pick-up")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = transportResponsibility == "farmer",
                                onClick = { transportResponsibility = "farmer" },
                                enabled = !uiState.isLoading
                            )
                            Text("Farmer Delivery")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = additionalTerms,
                        onValueChange = { additionalTerms = it },
                        label = { Text("Additional Terms (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        enabled = !uiState.isLoading
                    )
                    
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acceptNegotiation(
                            negotiationId,
                            finalPrice.toDoubleOrNull() ?: negotiation?.proposedPrice ?: 0.0,
                            finalQuantity.toDoubleOrNull() ?: negotiation?.proposedQuantity ?: 0.0,
                            qualityGrade.ifBlank { null },
                            paymentTerms.ifBlank { null },
                            transportResponsibility,
                            additionalTerms.ifBlank { null }
                        ) { contractId ->
                            showAcceptDialog = false
                            onContractCreated(contractId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating...")
                    } else {
                        Text("Create Contract")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = false }, enabled = !uiState.isLoading) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(message: NegotiationMessage, isMe: Boolean) {
    val isProposal = message.messageType == "proposal" || message.messageType == "counter_proposal"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                message.senderName ?: "User",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }
        
        Card(
            shape = if (isMe) 
                RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            else 
                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isMe -> MaterialTheme.colorScheme.primary
                    isProposal -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when {
                    isMe -> MaterialTheme.colorScheme.onPrimary
                    isProposal -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            modifier = Modifier
                .padding(horizontal = if (isMe) 0.dp else 4.dp)
                .widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isProposal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (message.messageType == "proposal") "Initial Proposal" else "Counter Offer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Price: ₹${message.proposedPrice}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Qty: ${message.proposedQuantity}",
                        fontSize = 13.sp,
                        alpha = 0.8f
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                }
                
                if (!message.message.isNullOrBlank()) {
                    Text(
                        message.message,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Text(
            message.createdAt.substringAfter("T").substringBeforeLast(":"),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}
