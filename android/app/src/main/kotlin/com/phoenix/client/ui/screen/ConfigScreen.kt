package com.phoenix.client.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoenix.client.domain.model.ClientConfig
import com.phoenix.client.ui.theme.PhoenixOrange
import com.phoenix.client.ui.viewmodel.ConfigViewModel

private enum class PendingKeyAction { GENERATE, PICK_FILE }

// ── Entry point ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: ConfigViewModel = hiltViewModel()) {
    val configs by viewModel.configs.collectAsState()
    var editingConfig by remember { mutableStateOf<ClientConfig?>(null) }

    BackHandler(enabled = editingConfig != null) {
        editingConfig = null
    }

    if (editingConfig != null) {
        ConfigEditContent(
            viewModel = viewModel,
            initialConfig = editingConfig!!,
            onBack = { editingConfig = null },
        )
    } else {
        ConfigListContent(
            viewModel = viewModel,
            onEdit = { config -> editingConfig = config },
            onAdd = {
                editingConfig = ClientConfig(name = "Config ${configs.size + 1}")
            },
        )
    }
}

// ── List screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigListContent(
    viewModel: ConfigViewModel,
    onEdit: (ClientConfig) -> Unit,
    onAdd: () -> Unit,
) {
    val configs      by viewModel.configs.collectAsState()
    val activeConfig by viewModel.config.collectAsState()
    val context = LocalContext.current

    var configToDelete by remember { mutableStateOf<ClientConfig?>(null) }

    // Delete confirmation dialog
    configToDelete?.let { cfg ->
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Delete \"${cfg.name}\"?") },
            text = { Text("This configuration profile will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConfig(cfg.id)
                        configToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Configurations",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add configuration")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (configs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No configurations\nTap + to add one",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(configs, key = { it.id }) { config ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            configToDelete = config
                        }
                        false // always snap back — the dialog handles deletion
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 4.dp)
                                .background(
                                    color = if (isSwiping) MaterialTheme.colorScheme.errorContainer
                                            else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (isSwiping) {
                                Row(
                                    modifier = Modifier.padding(end = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    ConfigCard(
                        config = config,
                        isActive = config.id == activeConfig.id,
                        onTap = { viewModel.selectConfig(config.id) },
                        onEdit = { onEdit(config) },
                        onShare = { shareConfig(context, config) },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Config card ───────────────────────────────────────────────────────────────

@Composable
private fun ConfigCard(
    config: ClientConfig,
    isActive: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Orange left stripe when active
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (isActive) PhoenixOrange else Color.Transparent),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.name.ifBlank { "Unnamed" },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isActive) PhoenixOrange
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                        if (config.remoteAddr.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = config.remoteAddr,
                                style = MaterialTheme.typography.bodySmall
                                    .copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                maxLines = 1,
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                // Badges
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (tlsLabel, tlsColor) = when (config.tlsMode) {
                        "system"   -> "System CA" to MaterialTheme.colorScheme.primary
                        "insecure" -> "Insecure"  to MaterialTheme.colorScheme.error
                        else       -> if (config.serverPubKey.isNotBlank())
                                          "Phoenix TLS" to PhoenixOrange
                                      else
                                          "h2c" to MaterialTheme.colorScheme.secondary
                    }
                    ConfigBadge(tlsLabel, tlsColor)
                    if (config.useVpnMode) ConfigBadge("VPN", PhoenixOrange)
                    if (config.authToken.isNotBlank()) ConfigBadge("Auth", MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
private fun ConfigBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ── Edit screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigEditContent(
    viewModel: ConfigViewModel,
    initialConfig: ClientConfig,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Form state — seeded from initialConfig once; updated only by user interaction
    var name           by remember { mutableStateOf(initialConfig.name) }
    var remoteAddr     by remember { mutableStateOf(initialConfig.remoteAddr) }
    var serverPubKey   by remember { mutableStateOf(initialConfig.serverPubKey) }
    var localSocksAddr by remember { mutableStateOf(initialConfig.localSocksAddr) }
    var enableUdp      by remember { mutableStateOf(initialConfig.enableUdp) }
    var authToken      by remember { mutableStateOf(initialConfig.authToken) }
    var tlsMode        by remember { mutableStateOf(initialConfig.tlsMode) }
    var tlsModeExpanded by remember { mutableStateOf(false) }
    var fingerprint    by remember { mutableStateOf(initialConfig.fingerprint) }
    var fingerprintExpanded by remember { mutableStateOf(false) }

    var useMtls by remember { mutableStateOf(initialConfig.privateKeyFile.isNotBlank()) }
    var privateKeyFile by remember { mutableStateOf(initialConfig.privateKeyFile) }
    var clientPublicKey by remember { mutableStateOf(initialConfig.clientPublicKey) }
    var publicKeyVisible by remember { mutableStateOf(false) }

    val hasUnsavedChanges = name != initialConfig.name ||
        remoteAddr.trim() != initialConfig.remoteAddr ||
        serverPubKey.trim() != initialConfig.serverPubKey ||
        localSocksAddr.trim() != initialConfig.localSocksAddr ||
        enableUdp != initialConfig.enableUdp ||
        authToken.trim() != initialConfig.authToken ||
        tlsMode != initialConfig.tlsMode ||
        fingerprint != initialConfig.fingerprint

    var showOverwriteConfirm by remember { mutableStateOf(false) }
    var pendingKeyAction     by remember { mutableStateOf<PendingKeyAction?>(null) }

    val keyFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.onKeyFilePicked(it, initialConfig.id) } }

    // Pick up key generation results into local form state
    LaunchedEffect(uiState.generatedPrivateKeyFile, uiState.pickedKeyFile) {
        val genFile    = uiState.generatedPrivateKeyFile
        val pickedFile = uiState.pickedKeyFile
        if (genFile != null) {
            privateKeyFile = genFile
            useMtls = true
        }
        if (pickedFile != null) {
            privateKeyFile = pickedFile
            clientPublicKey = ""
            useMtls = true
        }
        if (genFile != null || pickedFile != null) {
            viewModel.consumeKeyFileEvents()
        }
    }

    // Sync local clientPublicKey when a new key pair is generated
    LaunchedEffect(uiState.generatedPublicKey) {
        uiState.generatedPublicKey?.let { clientPublicKey = it }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    if (showOverwriteConfirm) {
        AlertDialog(
            onDismissRequest = { showOverwriteConfirm = false; pendingKeyAction = null },
            title = { Text("Replace existing key?") },
            text = {
                Text(
                    "A client private key already exists. " +
                        if (pendingKeyAction == PendingKeyAction.GENERATE)
                            "Generating a new key will overwrite it and you will need to update the server's authorized_clients list."
                        else
                            "Importing a new file will replace the current key.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirm = false
                        when (pendingKeyAction) {
                            PendingKeyAction.GENERATE -> viewModel.generateKeys(initialConfig.id)
                            PendingKeyAction.PICK_FILE -> keyFilePicker.launch(arrayOf("*/*"))
                            null -> {}
                        }
                        pendingKeyAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PhoenixOrange),
                ) {
                    Text(
                        text = if (pendingKeyAction == PendingKeyAction.GENERATE) "Generate" else "Choose File",
                        color = Color.Black,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirm = false; pendingKeyAction = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    uiState.generatedPublicKey?.let { pubKey ->
        PublicKeyDialog(
            publicKey = pubKey,
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Phoenix Public Key", pubKey))
            },
            onDismiss = viewModel::dismissPublicKeyDialog,
        )
    }

    uiState.keyGenError?.let { err ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPublicKeyDialog,
            title = { Text("Key operation failed") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPublicKeyDialog) { Text("OK") }
            },
        )
    }

    // ── Screen content ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        // Back button + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (initialConfig.name.isBlank()) "New Configuration"
                       else "Edit — ${initialConfig.name}",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Identity ──────────────────────────────────────────────────────
        SectionLabel("Identity")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile Name") },
            placeholder = { Text("e.g. Home, Work, CDN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldDescription("A label to identify this configuration profile.")

        Spacer(Modifier.height(24.dp))

        // ── Server ────────────────────────────────────────────────────────
        SectionLabel("Server")

        OutlinedTextField(
            value = remoteAddr,
            onValueChange = { remoteAddr = it },
            label = { Text("Server Address") },
            placeholder = { Text("host:port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldDescription("Address and port of your Phoenix server (e.g. example.com:443).")

        Spacer(Modifier.height(24.dp))

        // ── TLS & Authentication ──────────────────────────────────────────
        SectionLabel("TLS & Authentication")

        OutlinedTextField(
            value = serverPubKey,
            onValueChange = { serverPubKey = it },
            label = { Text("Server Public Key") },
            placeholder = { Text("Base64-encoded Ed25519 public key") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )
        FieldDescription(
            "The server's Ed25519 public key.\n" +
                "• Empty → h2c mode (cleartext HTTP/2, for CDN setups)\n" +
                "• Set → TLS mode (One-Way TLS or mTLS)"
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = authToken,
            onValueChange = { authToken = it },
            label = { Text("Auth Token") },
            placeholder = { Text("Shared secret (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldDescription("Must match auth_token on the server. Leave empty if not used.")

        Spacer(Modifier.height(16.dp))

        val tlsModeLabels = mapOf(
            ""         to "Phoenix Pinning (default)",
            "system"   to "System CA — CDN / Cloudflare",
            "insecure" to "Insecure — accept any certificate",
        )
        ExposedDropdownMenuBox(
            expanded = tlsModeExpanded,
            onExpandedChange = { tlsModeExpanded = it },
        ) {
            OutlinedTextField(
                value = tlsModeLabels[tlsMode] ?: tlsMode,
                onValueChange = {},
                readOnly = true,
                label = { Text("TLS Mode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlsModeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = tlsModeExpanded,
                onDismissRequest = { tlsModeExpanded = false },
            ) {
                tlsModeLabels.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { tlsMode = value; tlsModeExpanded = false },
                    )
                }
            }
        }
        FieldDescription(
            "• Phoenix Pinning — verify server by Ed25519 public key (default)\n" +
                "• System CA — trust OS certificate store (CDN / Cloudflare)\n" +
                "• Insecure — skip verification (no key needed)"
        )

        Spacer(Modifier.height(16.dp))

        val fingerprintLabels = mapOf(
            ""        to "Default (no spoofing)",
            "chrome"  to "Chrome",
            "firefox" to "Firefox",
            "safari"  to "Safari",
            "random"  to "Random",
        )
        ExposedDropdownMenuBox(
            expanded = fingerprintExpanded,
            onExpandedChange = { fingerprintExpanded = it },
        ) {
            OutlinedTextField(
                value = fingerprintLabels[fingerprint] ?: fingerprint,
                onValueChange = {},
                readOnly = true,
                label = { Text("TLS Fingerprint") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fingerprintExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = fingerprintExpanded,
                onDismissRequest = { fingerprintExpanded = false },
            ) {
                fingerprintLabels.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { fingerprint = value; fingerprintExpanded = false },
                    )
                }
            }
        }
        FieldDescription("Spoof TLS ClientHello fingerprint to bypass DPI. Chrome recommended.")

        Spacer(Modifier.height(16.dp))

        // mTLS toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mutual TLS (mTLS)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Enable only if your server has authorized_clients set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = useMtls,
                onCheckedChange = { enabled ->
                    useMtls = enabled
                    if (!enabled) privateKeyFile = ""
                },
            )
        }

        if (useMtls) {
            Spacer(Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Private Key Path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = if (privateKeyFile.isNotBlank())
                                "${context.filesDir.absolutePath}/$privateKeyFile"
                            else
                                "No key file selected",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (privateKeyFile.isNotBlank()) PhoenixOrange
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (privateKeyFile.isNotBlank()) {
                            pendingKeyAction = PendingKeyAction.GENERATE
                            showOverwriteConfirm = true
                        } else {
                            viewModel.generateKeys(initialConfig.id)
                        }
                    },
                    enabled = !uiState.isGeneratingKeys,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PhoenixOrange),
                ) {
                    if (uiState.isGeneratingKeys) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PhoenixOrange,
                        )
                    } else {
                        Text("Generate Key", color = PhoenixOrange)
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (privateKeyFile.isNotBlank()) {
                            pendingKeyAction = PendingKeyAction.PICK_FILE
                            showOverwriteConfirm = true
                        } else {
                            keyFilePicker.launch(arrayOf("*/*"))
                        }
                    },
                    enabled = !uiState.isGeneratingKeys,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Choose File")
                }
            }

            FieldDescription(
                "Your client Ed25519 private key. After generating, copy the public key to the server's authorized_clients list."
            )

            Spacer(Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Client Public Key",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Row {
                            if (clientPublicKey.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Phoenix Client Public Key", clientPublicKey))
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy public key",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { publicKeyVisible = !publicKeyVisible },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = if (publicKeyVisible) Icons.Outlined.VisibilityOff
                                                  else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    when {
                        clientPublicKey.isBlank() -> Text(
                            "Not available — generate a new key pair to see it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        publicKeyVisible -> SelectionContainer {
                            Text(
                                text = clientPublicKey,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = PhoenixOrange,
                            )
                        }
                        else -> Text(
                            text = "•".repeat(44),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Network ───────────────────────────────────────────────────────
        SectionLabel("Network")

        OutlinedTextField(
            value = localSocksAddr,
            onValueChange = { localSocksAddr = it },
            label = { Text("Local SOCKS5 Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldDescription("Local address where Phoenix listens for SOCKS5 connections. Default: 0.0.0.0:1080 (all interfaces).")

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Enable UDP (SOCKS5)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Allow UDP ASSOCIATE for DNS and other UDP traffic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enableUdp, onCheckedChange = { enableUdp = it })
        }

        Spacer(Modifier.height(32.dp))

        if (hasUnsavedChanges) {
            Text(
                text = "You have unsaved changes — press Save before connecting.",
                style = MaterialTheme.typography.bodySmall,
                color = PhoenixOrange,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                viewModel.save(
                    ClientConfig(
                        id              = initialConfig.id,
                        name            = name.trim().ifBlank { "Config" },
                        remoteAddr      = remoteAddr.trim(),
                        serverPubKey    = serverPubKey.trim(),
                        privateKeyFile  = if (useMtls) privateKeyFile.trim() else "",
                        clientPublicKey = if (useMtls) clientPublicKey else "",
                        useVpnMode      = initialConfig.useVpnMode,
                        localSocksAddr  = localSocksAddr.trim(),
                        enableUdp       = enableUdp,
                        authToken       = authToken.trim(),
                        tlsMode         = tlsMode,
                        fingerprint     = fingerprint,
                    ),
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PhoenixOrange),
        ) {
            Text("Save Configuration", color = Color.Black)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PhoenixOrange,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun FieldDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun PublicKeyDialog(
    publicKey: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keys Generated") },
        text = {
            Column {
                Text(
                    text = "Your client key pair has been created. " +
                        "Add this public key to your server's authorized_clients list:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        text = publicKey,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = PhoenixOrange,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Private key saved to app storage as client.private.key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCopy(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = PhoenixOrange),
            ) {
                Text("Copy & Close", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

// ── Share helper (non-composable) ─────────────────────────────────────────────

private fun shareConfig(context: Context, config: ClientConfig) {
    val tlsLabel = when (config.tlsMode) {
        "system"   -> "System CA (CDN / Cloudflare)"
        "insecure" -> "Insecure (no verification)"
        else       -> if (config.serverPubKey.isNotBlank()) "Phoenix Pinning" else "h2c (cleartext)"
    }
    val text = buildString {
        appendLine("Phoenix Config: ${config.name}")
        appendLine("Server: ${config.remoteAddr.ifBlank { "(not set)" }}")
        appendLine("TLS: $tlsLabel")
        if (config.serverPubKey.isNotBlank()) appendLine("Server Public Key: ${config.serverPubKey}")
        appendLine("VPN Mode: ${if (config.useVpnMode) "Enabled" else "Disabled"}")
        appendLine("Local SOCKS5: ${config.localSocksAddr}")
        if (config.fingerprint.isNotBlank()) appendLine("Fingerprint: ${config.fingerprint}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Phoenix Config: ${config.name}")
        putExtra(Intent.EXTRA_TEXT, text.trim())
    }
    context.startActivity(Intent.createChooser(intent, "Share Configuration"))
}
