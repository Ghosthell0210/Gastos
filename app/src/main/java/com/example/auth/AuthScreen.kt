package com.example.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.PesoClaroThemeExtras

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val semantic = PesoClaroThemeExtras.semanticColors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(semantic.heroGradientStart, semantic.heroGradientMid)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "PesoClaro",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "FINANZAS PERSONALES · CUP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Custom Fintech Emerald & Slate Hero Artwork
                FintechHeroArtwork(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.auth_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureRow(
                        icon = Icons.Rounded.AutoGraph,
                        title = "Gastos, presupuestos (75% / 100%) y reportes",
                        subtitle = "Visualiza tu consumo mensual en CUP con alertas por límite."
                    )
                    FeatureRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Deudas bidireccionales y recordatorios",
                        subtitle = "Registra lo que debes y lo que te deben con abonos y alertas."
                    )
                    FeatureRow(
                        icon = Icons.Rounded.CloudDone,
                        title = "Respaldo privado en la nube",
                        subtitle = "Tus datos permanecen sincronizados con tu cuenta de Google."
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.auth_signing_in),
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auth_sign_in_google),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FintechHeroArtwork(modifier: Modifier = Modifier) {
    val semantic = PesoClaroThemeExtras.semanticColors
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(semantic.heroGradientStart, semantic.heroGradientMid, semantic.heroGradientEnd)
                )
            )
    ) {
        val w = size.width
        val h = size.height

        // Decorative translucent radial orbs
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = h * 0.65f,
            center = Offset(w * 0.88f, h * 0.2f)
        )
        drawCircle(
            color = primary.copy(alpha = 0.22f),
            radius = h * 0.45f,
            center = Offset(w * 0.14f, h * 0.85f)
        )

        // Left ledger card silhouette
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset(w * 0.08f, h * 0.2f),
            size = Size(w * 0.42f, h * 0.62f),
            cornerRadius = CornerRadius(24f, 24f)
        )

        // Ascending bars inside left card
        val barWidth = w * 0.055f
        val barBaseY = h * 0.72f
        val heights = listOf(0.20f, 0.34f, 0.26f, 0.44f)
        heights.forEachIndexed { idx, frac ->
            val bh = h * frac
            val bx = w * 0.13f + idx * (barWidth + w * 0.025f)
            drawRoundRect(
                color = if (idx == 3) Color(0xFFA7F3D0) else Color.White.copy(alpha = 0.75f),
                topLeft = Offset(bx, barBaseY - bh),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        // Right mini donut ring card
        drawRoundRect(
            color = surface.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.56f, h * 0.18f),
            size = Size(w * 0.36f, h * 0.64f),
            cornerRadius = CornerRadius(24f, 24f)
        )

        val ringCenter = Offset(w * 0.74f, h * 0.50f)
        val ringDiameter = minOf(w * 0.22f, h * 0.44f)
        val ringTopLeft = Offset(ringCenter.x - ringDiameter / 2f, ringCenter.y - ringDiameter / 2f)
        val stroke = Stroke(width = 18f, cap = StrokeCap.Round)

        drawArc(
            color = Color(0xFF34D399),
            startAngle = -90f,
            sweepAngle = 195f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = Size(ringDiameter, ringDiameter),
            style = stroke
        )
        drawArc(
            color = Color(0xFFFBBF24),
            startAngle = 115f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = Size(ringDiameter, ringDiameter),
            style = stroke
        )
        drawArc(
            color = surfaceVariant.copy(alpha = 0.55f),
            startAngle = 210f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = Size(ringDiameter, ringDiameter),
            style = stroke
        )
    }
}
