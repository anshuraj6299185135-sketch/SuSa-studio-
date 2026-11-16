package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOutUser
import com.example.data.model.SecurityAuditLog
import com.example.ui.theme.CyanAccent
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
import com.example.ui.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AccountSecurityScreen(
    user: FirebaseUser,
    auditLogsState: UiState<List<SecurityAuditLog>>,
    onSignOutComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Account & Enterprise Security", style = MaterialTheme.typography.headlineSmall, color = TextMain)
        }

        // User Account Card
        item {
            Surface(
                color = StudioSurface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val initial = (user.displayName ?: user.email ?: "S").first().uppercaseChar()
                            Text(initial.toString(), style = MaterialTheme.typography.headlineSmall, color = IndigoLight)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName ?: "SuSa Studio Member",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextMain,
                            )
                            Text(
                                text = user.email ?: "Google Authenticated Account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                            Text(
                                text = "UID: ${user.uid.take(14)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignOutComplete,
                            )
                        },
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out & Revoke Device Session")
                    }
                }
            }
        }

        // Enterprise Security Posture
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enterprise WAF & Vault Posture", style = MaterialTheme.typography.titleMedium, color = TextMain)
                SecurityPostureRow(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Firebase App Check Attestation",
                    subtitle = "Hardware & debug token verification active for AI Logic calls",
                    badge = "ACTIVE",
                    tint = EmeraldSuccess,
                )
                SecurityPostureRow(
                    icon = Icons.Filled.Lock,
                    title = "Zero-Trust Cloud Vault Encryption",
                    subtitle = "TLS 1.3 in transit • AES-256 at rest • Scoped per UID rules",
                    badge = "HARDENED",
                    tint = IndigoLight,
                )
                SecurityPostureRow(
                    icon = Icons.Filled.AdminPanelSettings,
                    title = "Google Credential Manager",
                    subtitle = "Passkey & Google Identity OAuth2 session management",
                    badge = "VERIFIED",
                    tint = VioletAccent,
                )
            }
        }

        // Live Audit Log
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                Text("Security Audit Trail", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
        }

        when (auditLogsState) {
            is UiState.Loading -> {
                item {
                    Text("Loading audit events...", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            is UiState.Error -> {
                item {
                    Text(auditLogsState.message, style = MaterialTheme.typography.bodySmall, color = RoseError)
                }
            }
            is UiState.Success -> {
                if (auditLogsState.data.isEmpty()) {
                    item {
                        Surface(
                            color = StudioSurface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Security events such as project creations, AI calls, and render dispatches will appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                } else {
                    items(auditLogsState.data, key = { it.id }) { log ->
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(log.eventType, style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                    val timeStr = log.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Just now"
                                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Text(log.detail, style = MaterialTheme.typography.bodySmall, color = TextMain)
                                Text(log.deviceLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityPostureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    tint: Color,
) {
    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(badge, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}
