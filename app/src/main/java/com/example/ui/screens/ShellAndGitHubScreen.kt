package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AetherViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary

@Composable
fun ShellAndGitHubScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val shellHistory by viewModel.shellHistory.collectAsStateWithLifecycle()
    val githubRepos by viewModel.githubRepos.collectAsStateWithLifecycle()
    val termuxEnv by viewModel.termuxEnvironment.collectAsStateWithLifecycle()
    val lastTermuxOutcome by viewModel.lastTermuxOutcome.collectAsStateWithLifecycle()

    var shellCommand by remember { mutableStateOf("") }
    var executeAsRoot by remember { mutableStateOf(true) }

    var repoInput by remember { mutableStateOf("") }
    var repoHook by remember { mutableStateOf("Auto-Pull, Build & Agent Workflow Trigger") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Real-Time Deep System Diagnostics & Optimizer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DEEP SYSTEM TELEMETRY & DIAGNOSTICS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Text(
                                    text = diagnostics.cpuModelAndCores,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.refreshDiagnostics() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Refresh", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "RAM Utilization: ${diagnostics.ramUsagePercent}% (${diagnostics.availableRamMb} MB free / ${diagnostics.totalRamMb} MB total)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (diagnostics.ramUsagePercent / 100f).coerceIn(0f, 1f) },
                        color = if (diagnostics.ramUsagePercent > 85) AmberAlert else QuantumEmerald,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Storage Free: ${diagnostics.storageFreeGb} / ${diagnostics.storageTotalGb}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Battery: ${diagnostics.batteryPercent}% (${diagnostics.batteryTempCelsius}°C)",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kernel: ${diagnostics.kernelVersion} • Uptime: ${diagnostics.uptimeMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.runDeepSystemOptimization() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deep_system_optimize_button")
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Deep System Optimization & Memory Trim")
                    }
                }
            }
        }

        // 2. Interactive Shell & Root (su -c) Script Console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = QuantumEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AETHER ROOT / SH AUTOMATION SHELL",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                            Text(
                                text = "Execute System Commands & Custom Automation Scripts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickCommands = listOf(
                            "uname -a",
                            "cat /proc/meminfo | head -n 6",
                            "df -h",
                            "ps -A | head -n 12",
                            "getprop ro.product.model",
                            "id"
                        )
                        quickCommands.forEach { cmd ->
                            AssistChip(
                                onClick = {
                                    shellCommand = cmd
                                    viewModel.runShellCommand(cmd, executeAsRoot)
                                },
                                label = { Text(cmd) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = shellCommand,
                        onValueChange = { shellCommand = it },
                        label = { Text("Shell / Root Script Command (e.g. top -n 1 | head -n 10)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shell_command_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = executeAsRoot,
                            onCheckedChange = { executeAsRoot = it }
                        )
                        Text(
                            text = "Elevate with Root (`su -c`) Shell Privileges (Fallback to `sh` if unrooted)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.runShellCommand(shellCommand, executeAsRoot)
                            shellCommand = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_shell_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Shell Command")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    shellHistory.take(4).forEach { entry ->
                        Surface(
                            color = ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${if (entry.isRootAttempt) "root@aether:#" else "shell@aether:$"} ${entry.command}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.output,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. GitHub Repository Integration & Workflow Manager
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GITHUB REPOSITORY WORKFLOW INTEGRATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "Add & Sync Any GitHub Repository into Agent Workflows",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presetRepos = listOf(
                            "CopilotKit/OpenMuse",
                            "openclaw/openclaw",
                            "fastclaw-ai/anyclaw",
                            "CopilotKit/OpenDots",
                            "termux/termux-app",
                            "termux/termux-api",
                            "ollama/ollama"
                        )
                        presetRepos.forEach { slug ->
                            AssistChip(
                                onClick = {
                                    repoInput = slug
                                    viewModel.addGitHubRepository(slug, repoHook)
                                },
                                label = { Text(slug, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("GitHub Repository (e.g. CopilotKit/OpenMuse or openclaw/openclaw)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = repoHook,
                        onValueChange = { repoHook = it },
                        label = { Text("Agent Automation Hook / Script Pipeline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.addGitHubRepository(repoInput, repoHook)
                            repoInput = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_github_repo_button")
                    ) {
                        Text("Connect & Sync GitHub Repository")
                    }
                }
            }
        }

        items(githubRepos, key = { it.id }) { repo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.ownerRepo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = repo.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteGitHubRepo(repo.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Repo",
                                tint = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Branch: ${repo.defaultBranch} • ${repo.language} • ★ ${repo.starsCount} • Issues: ${repo.openIssuesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = QuantumEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Workflow Hook: ${repo.automationHook}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 4. Full Termux & Sub-Dependencies Console
        item {
            TermuxAndSubDependenciesSection(
                viewModel = viewModel,
                termuxEnv = termuxEnv,
                lastTermuxOutcome = lastTermuxOutcome
            )
        }
    }
}

