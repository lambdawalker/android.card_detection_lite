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

/**
 * A data model for representing a feature or benefit that is enabled by a permission.
 * This is used to populate the list of reasons why a user should grant the permission.
 *
 * @property icon The [ImageVector] to display next to the feature text.
 * @property title A short, bolded title for the feature (e.g., "Identity Verification").
 * @property subtitle A concise description of the feature's benefit.
 */
data class PermissionFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

/**
 * A reusable, full-screen UI Composable for explaining why a permission is necessary and
 * prompting the user to grant it.
 *
 * This screen is designed to be user-friendly and informative, increasing the likelihood
 * of permission acceptance by clearly communicating the value exchange.
 *
 * @param onBack A lambda function to be invoked when the user presses the top-left back arrow.
 *               This should typically navigate the user away from the feature that requires
 *               the permission.
 * @param onAllow A lambda function to be invoked when the user presses the primary action button
 *                (e.g., "Allow Camera Access"). This callback is expected to trigger the
 *                actual system permission request dialog.
 * @param onNotNow A lambda function for the secondary action, allowing the user to dismiss
 *                 the screen temporarily without making a permanent decision.
 * @param modifier A [Modifier] to be applied to the root `Surface` of the screen.
 * @param title The main headline of the screen, explaining what permission is needed.
 * @param body A more detailed paragraph explaining *why* the permission is essential for the
 *             app's functionality.
 * @param features A list of [PermissionFeature]s to be displayed as bullet points, highlighting
 *                 the concrete benefits the user will gain by granting the permission.
 */
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
            HeaderSection(onBack)
            Spacer(modifier = Modifier.height(10.dp))
            CameraIllustration()
            Spacer(modifier = Modifier.height(20.dp))

            // Main text content explaining the permission.
            Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.weight(0.5f))

            // List of features enabled by the permission.
            features.forEach { feature ->
                FeatureItem(icon = feature.icon, title = feature.title, subtitle = feature.subtitle)
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))

            // Action buttons for the user.
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Allow Camera Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) {
                Text("Not Now", fontWeight = FontWeight.SemiBold)
            }

            Text(text = "Powered by LambdaWalker", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 16.dp), letterSpacing = 2.sp)
        }
    }
}

/**
 * @Composable private
 * The header component of the permission screen. Includes a back button and a visual progress indicator.
 * @param onBack The callback to execute when the back button is clicked.
 */
@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.width(100.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Box(modifier = Modifier.fillMaxWidth(0.33f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
        Spacer(modifier = Modifier.size(40.dp)) // Spacer for symmetrical alignment.
    }
}

/**
 * @Composable private
 * A decorative visual element that abstractly represents the camera scanning an ID card.
 */
@Composable
private fun CameraIllustration() {
    Box(
        modifier = Modifier.height(210.dp).width(260.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent))),
        contentAlignment = Alignment.Center
    ) {
        // Mock ID Card element
        Box(modifier = Modifier.size(width = 180.dp, height = 110.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Box(modifier = Modifier.size(width = 80.dp, height = 6.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(width = 50.dp, height = 6.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(width = 60.dp, height = 6.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape))
            }
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
        }
        // Mock Camera Badge element
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

/**
 * @Composable private
 * Displays a single row for a [PermissionFeature], containing its icon, title, and subtitle.
 */
@Composable
private fun FeatureItem(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101622)
@Composable
private fun SmallDevicePreview() {
    MaterialTheme {
        PermissionScreen(
            title = "Camera Access Required",
            body = "To create your secure signing profile and log your first contract, we need to scan your government-issued ID. This ensures that only you can sign and verify your documents.",
            features = listOf(
                PermissionFeature(Icons.Default.VerifiedUser, "Identity Verification", "Securely confirm your identity to prevent fraud."),
                PermissionFeature(Icons.Default.Lock, "Encrypted Storage", "Your data is encrypted and stored locally on your device.")
            ),
            onBack = {}, onAllow = {}, onNotNow = {}
        )
    }
}
