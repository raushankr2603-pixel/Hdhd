package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AiRouterEngine
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary

private data class ProviderPreset(
    val label: String,
    val providerName: String,
    val baseUrl: String,
    val defaultModel: String,
    val isLocal: Boolean,
    val apiKeyHint: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProvidersScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val offlineLock by viewModel.offlinePrivacyLock.collectAsStateWithLifecycle()
    val discoveredModels by viewModel.discoveredModels.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscoveringModels.collectAsStateWithLifecycle()
    val liveTestResult by viewModel.providerLiveTestResult.collectAsStateWithLifecycle()

    var editingProviderId by remember { mutableStateOf<Int?>(null) }
    var providerName by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var isLocalProvider by remember { mutableStateOf(false) }
    var showApiKeyPlain by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Local (Ollama/Edge), 2 = Cloud (NIM/OpenRouter/Groq/OpenAI)

    val presets = remember {
        listOf(
            ProviderPreset(
                label = "Ollama Local",
                providerName = "Ollama Local Engine",
                baseUrl = "http://127.0.0.1:11434/v1",
                defaultModel = "llama3.2:3b",
                isLocal = true,
                apiKeyHint = "Optional for local Ollama (default: ollama)"
            ),
            ProviderPreset(
                label = "NVIDIA NIM",
                providerName = "NVIDIA NIM Cloud",
                baseUrl = "https://integrate.api.nvidia.com/v1",
                defaultModel = "meta/llama-3.1-70b-instruct",
                isLocal = false,
                apiKeyHint = "nvapi-... key from build.nvidia.com"
            ),
            ProviderPreset(
                label = "OpenRouter",
                providerName = "OpenRouter Unified API",
                baseUrl = "https://openrouter.ai/api/v1",
                defaultModel = "anthropic/claude-3.5-sonnet",
                isLocal = false,
                apiKeyHint = "sk-or-v1-... key from openrouter.ai"
            ),
            ProviderPreset(
                label = "Groq Cloud",
                providerName = "Groq Cloud LPU API",
                baseUrl = "https://api.groq.com/openai/v1",
                defaultModel = "llama-3.3-70b-versatile",
                isLocal = false,
                apiKeyHint = "gsk_... key from console.groq.com"
            ),
            ProviderPreset(
                label = "xAI Grok",
                providerName = "xAI Grok API",
                baseUrl = "https://api.x.ai/v1",
                defaultModel = "grok-2-latest",
                isLocal = false,
                apiKeyHint = "xai-... key from console.x.ai"
            ),
            ProviderPreset(
                label = "OpenAI-Compatible",
                providerName = "OpenAI / vLLM / Custom Endpoint",
                baseUrl = "https://api.openai.com/v1",
                defaultModel = "gpt-4o-mini",
                isLocal = false,
                apiKeyHint = "sk-... OpenAI or compatible bearer key"
            ),
            ProviderPreset(
                label = "LM Studio Local",
                providerName = "LM Studio Local Server",
                baseUrl = "http://127.0.0.1:1234/v1",
                defaultModel = "qwen2.5-coder-7b-instruct",
                isLocal = true,
                apiKeyHint = "lm-studio (local LAN/localhost)"
            ),
            ProviderPreset(
                label = "Local Edge GGUF",
                providerName = "On-Device GGUF Quantized",
                baseUrl = "local://on-device-neural-engine",
                defaultModel = "qwen2.5-coder-3b-q4_k_m",
                isLocal = true,
                apiKeyHint = "No API key needed (100% On-Device)"
            )
        )
    }

    val filteredProviders = remember(providers, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> providers.filter { it.isLocalOffline }
            2 -> providers.filter { !it.isLocalOffline }
            else -> providers
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Live Model Verification Output Banner (when tested)
        liveTestResult?.let { testOutput ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, QuantumEmerald, RoundedCornerShape(18.dp))
                        .testTag("provider_live_test_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE AI MODEL DIAGNOSTIC RESPONSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = testOutput,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.clearProviderLiveTestResult() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss test result",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 1. Offline Privacy Lock & Routing Mode Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = QuantumEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% OFFLINE PRIVACY & LOCAL PROCESSING",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Force all voice commands, screen interactions, and automation tasks to run strictly on local/Ollama models with zero external cloud calls.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = offlineLock,
                        onCheckedChange = { viewModel.toggleOfflinePrivacyLock(it) },
                        modifier = Modifier.testTag("models_offline_switch")
                    )
                }
            }
        }

        // 2. Configure Local or Cloud AI Provider Form (Provider Name, Base URL, API Key, Model Name)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (editingProviderId != null) 1.5.dp else 1.dp,
                        color = if (editingProviderId != null) CyberCyan else BorderSubtle,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingProviderId == null) {
                                    "CONFIGURE LOCAL OR CLOUD AI PROVIDER"
                                } else {
                                    "EDITING AI PROVIDER #$editingProviderId"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Ollama, NVIDIA NIM, OpenRouter, Groq, xAI Grok & OpenAI-Compatible APIs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (editingProviderId != null) {
                            OutlinedButton(
                                onClick = {
                                    editingProviderId = null
                                    providerName = ""
                                    baseUrl = ""
                                    apiKey = ""
                                    modelName = ""
                                }
                            ) {
                                Text("Cancel Edit", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick-Select Provider Template (Local & Cloud):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    editingProviderId = null
                                    providerName = preset.providerName
                                    baseUrl = preset.baseUrl
                                    modelName = preset.defaultModel
                                    isLocalProvider = preset.isLocal
                                    viewModel.loadCuratedModelsForDraft(preset.providerName, preset.baseUrl)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (preset.isLocal) Icons.Default.Memory else Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = if (preset.isLocal) QuantumEmerald else CyberCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("preset_${preset.label.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = providerName,
                        onValueChange = {
                            providerName = it
                            viewModel.loadCuratedModelsForDraft(it, baseUrl)
                        },
                        label = { Text("1. Provider Name (e.g. Ollama, NVIDIA NIM, OpenRouter, Groq, Grok)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provider_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            viewModel.loadCuratedModelsForDraft(providerName, it)
                        },
                        label = { Text("2. Base URL (e.g. http://127.0.0.1:11434/v1 or https://api.groq.com/openai/v1)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("base_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("3. API Key (Encrypted in AES-256-GCM Vault)") },
                        singleLine = true,
                        visualTransformation = if (showApiKeyPlain) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showApiKeyPlain = !showApiKeyPlain }) {
                                Icon(
                                    imageVector = if (showApiKeyPlain) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API key visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            label = { Text("4. Model Name (e.g. llama-3.3-70b-versatile, llama3.2:3b)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("model_name_input")
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.discoverModelsFromEndpoint(
                                    providerName = providerName,
                                    baseUrl = baseUrl,
                                    rawApiKey = apiKey
                                )
                            },
                            modifier = Modifier.testTag("discover_models_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Fetch Models from Endpoint",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Models", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (isDiscovering) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            color = CyberCyan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (discoveredModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a Model for this Provider (Tap chip to populate Model Name):",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            discoveredModels.forEach { candidateModel ->
                                FilterChip(
                                    selected = modelName == candidateModel,
                                    onClick = { modelName = candidateModel },
                                    label = { Text(candidateModel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = QuantumEmerald.copy(alpha = 0.22f),
                                        selectedLabelColor = QuantumEmerald
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isLocalProvider,
                            onCheckedChange = { isLocalProvider = it }
                        )
                        Text(
                            text = "Local / On-Device / Ollama LAN Model (Permitted when 100% Offline Lock is ON)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.saveOrUpdateCustomAiProvider(
                                editingId = editingProviderId,
                                providerName = providerName,
                                baseUrl = baseUrl,
                                rawApiKey = apiKey,
                                modelName = modelName,
                                isLocalOffline = isLocalProvider,
                                activateImmediately = true
                            )
                            editingProviderId = null
                            providerName = ""
                            baseUrl = ""
                            apiKey = ""
                            modelName = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_provider_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (editingProviderId == null) "Save & Activate AI Provider"
                            else "Update & Activate AI Provider"
                        )
                    }
                }
            }
        }

        // 3. Filter Tabs for Configured Providers (All / Local Models / Cloud Providers)
        item {
            Column {
                Text(
                    text = "INTEGRATED AI MODELS & PROVIDERS (${filteredProviders.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "All (${providers.size})",
                        "Local / Ollama (${providers.count { it.isLocalOffline }})",
                        "Cloud APIs (${providers.count { !it.isLocalOffline }})"
                    )
                    tabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedFilterTab == idx,
                            onClick = { selectedFilterTab = idx },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("provider_filter_tab_$idx")
                        )
                    }
                }
            }
        }

        // 4. Configured Providers List with Quick Model Switcher, Edit, Ping, and Live Test
        items(filteredProviders, key = { it.id }) { item ->
            val quickModels = remember(item.providerName, item.baseUrl) {
                AiRouterEngine.curatedModelsForProvider(item.providerName, item.baseUrl)
            }
            val hasStoredKey = item.encryptedApiKey.isNotBlank()

            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (item.isActive) 1.5.dp else 1.dp,
                        color = if (item.isActive) CyberCyan else BorderSubtle,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isLocalOffline) Icons.Default.Memory else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (item.isLocalOffline) QuantumEmerald else CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.providerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = CyberCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Selected Model: ${item.modelName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QuantumEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Endpoint: ${item.baseUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    editingProviderId = item.id
                                    providerName = item.providerName
                                    baseUrl = item.baseUrl
                                    apiKey = viewModel.decryptProviderKeyForEdit(item)
                                    modelName = item.modelName
                                    isLocalProvider = item.isLocalOffline
                                    viewModel.loadCuratedModelsForDraft(item.providerName, item.baseUrl)
                                },
                                modifier = Modifier.testTag("edit_provider_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit provider & API key",
                                    tint = CyberCyan
                                )
                            }
                            if (!item.baseUrl.startsWith("local://")) {
                                IconButton(onClick = { viewModel.deleteProvider(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete provider",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inline Quick Model Selector Chips for this Provider
                    Text(
                        text = "Quick-Switch Model for ${item.providerName}:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickModels.take(5).forEach { candidate ->
                            val isCurrent = candidate.equals(item.modelName, ignoreCase = true)
                            Surface(
                                color = if (isCurrent) QuantumEmerald.copy(alpha = 0.22f) else ObsidianSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = if (isCurrent) QuantumEmerald else BorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                                onClick = { viewModel.switchProviderModel(item, candidate) }
                            ) {
                                Text(
                                    text = candidate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) QuantumEmerald else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = (if (hasStoredKey || item.isLocalOffline) QuantumEmerald else AmberAlert).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when {
                                    item.baseUrl.startsWith("local://") -> "On-Device Local Core"
                                    hasStoredKey && item.lastLatencyMs > 0 -> "AES-256 Key • ${item.lastLatencyMs}ms"
                                    hasStoredKey -> "AES-256 API Key Stored"
                                    else -> "Tap Edit to Set API Key"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasStoredKey || item.isLocalOffline) QuantumEmerald else AmberAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.testProviderConnection(item) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ping", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { viewModel.runProviderLiveTest(item) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("test_prompt_provider_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeuralViolet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test", style = MaterialTheme.typography.labelSmall)
                            }

                            if (!item.isActive) {
                                Button(
                                    onClick = { viewModel.selectActiveProvider(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = Color(0xFF001F24)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
