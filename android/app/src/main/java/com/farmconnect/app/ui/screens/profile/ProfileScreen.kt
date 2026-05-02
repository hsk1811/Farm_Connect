package com.farmconnect.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Context
import com.farmconnect.app.R
import com.farmconnect.app.data.local.LanguagePreferences
import com.farmconnect.app.data.local.ThemePreferences
import com.farmconnect.app.ui.components.LanguageSelectionDialog
import com.farmconnect.app.ui.components.ThemeSelectionDialog
import com.farmconnect.app.ui.viewmodels.AuthViewModel
import com.farmconnect.app.utils.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Get context
    val context = LocalContext.current
    
    val userName by authViewModel.userName.collectAsState(initial = "User")
    val userRole by authViewModel.userRole.collectAsState(initial = "farmer")
    
    // Get current language from SharedPreferences directly
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var currentLanguage by remember { mutableStateOf(prefs.getString("language", "system") ?: "system") }
    
    // Get theme preferences
    val themePreferences = remember { ThemePreferences(context) }
    val currentTheme by themePreferences.theme.collectAsState(initial = ThemePreferences.THEME_SYSTEM)
    
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    val gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
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
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(gradientColors))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userName?.firstOrNull() ?: 'U').uppercase(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        userName ?: "User",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (userRole == "farmer") Icons.Default.Agriculture else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                userRole?.replaceFirstChar { it.uppercase() } ?: "User",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Menu Items
            val context = androidx.compose.ui.platform.LocalContext.current
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.farmconnect.app.R.string.profile), 
                    fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                ProfileMenuItem(Icons.Default.Person, stringResource(R.string.edit_profile), stringResource(R.string.update_information)) {
                   onNavigateToEditProfile()
                }
                ProfileMenuItem(Icons.Default.Lock, stringResource(R.string.change_password), stringResource(R.string.update_password)) {
                    onNavigateToChangePassword()
                }
                ProfileMenuItem(Icons.Default.Palette, stringResource(R.string.theme), stringResource(R.string.change_appearance)) {
                    showThemeDialog = true
                }
                ProfileMenuItem(Icons.Default.Notifications, stringResource(R.string.notifications), stringResource(R.string.manage_notifications)) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(stringResource(R.string.preferences), fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                val languageText = when(currentLanguage) {
                    "en" -> "English"
                    "hi" -> "हिंदी"
                    "mr" -> "मराठी"
                    else -> "System Default"
                }
                ProfileMenuItem(Icons.Default.Language, stringResource(R.string.language), languageText) {
                    showLanguageDialog = true
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(stringResource(R.string.support), fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                ProfileMenuItem(Icons.Default.Help, stringResource(R.string.help_center), stringResource(R.string.get_help)) {
                    android.widget.Toast.makeText(context, "Contact support@farmconnect.com", android.widget.Toast.LENGTH_SHORT).show()
                }
                ProfileMenuItem(Icons.Default.Info, stringResource(R.string.about), stringResource(R.string.app_version)) {
                    android.widget.Toast.makeText(context, "FarmConnect v1.0.0", android.widget.Toast.LENGTH_SHORT).show()
                }
                ProfileMenuItem(Icons.Default.Policy, stringResource(R.string.privacy_policy), stringResource(R.string.read_policy)) {
                    onNavigateToPrivacyPolicy()
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Logout Button
                OutlinedButton(
                    onClick = { authViewModel.logout(onLogout) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.log_out), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { selectedLanguage ->
                    // Save language preference directly to SharedPreferences
                    prefs.edit().putString("language", selectedLanguage).commit()
                    currentLanguage = selectedLanguage
                    
                    // Recreate activity to apply new locale (preserves authentication)
                    (context as? android.app.Activity)?.recreate()
                }
            )
        }
        
        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentTheme,
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { selectedTheme ->
                    scope.launch {
                        themePreferences.setTheme(selectedTheme)
                        // Recreate activity to apply new theme
                        (context as? android.app.Activity)?.recreate()
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
