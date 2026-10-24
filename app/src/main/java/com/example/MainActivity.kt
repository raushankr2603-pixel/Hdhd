package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AetherViewModel
import com.example.ui.screens.AiProvidersScreen
import com.example.ui.screens.EcosystemSyncScreen
import com.example.ui.screens.LiveAssistantScreen
import com.example.ui.screens.MacrosAndAgentsScreen
import com.example.ui.screens.MuseWorkspaceScreen
import com.example.ui.screens.OpenEcosystemNexusScreen
import com.example.ui.screens.PermissionsCenterScreen
import com.example.ui.screens.ShellAndGitHubScreen
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.QuantumEmerald
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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
    var activeVmId by rememberSaveable { mutableIntStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilitiesList.collectAsStateWithLifecycle()
    val agentComputers by viewModel.agentComputers.collectAsStateWithLifecycle()
    val spacePages by viewModel.openDotsSpacePages.collectAsStateWithLifecycle()
    val grantedCount = capabilities.count { it.isGranted }
    val activeComputer = agentComputers.firstOrNull { it.id == activeVmId } ?: agentComputers.firstOrNull()

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
            AetherNavDestination("muse", "Muse VM", Icons.Default.Computer, "nav_tab_muse"),
            AetherNavDestination("live", "Live", Icons.Default.GraphicEq, "nav_tab_live"),
            AetherNavDestination("models", "Models", Icons.Default.Hub, "nav_tab_models"),
            AetherNavDestination("workflows", "Macros", Icons.Default.AccountTree, "nav_tab_workflows"),
            AetherNavDestination("nexus", "Nexus", Icons.Default.AutoAwesome, "nav_tab_nexus"),
            AetherNavDestination("shell_git", "Termux", Icons.Default.Terminal, "nav_tab_shell")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ObsidianSurface,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Muse Studio Brand Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan.copy(alpha = 0.16f))
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = "Muse Studio",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Muse • AetherOS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${agentComputers.size} Dedicated Agent Cloud Computers",
                                style = MaterialTheme.typography.labelSmall,
                                color = QuantumEmerald
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // Section 1: Active Agent Cloud Computers (GrokBot / MetaMuse style)
                    Text(
                        text = "DEDICATED AGENT COMPUTERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )

                    agentComputers.forEach { vm ->
                        val isSelected = vm.id == activeComputer?.id && selectedTab == 0
                        val statusColor = when (vm.status) {
                            "MANUAL_TAKEOVER" -> AmberAlert
                            "EXECUTING" -> CyberCyan
                            else -> QuantumEmerald
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.14f) else ObsidianCard)
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    activeVmId = vm.id
                                    viewModel.refreshComputerFiles(vm)
                                    selectedTab = 0
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(10.dp)
                                .testTag("drawer_agent_vm_${vm.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vm.agentName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${vm.vmHostname} • ${vm.computeBackend}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${vm.cpuLoadPercent}%/${vm.memoryUsedMb}M",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // Section 2: Recent Deliverable Artifacts / Space Pages
                    Text(
                        text = "RECENT DELIVERABLE ARTIFACTS (${spacePages.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuralViolet
                    )
                    spacePages.take(3).forEach { page ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .clickable {
                                    selectedTab = 0
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.pageTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${page.spaceName} • ${page.approvalStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // Section 3: All AetherOS & Muse Operating Spaces
                    Text(
                        text = "OPERATING SPACES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = QuantumEmerald
                    )

                    destinations.forEachIndexed { index, dest ->
                        NavigationDrawerItem(
                            label = { Text(dest.label) },
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                coroutineScope.launch { drawerState.close() }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.16f),
                                selectedTextColor = CyberCyan,
                                selectedIconColor = CyberCyan,
                                unselectedTextColor = Color.White,
                                unselectedIconColor = TextSecondary
                            )
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("Cross-Device & Smart Home Mesh") },
                        selected = selectedTab == 6,
                        onClick = {
                            selectedTab = 6
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Devices, contentDescription = "Devices") },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.16f),
                            selectedTextColor = CyberCyan,
                            selectedIconColor = CyberCyan,
                            unselectedTextColor = Color.White,
                            unselectedIconColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_ecosystem")
                    )

                    NavigationDrawerItem(
                        label = { Text("Permissions & Accessibility ($grantedCount/${capabilities.size})") },
                        selected = selectedTab == 7,
                        onClick = {
                            selectedTab = 7
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Permissions") },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = QuantumEmerald.copy(alpha = 0.16f),
                            selectedTextColor = QuantumEmerald,
                            selectedIconColor = QuantumEmerald,
                            unselectedTextColor = Color.White,
                            unselectedIconColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) {
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
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("muse_drawer_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Muse Sidebar",
                                    tint = CyberCyan
                                )
                            }
                        },
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Muse • AetherOS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    activeComputer?.let { vm ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberCyan.copy(alpha = 0.15f))
                                                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = vm.vmHostname,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "GrokBot & MetaMuse Agent VMs • AnyClaw • OpenDots • Termux",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuantumEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = { selectedTab = 6 },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("top_bar_devices_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = "Cross-Device Mesh",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mesh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                            Button(
                                onClick = { selectedTab = 7 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTab == 7) QuantumEmerald else CyberCyan,
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
                            0 -> MuseWorkspaceScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { targetTab -> selectedTab = targetTab }
                            )
                            1 -> LiveAssistantScreen(viewModel = viewModel)
                            2 -> AiProvidersScreen(viewModel = viewModel)
                            3 -> MacrosAndAgentsScreen(viewModel = viewModel)
                            4 -> OpenEcosystemNexusScreen(viewModel = viewModel)
                            5 -> ShellAndGitHubScreen(viewModel = viewModel)
                            6 -> EcosystemSyncScreen(viewModel = viewModel)
                            7 -> PermissionsCenterScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
