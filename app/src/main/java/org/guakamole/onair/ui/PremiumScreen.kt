package org.guakamole.onair.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.guakamole.onair.BuildConfig
import org.guakamole.onair.R
import org.guakamole.onair.billing.PremiumManager

/** Premium purchase screen showing available premium features and purchase option. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PremiumScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val premiumManager = remember { PremiumManager.getInstance(context) }

        val isPremium by premiumManager.isPremium.collectAsState()
        val isLoading by premiumManager.isLoading.collectAsState()
        val productDetails by premiumManager.productDetails.collectAsState()

        val price = premiumManager.getFormattedPrice() ?: stringResource(R.string.premium_loading)

        val gradient =
                Brush.verticalGradient(
                        colors =
                                listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.background
                                )
                )

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text(stringResource(R.string.premium_features)) },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        imageVector = Icons.Default.ArrowBack,
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string.back_to_stations
                                                                )
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent
                                        )
                        )
                }
        ) { paddingValues ->
                Column(
                        modifier =
                                modifier.fillMaxSize()
                                        .background(gradient)
                                        .padding(paddingValues)
                                        .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Premium icon
                        Box(
                                modifier =
                                        Modifier.size(100.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiary
                                                                        )
                                                        )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector =
                                                if (isPremium) Icons.Default.CheckCircle
                                                else Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp),
                                        tint = Color.White
                                )
                        }

                        // Status text
                        AnimatedContent(targetState = isPremium, label = "premium_status") { premium
                                ->
                                if (premium) {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string.premium_already_owned
                                                        ),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                } else {
                                        Text(
                                                text = stringResource(R.string.premium_unlock),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Feature list
                        FeatureCard(
                                icon = Icons.Default.DirectionsCar,
                                title = stringResource(R.string.premium_android_auto),
                                description = stringResource(R.string.premium_android_auto_desc)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Purchase button or status
                        if (!isPremium) {
                                Button(
                                        onClick = {
                                                (context as? Activity)?.let { activity ->
                                                        premiumManager.purchase(activity)
                                                }
                                        },
                                        enabled = !isLoading && productDetails != null,
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(28.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.primary
                                                )
                                ) {
                                        if (isLoading) {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                )
                                        } else {
                                                Text(
                                                        text =
                                                                "${stringResource(R.string.premium_purchase)} - $price",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }

                                // Restore purchase button
                                val scope = rememberCoroutineScope()
                                TextButton(
                                        onClick = {
                                                scope.launch { premiumManager.restorePurchases() }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Text(
                                                text = stringResource(R.string.premium_restore),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }
                        }

                        // Debug toggle (only in debug builds)
                        if (BuildConfig.DEBUG) {
                                val isDebug =
                                        premiumManager.isDebugPremiumEnabled() ||
                                                !premiumManager.isPremium.value
                                if (isDebug) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                                onClick = {
                                                        premiumManager.debugSetPremium(!isPremium)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                Text(
                                                        text =
                                                                if (isPremium)
                                                                        stringResource(
                                                                                R.string
                                                                                        .premium_debug_enabled
                                                                        )
                                                                else
                                                                        stringResource(
                                                                                R.string
                                                                                        .premium_debug_disabled
                                                                        ),
                                                        color = MaterialTheme.colorScheme.secondary
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                }
        }
}

@Composable
private fun FeatureCard(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.15f
                                                        )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                }
        }
}
