package com.farmconnect.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmconnect.app.data.local.SearchFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: SearchFilters,
    onApplyFilters: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var variety by remember { mutableStateOf(currentFilters.variety ?: "") }
    var minPrice by remember { mutableStateOf(currentFilters.minPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(currentFilters.maxPrice?.toString() ?: "") }
    var minQuantity by remember { mutableStateOf(currentFilters.minQuantity?.toString() ?: "") }
    var maxQuantity by remember { mutableStateOf(currentFilters.maxQuantity?.toString() ?: "") }
    var selectedQualityGrade by remember { mutableStateOf(currentFilters.qualityGrade) }
    var city by remember { mutableStateOf(currentFilters.city ?: "") }
    var state by remember { mutableStateOf(currentFilters.state ?: "") }
    var sortBy by remember { mutableStateOf(currentFilters.sortBy ?: "date") }
    var sortOrder by remember { mutableStateOf(currentFilters.sortOrder ?: "desc") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters & Sorting",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Divider()

            // Variety
            Text("Variety", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = variety,
                onValueChange = { variety = it },
                placeholder = { Text("e.g., Basmati") },
                modifier = Modifier.fillMaxWidth()
            )

            // Quality Grade
            Text("Quality Grade", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("A", "B", "C", "Premium").forEach { grade ->
                    FilterChip(
                        selected = selectedQualityGrade == grade,
                        onClick = {
                            selectedQualityGrade = if (selectedQualityGrade == grade) null else grade
                        },
                        label = { Text(grade) }
                    )
                }
            }

            // Price Range
            Text("Price Range (per unit)", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it.filter { char -> char.isDigit() || char == '.' } },
                    placeholder = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    prefix = { Text("₹") }
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it.filter { char -> char.isDigit() || char == '.' } },
                    placeholder = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    prefix = { Text("₹") }
                )
            }

            // Quantity Range
            Text("Quantity Range", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minQuantity,
                    onValueChange = { minQuantity = it.filter { char -> char.isDigit() || char == '.' } },
                    placeholder = { Text("Min") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxQuantity,
                    onValueChange = { maxQuantity = it.filter { char -> char.isDigit() || char == '.' } },
                    placeholder = { Text("Max") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Location
            Text("Location", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                placeholder = { Text("City") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                placeholder = { Text("State") },
                modifier = Modifier.fillMaxWidth()
            )

            // Sort Options
            Text("Sort By", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption(
                    label = "Date (Newest First)",
                    isSelected = sortBy == "date" && sortOrder == "desc",
                    onClick = { sortBy = "date"; sortOrder = "desc" }
                )
                SortOption(
                    label = "Date (Oldest First)",
                    isSelected = sortBy == "date" && sortOrder == "asc",
                    onClick = { sortBy = "date"; sortOrder = "asc" }
                )
                SortOption(
                    label = "Price (Low to High)",
                    isSelected = sortBy == "price" && sortOrder == "asc",
                    onClick = { sortBy = "price"; sortOrder = "asc" }
                )
                SortOption(
                    label = "Price (High to Low)",
                    isSelected = sortBy == "price" && sortOrder == "desc",
                    onClick = { sortBy = "price"; sortOrder = "desc" }
                )
                SortOption(
                    label = "Quantity (Low to High)",
                    isSelected = sortBy == "quantity" && sortOrder == "asc",
                    onClick = { sortBy = "quantity"; sortOrder = "asc" }
                )
                SortOption(
                    label = "Quantity (High to Low)",
                    isSelected = sortBy == "quantity" && sortOrder == "desc",
                    onClick = { sortBy = "quantity"; sortOrder = "desc" }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        variety = ""
                        minPrice = ""
                        maxPrice = ""
                        minQuantity = ""
                        maxQuantity = ""
                        selectedQualityGrade = null
                        city = ""
                        state = ""
                        sortBy = "date"
                        sortOrder = "desc"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        val filters = SearchFilters(
                            variety = variety.ifEmpty { null },
                            minPrice = minPrice.toDoubleOrNull(),
                            maxPrice = maxPrice.toDoubleOrNull(),
                            minQuantity = minQuantity.toDoubleOrNull(),
                            maxQuantity = maxQuantity.toDoubleOrNull(),
                            qualityGrade = selectedQualityGrade,
                            city = city.ifEmpty { null },
                            state = state.ifEmpty { null },
                            sortBy = sortBy,
                            sortOrder = sortOrder
                        )
                        onApplyFilters(filters)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Apply Filters")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp)
        }
    }
}
