package com.apexfission.android.carddetectionlite.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.R

data class PermissionFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
fun PermissionScreen(
    onBack: () -> Unit,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.camera_permission_title),
    body: String = stringResource(R.string.camera_permission_body),
    features: List<PermissionFeature> = listOf(
        PermissionFeature(
            icon = Icons.Default.VerifiedUser,
            title = stringResource(R.string.permission_feature_title_identity),
            subtitle = stringResource(R.string.permission_feature_subtitle_identity)
        )
    )
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header ---
            HeaderSection(onBack)

            Spacer(modifier = Modifier.height(10.dp))

            // --- Illustration Area ---
            CameraIllustration()

            Spacer(modifier = Modifier.height(20.dp))

            // --- Text Content ---
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // --- Trust Features ---
            features.forEach { feature ->
                FeatureItem(
                    icon = feature.icon,
                    title = feature.title,
                    subtitle = feature.subtitle
                )
                Spacer(modifier = Modifier.height(20.dp))
            }


            Spacer(modifier = Modifier.weight(1f))

            // --- Footer Actions ---
            Button(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Allow Camera Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = "Powered by LambdaWalker",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun HeaderSection(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.size(40.dp)) // Symmetry spacer
    }
}

@Composable
fun CameraIllustration() {
    Box(
        modifier = Modifier
            .height(210.dp)
            .width(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ID Card
        Box(
            modifier = Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 6.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 6.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape)
                )
            }

            // Scanning Line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Camera Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF101622
)
@Composable
fun SmallDevicePreview() {
    MaterialTheme {
        PermissionScreen(
            title = "Camera Access Required",
            body = "To create your secure signing profile and log your first contract, we need to scan your government-issued ID. This ensures that only you can sign and verify your documents.",
            features = listOf(
                PermissionFeature(
                    icon = Icons.Default.VerifiedUser,
                    title = "Identity Verification",
                    subtitle = "Securely confirm your identity to prevent fraud."
                ),
                PermissionFeature(
                    icon = Icons.Default.Lock,
                    title = "Encrypted Storage",
                    subtitle = "Your data is encrypted and stored locally on your device."
                )
            ),
            onBack = {},
            onAllow = {},
            onNotNow = {}
        )
    }
}