package com.phoenix.client.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoenix.client.BuildConfig
import com.phoenix.client.ui.theme.PhoenixOrange
import com.phoenix.client.ui.viewmodel.SettingsViewModel
import com.phoenix.client.util.UpdateChecker

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(24.dp))

        // ── Connection Mode ──────────────────────────────────────────────────
        Text(
            text = "CONNECTION MODE",
            style = MaterialTheme.typography.labelSmall,
            color = PhoenixOrange,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Choose how Phoenix routes your device traffic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(12.dp))

        ModeOptionCard(
            selected = !config.useVpnMode,
            onClick = { viewModel.setVpnMode(false) },
            title = "SOCKS5 Proxy",
            description = "Phoenix listens locally on 127.0.0.1:10080. Configure individual apps to use this proxy. No root or VPN permission required.",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = if (!config.useVpnMode) PhoenixOrange
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            },
        )

        Spacer(Modifier.height(8.dp))

        ModeOptionCard(
            selected = config.useVpnMode,
            onClick = { viewModel.setVpnMode(true) },
            title = "VPN Mode",
            description = "Routes all device traffic through Phoenix automatically. Android VPN permission is required. No root needed.",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.VpnLock,
                    contentDescription = null,
                    tint = if (config.useVpnMode) PhoenixOrange
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            },
        )

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(Modifier.height(24.dp))

        // ── About ────────────────────────────────────────────────────────────
        Text(
            text = "ABOUT",
            style = MaterialTheme.typography.labelSmall,
            color = PhoenixOrange,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val context = LocalContext.current
        LinkRow(
            icon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            title = "GitHub",
            subtitle = "Releases, source code and issue tracker",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_URL))) },
        )

        Spacer(Modifier.height(4.dp))

        LinkRow(
            icon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            title = "Telegram",
            subtitle = "Announcements and community support",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.TELEGRAM_URL))) },
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun LinkRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String,
    icon: @Composable () -> Unit,
) {
    val borderColor = if (selected) PhoenixOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bgColor = if (selected) PhoenixOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            icon()
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) PhoenixOrange else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
