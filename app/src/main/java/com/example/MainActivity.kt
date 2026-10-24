package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AetherViewModel
import com.example.ui.screens.AiProvidersScreen
import com.example.ui.screens.EcosystemSyncScreen
import com.example.ui.screens.LiveAssistantScreen
import com.example.ui.screens.MacrosAndAgentsScreen
import com.example.ui.screens.OpenEcosystemNexusScreen
import com.example.ui.screens.PermissionsCenterScreen
import com.example.ui.screens.ShellAndGitHubScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.QuantumEmerald

private data class AetherNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AetherAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherAppRoot(
    viewModel: AetherViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilitiesList.collectAsStateWithLifecycle()
    val grantedCount = capabilities.count { it.isGranted }

    LaunchedEffect(statusToast) {
        statusToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusToast()
        }
    }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    val destinations = remember {
        listOf(
            AetherNavDestination("live", "Live", Icons.Default.GraphicEq, "nav_tab_live"),
            AetherNavDestination("models", "Models", Icons.Default.Hub, "nav_tab_models"),
            AetherNavDestination("workflows", "Macros", Icons.Default.AccountTree, "nav_tab_workflows"),
            AetherNavDestination("nexus", "Nexus", Icons.Default.AutoAwesome, "nav_tab_nexus"),
            AetherNavDestination("ecosystem", "Devices", Icons.Default.Devices, "nav_tab_ecosystem"),
            AetherNavDestination("shell_git", "Termux", Icons.Default.Terminal, "nav_tab_shell")
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "AetherOS Agent",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = "OpenMuse • AnyClaw • OpenDots • Termux • Live Vision",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = { selectedTab = 6 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 6) QuantumEmerald else CyberCyan,
                                contentColor = Color(0xFF001F24)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_bar_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions & Accessibility Hub",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Perms ($grantedCount/${capabilities.size})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> LiveAssistantScreen(viewModel = viewModel)
                        1 -> AiProvidersScreen(viewModel = viewModel)
                        2 -> MacrosAndAgentsScreen(viewModel = viewModel)
                        3 -> OpenEcosystemNexusScreen(viewModel = viewModel)
                        4 -> EcosystemSyncScreen(viewModel = viewModel)
                        5 -> ShellAndGitHubScreen(viewModel = viewModel)
                        6 -> PermissionsCenterScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
