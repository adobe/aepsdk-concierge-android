package com.adobe.marketing.mobile.conciergetestapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.components.messages.ConciergeResponse

/**
 * Demo screen showcasing the markdown parser functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownDemoScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val adobeSampleMarkdown = "To edit photos, Adobe has some fantastic tools:\n\n1. **Photoshop** – Perfect for advanced editing, retouching, and creating stunning effects.  \n2. **Lightroom** – Great for enhancing and organizing photos, especially across devices.  \n3. **Adobe Express** – Ideal for quick and simple edits with additional design features.\n\nWhat kind of edits are you looking to do? Let me know, and I'll guide you to the best app! 😊"
    
    val beachSampleMarkdown = "Looking for a beach trip for your summer getaway? Here are some fantastic beach destinations to consider:\n\n1. **[Miami](https://www.southwestvacations.com/destinations/united-states/miami-vacation-packages-mf1)** - Known for its vibrant nightlife and beautiful sandy shores, Miami is perfect for those who want to soak up the sun and enjoy a lively atmosphere.\n\n2. **[Fort Lauderdale](https://www.southwestvacations.com/destinations/united-states/fort-lauderdale-vacation-packages-fll)** - Famous for its boating canals and stunning beaches, Fort Lauderdale offers a relaxed beach vibe along with plenty of water activities.\n\n3. **[Destin](https://www.southwestvacations.com/destinations/united-states/south-walton-beach-destin-vacation-packages-ec1)** - Renowned for its emerald waters and sugar-white sand beaches, Destin is a great choice for families and beach lovers looking for fun in the sun.\n\n4. **[Pensacola](https://www.southwestvacations.com/destinations/united-states/pensacola-vacation-packages-pns)** - With its rich history and beautiful beaches, Pensacola provides a mix of cultural attractions and outdoor activities, making it a diverse getaway option.\n\n5. **[Naples](https://www.southwestvacations.com/destinations/united-states/naples-vacation-packages-na3/)** - Enjoy over ten miles of stunning white sand beaches and clear Gulf waters, along with a vibrant dining scene and family-friendly activities.\n\nEach of these destinations offers a unique beach experience, so you can find the perfect spot for your summer escape! If you'd like more information on any of these locations, just let me know!"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Markdown Parser Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Adobe Sample
        Text(
            text = "Adobe Sample",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ConciergeResponse(text = adobeSampleMarkdown)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Beach Destinations Sample
        Text(
            text = "Beach Destinations Sample",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ConciergeResponse(text = beachSampleMarkdown)
            }
        }
    }
}
