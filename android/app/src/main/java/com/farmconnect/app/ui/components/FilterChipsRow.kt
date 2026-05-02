package com.farmconnect.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterChipsRow(
    searchQuery: String,
    variety: String?,
    qualityGrade: String?,
    minPrice: Double?,
    maxPrice: Double?,
    minQuantity: Double?,
    maxQuantity: Double?,
    city: String?,
    state: String?,
    sortLabel: String?,
    onClearSearch: () -> Unit,
    onClearVariety: () -> Unit,
    onClearQualityGrade: () -> Unit,
    onClearPriceRange: () -> Unit,
    onClearQuantityRange: () -> Unit,
    onClearCity: () -> Unit,
    onClearState: () -> Unit,
    onClearSort: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = searchQuery.isNotEmpty() || variety != null || 
                          qualityGrade != null || minPrice != null || maxPrice != null ||
                          minQuantity != null || maxQuantity != null || 
                          city != null || state != null || sortLabel != null

    if (hasActiveFilters) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchQuery.isNotEmpty()) {
                FilterChip(label = "\"$searchQuery\"", onRemove = onClearSearch)
            }
            
            if (variety != null) {
                FilterChip(label = "Variety: $variety", onRemove = onClearVariety)
            }
            
            if (qualityGrade != null) {
                FilterChip(label = "Grade: $qualityGrade", onRemove = onClearQualityGrade)
            }
            
            if (minPrice != null || maxPrice != null) {
                val label = when {
                    minPrice != null && maxPrice != null -> "₹$minPrice - ₹$maxPrice"
                    minPrice != null -> "₹$minPrice+"
                    else -> "Up to ₹$maxPrice"
                }
                FilterChip(label = label, onRemove = onClearPriceRange)
            }
            
            if (minQuantity != null || maxQuantity != null) {
                val label = when {
                    minQuantity != null && maxQuantity != null -> "$minQuantity - $maxQuantity"
                    minQuantity != null -> "${minQuantity}+"
                    else -> "Up to $maxQuantity"
                }
                FilterChip(label = "Qty: $label", onRemove = onClearQuantityRange)
            }
            
            if (city != null) {
                FilterChip(label = "City: $city", onRemove = onClearCity)
            }
            
            if (state != null) {
                FilterChip(label = "State: $state", onRemove = onClearState)
            }
            
            if (sortLabel != null) {
                FilterChip(label = sortLabel, onRemove = onClearSort)
            }
            
            // Clear all button
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFD32F2F)
                )
            ) {
                Text("Clear All", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF2E7D32)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2E7D32)
                )
            }
        }
    }
}
