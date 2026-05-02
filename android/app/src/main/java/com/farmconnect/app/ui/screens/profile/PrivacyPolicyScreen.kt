package com.farmconnect.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            // Last Updated
            Text(
                text = "Last Updated: January 2026",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Introduction
            SectionTitle("Introduction")
            SectionText(
                "Welcome to FarmConnect. We respect your privacy and are committed to protecting your personal data. " +
                "This privacy policy will inform you about how we handle your personal data when you use our mobile application."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Information We Collect
            SectionTitle("Information We Collect")
            SectionText("We collect the following types of information:")
            BulletPoint("Personal Information: Name, email address, phone number")
            BulletPoint("Profile Information: Business details, location, farm information")
            BulletPoint("Listing Information: Crop details, prices, quantities, and photos")
            BulletPoint("Transaction Data: Negotiations, contracts, and payment information")
            BulletPoint("Usage Data: How you interact with our application")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // How We Use Your Information
            SectionTitle("How We Use Your Information")
            SectionText("We use your information to:")
            BulletPoint("Provide and maintain our service")
            BulletPoint("Connect farmers with buyers")
            BulletPoint("Process transactions and contracts")
            BulletPoint("Send notifications about your activities")
            BulletPoint("Improve our application and services")
            BulletPoint("Ensure security and prevent fraud")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Sharing
            SectionTitle("Data Sharing")
            SectionText(
                "We do not sell your personal data. We only share your information with other users as necessary " +
                "to facilitate transactions (e.g., sharing farmer contact details with buyers during negotiations)."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Security
            SectionTitle("Data Security")
            SectionText(
                "We implement appropriate technical and organizational measures to protect your personal data against " +
                "unauthorized access, alteration, disclosure, or destruction. However, no method of transmission over " +
                "the internet is 100% secure."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Your Rights
            SectionTitle("Your Rights")
            SectionText("You have the right to:")
            BulletPoint("Access your personal data")
            BulletPoint("Correct inaccurate or incomplete data")
            BulletPoint("Request deletion of your data")
            BulletPoint("Object to processing of your data")
            BulletPoint("Withdraw consent at any time")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Retention
            SectionTitle("Data Retention")
            SectionText(
                "We retain your personal data for as long as necessary to provide our services and comply with legal " +
                "obligations. When you delete your account, we will delete or anonymize your personal data, except " +
                "where we are required to retain it for legal purposes."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Children's Privacy
            SectionTitle("Children's Privacy")
            SectionText(
                "Our service is not intended for users under the age of 18. We do not knowingly collect personal " +
                "information from children under 18."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Changes to Privacy Policy
            SectionTitle("Changes to This Privacy Policy")
            SectionText(
                "We may update our privacy policy from time to time. We will notify you of any changes by posting " +
                "the new privacy policy on this page and updating the \"Last Updated\" date."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact Information
            SectionTitle("Contact Us")
            SectionText(
                "If you have any questions about this privacy policy or our data practices, please contact us at:\n\n" +
                "Email: haraat@gmail.com\n" +
                "Phone: +91 9321991492"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2E7D32),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color.DarkGray,
        lineHeight = 20.sp
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
        Text(
            text = "• ",
            fontSize = 14.sp,
            color = Color.DarkGray
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.DarkGray,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
