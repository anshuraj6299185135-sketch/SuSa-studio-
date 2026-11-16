package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseError
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isAutoSigningIn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isAutoSigningIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isAutoSigningIn = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_studio_hero),
            contentDescription = "SuSa studio backdrop",
            contentScale = ContentScale.Crop,
            alpha = 0.28f,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ObsidianBg)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, IndigoPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MovieCreation,
                            contentDescription = "SuSa studio emblem",
                            tint = IndigoLight,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextMain, fontWeight = FontWeight.Bold)) {
                                append("SuSa ")
                            }
                            withStyle(SpanStyle(color = IndigoPrimary, fontWeight = FontWeight.Bold)) {
                                append("studio")
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    color = IndigoPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "NEXT-GEN CLOUD MEDIA SUITE",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoLight,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Edit, Scout & Render in the Cloud.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(TextMain, IndigoLight, VioletAccent)
                        )
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Multi-track 4K/8K timelines, Gemini AI storyboard & auto-captions, Google Maps shoot scouting, and encrypted Firestore team sync.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FeaturePill(Icons.Filled.AutoAwesome, "Gemini AI Studio", VioletAccent)
                    FeaturePill(Icons.Filled.Place, "Maps Shoot Scout", EmeraldSuccess)
                    FeaturePill(Icons.Filled.CloudDone, "GPU Cloud Render", IndigoLight)
                    FeaturePill(Icons.Filled.Shield, "App Check + TLS 1.3", EmeraldSuccess)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Encrypted vault",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Zero-Trust Cloud Portal",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMain,
                        )
                    }

                    Text(
                        text = "Sign in with your Google Studio account to unlock synced timelines, AI automation, and your encrypted 30-day cloud vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )

                    if (errorMessage != null) {
                        Surface(
                            color = RoseError.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseError,
                                modifier = Modifier.padding(12.dp),
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
                                scope = scope,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading && !isAutoSigningIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White,
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button"),
                    ) {
                        if (isLoading || isAutoSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAutoSigningIn) "Checking Studio session..." else "Authenticating...",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    label: String,
    tint: Color,
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMain,
            )
        }
    }
}
