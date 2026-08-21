package pl.dlaflow.mobile

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.dlaflow.mobile.app.navigation.MobileAssistantBackAction
import pl.dlaflow.mobile.app.navigation.MobileAssistantOverlayScreen
import pl.dlaflow.mobile.app.navigation.MobileAssistantTab
import pl.dlaflow.mobile.app.navigation.MobileRoute
import pl.dlaflow.mobile.app.navigation.mobileAssistantBackAction
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowFilterChip
import pl.dlaflow.mobile.core.designsystem.DlaFlowHeaderIconButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowKeyValue
import pl.dlaflow.mobile.core.designsystem.DlaFlowKpiTile
import pl.dlaflow.mobile.core.designsystem.DlaFlowMetricBox
import pl.dlaflow.mobile.core.designsystem.DlaFlowNotificationEmptyRow
import pl.dlaflow.mobile.core.designsystem.DlaFlowNotificationPreviewCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowNotificationRow
import pl.dlaflow.mobile.core.designsystem.DlaFlowPhotoTaskCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowPrimaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowSearchField
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusBadge
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusStrip
import pl.dlaflow.mobile.core.designsystem.DlaFlowTextField
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.feature.dashboard.DashboardAction
import pl.dlaflow.mobile.feature.dashboard.DashboardContent
import pl.dlaflow.mobile.feature.dashboard.DashboardFeatureScreen
import pl.dlaflow.mobile.feature.dashboard.DashboardKpis
import pl.dlaflow.mobile.feature.dashboard.DashboardNotification
import pl.dlaflow.mobile.feature.dashboard.DashboardPhotoTask
import pl.dlaflow.mobile.feature.dashboard.DashboardUiState
import pl.dlaflow.mobile.feature.dashboard.contentOrNull
import pl.dlaflow.mobile.feature.orders.OrdersAction
import pl.dlaflow.mobile.app.navigation.MobileKpiDestination
import pl.dlaflow.mobile.feature.orders.OrdersFeatureScreen
import pl.dlaflow.mobile.feature.orders.OrdersPackageScannerStrip
import pl.dlaflow.mobile.feature.orders.OrdersPackageScannerState
import pl.dlaflow.mobile.feature.orders.OrdersRoute
import pl.dlaflow.mobile.feature.orders.OrdersUiState
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiState
import pl.dlaflow.mobile.feature.pairing.PairingFeatureScreen
import pl.dlaflow.mobile.feature.pairing.PairingStep
import pl.dlaflow.mobile.feature.pairing.PairingUiState
import pl.dlaflow.mobile.feature.scanner.ScannerMatchKind
import pl.dlaflow.mobile.feature.scanner.ScannerUiState
import pl.dlaflow.mobile.feature.settings.SettingsAction
import pl.dlaflow.mobile.feature.settings.SettingsContent
import pl.dlaflow.mobile.feature.settings.SettingsFeatureScreen
import pl.dlaflow.mobile.feature.settings.SettingsRoute
import pl.dlaflow.mobile.feature.settings.SettingsUiState

enum class MobileNotificationFilter(val label: String) {
    ALL("Wszystkie"),
    ATTENTION("Wymaga uwagi"),
    UNREAD("Nowe"),
}

data class PackageScannerResolvedCopy(
    val title: String,
    val supportingText: String,
)

internal fun shouldShowPackageScannerHeaderAction(
    selectedTab: MobileAssistantTab,
    overlayScreen: MobileAssistantOverlayScreen,
): Boolean = selectedTab == MobileAssistantTab.ORDERS && overlayScreen == MobileAssistantOverlayScreen.NONE

internal fun packageScannerResolvedCopy(result: pl.dlaflow.mobile.feature.scanner.ScannerLookupResult): PackageScannerResolvedCopy {
    if (result.kind != ScannerMatchKind.NO_MATCH && result.order != null) {
        return if (result.kind == ScannerMatchKind.AMBIGUOUS) {
            PackageScannerResolvedCopy(
                title = "Znaleziono kilka możliwych paczek",
                supportingText = "Pokazujemy najnowsze pasujące zamówienie. Sprawdź dane przed dalszą obsługą.",
            )
        } else {
            PackageScannerResolvedCopy(
                title = "Paczka znaleziona",
                supportingText = result.order.customer,
            )
        }
    }

    return PackageScannerResolvedCopy(
        title = "Nie znaleziono paczki",
        supportingText = "Ten kod nie pasuje do żadnej paczki w DlaFlow.",
    )
}
internal fun ScannerUiState.toOrdersPackageScannerState(): OrdersPackageScannerState = when (val lookup = lookupState) {
    DlaFlowUiState.Empty -> OrdersPackageScannerState.Empty
    DlaFlowUiState.Loading -> OrdersPackageScannerState.Loading
    is DlaFlowUiState.Error -> OrdersPackageScannerState.Failed("Nie udało się sprawdzić paczki.")
    is DlaFlowUiState.Offline -> OrdersPackageScannerState.Failed("Sprawdź internet i spróbuj ponownie.")
    DlaFlowUiState.NoAccess -> OrdersPackageScannerState.Failed("Twoje konto nie ma uprawnień do tej operacji.")
    is DlaFlowUiState.Content -> {
        val result = lookup.data
        val copy = packageScannerResolvedCopy(result)
        val order = result.order.takeIf { result.kind != ScannerMatchKind.NO_MATCH }
        OrdersPackageScannerState.Resolved(
            title = copy.title,
            supportingText = copy.supportingText,
            orderStatus = order?.let { "#${it.orderNumber} · ${it.status}" },
            orderNumber = order?.orderNumber,
            retryable = order == null,
        )
    }
}

fun filterNotifications(
    notifications: List<MobileAssistantNotification>,
    filter: MobileNotificationFilter,
): List<MobileAssistantNotification> = when (filter) {
    MobileNotificationFilter.ALL -> notifications
    MobileNotificationFilter.ATTENTION -> notifications.filter { toneColorKey(it.tone) == "attention" }
    MobileNotificationFilter.UNREAD -> notifications.filter { it.readAt.isNullOrBlank() }
}

private fun filterDashboardNotifications(
    notifications: List<DashboardNotification>,
    filter: MobileNotificationFilter,
): List<DashboardNotification> = when (filter) {
    MobileNotificationFilter.ALL -> notifications
    MobileNotificationFilter.ATTENTION -> notifications.filter { toneColorKey(it.tone) == "attention" }
    MobileNotificationFilter.UNREAD -> notifications.filter { it.readAt.isNullOrBlank() }
}

private fun MobileAssistantNotification.toDashboardNotification() = DashboardNotification(
    id = id,
    title = title,
    description = description,
    tone = tone,
    source = source,
    account = account,
    occurredAt = occurredAt,
    readAt = readAt,
    actionType = mobileAction.type,
    actionLabel = mobileAction.label,
)

private fun toneColorKey(tone: String): String {
    val normalized = tone.lowercase(Locale.ROOT)

    return if (normalized == "error" || normalized == "warning") "attention" else normalized
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileAssistantScreen(
    session: MobileSession?,
    dashboardState: DashboardUiState,
    photoTasks: List<MobilePhotoTask>,
    scannerState: ScannerUiState,
    statusMessage: String,
    selectedTab: MobileAssistantTab,
    apiUrl: String,
    pairingState: PairingUiState,
    settingsState: SettingsUiState,
    settingsContent: SettingsContent,
    appVersionName: String,
    appUpdate: MobileAppUpdate? = null,
    appUpdateDialogVisible: Boolean = false,
    appUpdateBlocking: Boolean = false,
    appUpdateDismissalsRemaining: Int = 0,
    appUpdateChecking: Boolean = false,
    appUpdateDownloading: Boolean = false,
    appUpdateDownloadProgress: Int = 0,
    appUpdateError: String = "",
    ordersState: OrdersUiState = OrdersUiState(),
    mobileProducts: List<MobileProduct> = emptyList(),
    mobileProductsNextCursor: String? = null,
    mobileProductsTotal: Int = 0,
    mobileProductsLoading: Boolean = false,
    mobileProductsSearch: String = "",
    mobileProductsFilter: MobileProductFilter = MobileProductFilter.ALL,
    mobileProductVariants: Map<String, List<MobileProductVariant>> = emptyMap(),
    mobileProductVariantsLoading: Set<String> = emptySet(),
    mobileProductsReadOnly: Boolean = false,
    mobileProductsNoAccess: Boolean = false,
    mobileOverlayScreen: MobileAssistantOverlayScreen = MobileAssistantOverlayScreen.NONE,
    mobileNotifications: List<MobileAssistantNotification> = emptyList(),
    mobileNotificationsLoading: Boolean = false,
    mobileNotificationFilter: MobileNotificationFilter = MobileNotificationFilter.ALL,
    onPairingCodeChange: (String) -> Unit,
    onContinuePairing: () -> Unit,
    onScanPairingQr: () -> Unit,
    onPairingDeviceNameChange: (String) -> Unit,
    onSubmitPairing: () -> Unit,
    onShowPairingHelp: () -> Unit,
    onPairingBack: () -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    onDashboardAction: (DashboardAction) -> Unit,
    onRefreshCurrentTab: () -> Unit = {},
    onSelectTab: (MobileAssistantTab) -> Unit,
    onOrdersAction: (OrdersAction) -> Unit = {},
    onProductsSearchChange: (String) -> Unit = {},
    onProductsFilterChange: (MobileProductFilter) -> Unit = {},
    onLoadMoreProducts: () -> Unit = {},
    onToggleProductVariants: (String) -> Unit = {},
    onQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit = { _, _, _ -> },
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit = { _, _, _ -> },
    onCloseOverlay: () -> Unit = {},
    onNotificationFilterChange: (MobileNotificationFilter) -> Unit = {},
    onMarkNotificationsRead: () -> Unit = {},
    onInstallAppUpdate: () -> Unit,
    onDismissAppUpdate: () -> Unit,
) {
    val dashboard = dashboardState.contentOrNull()
    val dark = isSystemInDarkTheme()
    val route = if (session == null) {
        MobileRoute.Pairing(
            helpVisible = pairingState.step == PairingStep.HELP,
            nameVisible = pairingState.step == PairingStep.NAME,
        )
    } else {
        MobileRoute.Assistant(
            selectedTab = selectedTab,
            overlayScreen = mobileOverlayScreen,
            orderDetailVisible = ordersState.route is OrdersRoute.Detail,
            settingsDetailVisible = settingsState.route is SettingsRoute.Detail,
        )
    }
    val backAction = mobileAssistantBackAction(route)

    DlaFlowTheme(dark = dark) { colors ->
        BackHandler(enabled = backAction != MobileAssistantBackAction.NONE) {
            when (backAction) {
                MobileAssistantBackAction.CLOSE_PAIRING_HELP,
                MobileAssistantBackAction.CLOSE_PAIRING_NAME,
                -> onPairingBack()
                MobileAssistantBackAction.CLOSE_ORDER_DETAIL -> onOrdersAction(OrdersAction.CloseDetail)
                MobileAssistantBackAction.CLOSE_SETTINGS_DETAIL -> onSettingsAction(SettingsAction.Back)
                MobileAssistantBackAction.CLOSE_OVERLAY -> onCloseOverlay()
                MobileAssistantBackAction.NONE -> Unit
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            containerColor = colors.appBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (session != null) {
                    BottomNavigation(colors, selectedTab, dashboard, onSelectTab)
                }
            },
        ) { padding ->
            Surface(
                color = colors.appBg,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (session == null) {
                    PairingFeatureScreen(
                        colors = colors,
                        state = pairingState,
                        appVersionName = appVersionName,
                        externalStatusMessage = statusMessage,
                        onCodeChange = onPairingCodeChange,
                        onContinue = onContinuePairing,
                        onScanQr = onScanPairingQr,
                        onDeviceNameChange = onPairingDeviceNameChange,
                        onSubmit = onSubmitPairing,
                        onShowHelp = onShowPairingHelp,
                        onBack = onPairingBack,
                    )
                } else {
                AssistantContent(
                    colors = colors,
                    apiUrl = apiUrl,
                    session = session,
                    dashboardState = dashboardState,
                    dashboard = dashboard,
                    photoTasks = photoTasks,
                    scannerState = scannerState,
                    statusMessage = statusMessage,
                    selectedTab = selectedTab,
                    mobileProducts = mobileProducts,
                    mobileProductsNextCursor = mobileProductsNextCursor,
                    mobileProductsTotal = mobileProductsTotal,
                    mobileProductsLoading = mobileProductsLoading,
                    mobileProductsSearch = mobileProductsSearch,
                    mobileProductsFilter = mobileProductsFilter,
                    mobileProductVariants = mobileProductVariants,
                    mobileProductVariantsLoading = mobileProductVariantsLoading,
                    mobileProductsReadOnly = mobileProductsReadOnly,
                    mobileProductsNoAccess = mobileProductsNoAccess,
                    mobileOverlayScreen = mobileOverlayScreen,
                    mobileNotifications = mobileNotifications,
                    mobileNotificationsLoading = mobileNotificationsLoading,
                    mobileNotificationFilter = mobileNotificationFilter,
                    settingsState = settingsState,
                    settingsContent = settingsContent,
                    ordersState = ordersState,
                    onSettingsAction = onSettingsAction,
                    onDashboardAction = onDashboardAction,
                    onRefreshCurrentTab = onRefreshCurrentTab,
                    onOrdersAction = onOrdersAction,
                    onProductsSearchChange = onProductsSearchChange,
                    onProductsFilterChange = onProductsFilterChange,
                    onLoadMoreProducts = onLoadMoreProducts,
                    onToggleProductVariants = onToggleProductVariants,
                    onQuickEditProduct = onQuickEditProduct,
                    onQuickEditVariant = onQuickEditVariant,
                    onCloseOverlay = onCloseOverlay,
                    onNotificationFilterChange = onNotificationFilterChange,
                    onMarkNotificationsRead = onMarkNotificationsRead,
                    )
                }
            }
            }
            if (session != null) {
                RefreshPopupOverlay(
                    colors = colors,
                    visible = shouldShowRefreshOverlay(
                        selectedTab = selectedTab,
                        overlayScreen = mobileOverlayScreen,
                        dashboardState = dashboardState,
                        ordersState = ordersState,
                        mobileProductsLoading = mobileProductsLoading,
                        mobileNotificationsLoading = mobileNotificationsLoading,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp)
                        .zIndex(10f),
                )
            }
        }
        if (session != null && appUpdateDialogVisible && appUpdate != null) {
            MobileAppUpdateDialog(
                colors = colors,
                update = appUpdate,
                blocking = appUpdateBlocking,
                dismissalsRemaining = appUpdateDismissalsRemaining,
                downloading = appUpdateDownloading,
                downloadProgress = appUpdateDownloadProgress,
                error = appUpdateError,
                onInstall = onInstallAppUpdate,
                onDismiss = onDismissAppUpdate,
            )
        }
    }
}

private fun shouldShowRefreshOverlay(
    selectedTab: MobileAssistantTab,
    overlayScreen: MobileAssistantOverlayScreen,
    dashboardState: DashboardUiState,
    ordersState: OrdersUiState,
    mobileProductsLoading: Boolean,
    mobileNotificationsLoading: Boolean,
): Boolean = when (selectedTab) {
    MobileAssistantTab.DASHBOARD,
    MobileAssistantTab.MESSAGES,
    MobileAssistantTab.MORE,
    -> dashboardState.isRefreshing
    MobileAssistantTab.ORDERS -> ordersState.isRefreshing
    MobileAssistantTab.PRODUCTS -> mobileProductsLoading
}.let { tabRefreshing ->
    if (overlayScreen == MobileAssistantOverlayScreen.NOTIFICATIONS) mobileNotificationsLoading else tabRefreshing
}

@Composable
private fun RefreshPopupOverlay(
    colors: DlaFlowComposeColors,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, colors.primarySoftBorder),
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_refreshing_title),
                        color = colors.textStrong,
                        fontFamily = DlaFlowInter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_refreshing_description),
                        color = colors.textMuted,
                        fontFamily = DlaFlowInter,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
private fun AssistantContent(
    colors: DlaFlowComposeColors,
    apiUrl: String,
    session: MobileSession,
    dashboardState: DashboardUiState,
    dashboard: DashboardContent?,
    photoTasks: List<MobilePhotoTask>,
    scannerState: ScannerUiState,
    statusMessage: String,
    selectedTab: MobileAssistantTab,
    mobileProducts: List<MobileProduct>,
    mobileProductsNextCursor: String?,
    mobileProductsTotal: Int,
    mobileProductsLoading: Boolean,
    mobileProductsSearch: String,
    mobileProductsFilter: MobileProductFilter,
    mobileProductVariants: Map<String, List<MobileProductVariant>>,
    mobileProductVariantsLoading: Set<String>,
    mobileProductsReadOnly: Boolean,
    mobileProductsNoAccess: Boolean,
    mobileOverlayScreen: MobileAssistantOverlayScreen,
    mobileNotifications: List<MobileAssistantNotification>,
    mobileNotificationsLoading: Boolean,
    mobileNotificationFilter: MobileNotificationFilter,
    settingsState: SettingsUiState,
    settingsContent: SettingsContent,
    ordersState: OrdersUiState,
    onSettingsAction: (SettingsAction) -> Unit,
    onDashboardAction: (DashboardAction) -> Unit,
    onOrdersAction: (OrdersAction) -> Unit,
    onRefreshCurrentTab: () -> Unit,
    onProductsSearchChange: (String) -> Unit,
    onProductsFilterChange: (MobileProductFilter) -> Unit,
    onLoadMoreProducts: () -> Unit,
    onToggleProductVariants: (String) -> Unit,
    onQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
    onCloseOverlay: () -> Unit,
    onNotificationFilterChange: (MobileNotificationFilter) -> Unit,
    onMarkNotificationsRead: () -> Unit,
) {
    val mobileMediaClient = remember(apiUrl, session.deviceId) {
        mobileApiClientForDevice(apiUrl, session.deviceId)
    }
    val thumbnailLoader = remember(mobileMediaClient, session.token) {
        DlaFlowThumbnailLoader { url ->
            loadMobileImageBitmap(mobileMediaClient, url, session.token)?.asImageBitmap()
        }
    }

    val isRefreshing = when {
        mobileOverlayScreen == MobileAssistantOverlayScreen.NOTIFICATIONS -> mobileNotificationsLoading
        selectedTab == MobileAssistantTab.DASHBOARD || selectedTab == MobileAssistantTab.MESSAGES -> dashboardState.isRefreshing
        selectedTab == MobileAssistantTab.ORDERS -> ordersState.isRefreshing
        selectedTab == MobileAssistantTab.PRODUCTS -> mobileProductsLoading
        else -> false
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshCurrentTab,
        state = rememberPullToRefreshState(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppHeader(
                colors = colors,
                status = "Połączono",
                unreadCount = dashboard?.notificationSummary?.unreadCount ?: 0,
                unreadAttentionCount = dashboard?.notificationSummary?.unreadAttentionCount ?: 0,
                onScanPackage = if (shouldShowPackageScannerHeaderAction(selectedTab, mobileOverlayScreen)) {
                    { onDashboardAction(DashboardAction.ScanPackage) }
                } else {
                    null
                },
                onOpenNotifications = { onDashboardAction(DashboardAction.OpenNotifications) },
            )
            if (mobileOverlayScreen == MobileAssistantOverlayScreen.NOTIFICATIONS) {
                NotificationsScreen(
                    colors = colors,
                    notifications = if (mobileNotifications.isNotEmpty()) {
                        mobileNotifications.map { it.toDashboardNotification() }
                    } else {
                        dashboard?.notifications.orEmpty()
                    },
                    loading = mobileNotificationsLoading,
                    selectedFilter = mobileNotificationFilter,
                    onFilterChange = onNotificationFilterChange,
                    onBack = onCloseOverlay,
                    onMarkRead = onMarkNotificationsRead,
                )
            } else {
                when (selectedTab) {
                    MobileAssistantTab.DASHBOARD -> DashboardFeatureScreen(
                        colors = colors,
                        sessionUserName = session.userEmail,
                        state = dashboardState,
                        fallbackPhotoTask = photoTasks.firstOrNull()?.let { task ->
                            DashboardPhotoTask(
                                id = task.id,
                                productName = task.productName,
                                productSku = task.productSku,
                                productImage = "",
                                status = task.status,
                                mediaCount = task.mediaCount,
                                maxPhotos = task.maxPhotos,
                                expiresAt = task.expiresAt,
                            )
                        },
                        onAction = onDashboardAction,
                    )
                    MobileAssistantTab.ORDERS -> OrdersFeatureScreen(
                        colors = colors,
                        state = ordersState,
                        thumbnailLoader = thumbnailLoader,
                        leadContent = {
                            LegacyKpiGrid(colors, dashboard?.kpis) { destination ->
                                onDashboardAction(DashboardAction.OpenOrdersFilter(destination))
                            }
                            OrdersPackageScannerStrip(
                                colors = colors,
                                scanState = scannerState.toOrdersPackageScannerState(),
                                onOpenOrder = { onOrdersAction(OrdersAction.OpenOrder(it)) },
                                onScanAgain = { onDashboardAction(DashboardAction.ScanPackage) },
                            )
                        },
                        onAction = onOrdersAction,
                    )
                    MobileAssistantTab.PRODUCTS -> ProductsTab(
                        colors = colors,
                        mobileMediaClient = mobileMediaClient,
                        mobileToken = session.token,
                        dashboard = dashboard,
                        photoTasks = photoTasks,
                        mobileProducts = mobileProducts,
                        mobileProductsNextCursor = mobileProductsNextCursor,
                        mobileProductsTotal = mobileProductsTotal,
                        mobileProductsLoading = mobileProductsLoading,
                        mobileProductsSearch = mobileProductsSearch,
                        mobileProductsFilter = mobileProductsFilter,
                        mobileProductVariants = mobileProductVariants,
                        mobileProductVariantsLoading = mobileProductVariantsLoading,
                        mobileProductsReadOnly = mobileProductsReadOnly,
                        mobileProductsNoAccess = mobileProductsNoAccess,
                        onRefresh = { onDashboardAction(DashboardAction.Refresh) },
                        onProductsSearchChange = onProductsSearchChange,
                        onProductsFilterChange = onProductsFilterChange,
                        onLoadMoreProducts = onLoadMoreProducts,
                        onToggleProductVariants = onToggleProductVariants,
                        onQuickEditProduct = onQuickEditProduct,
                        onQuickEditVariant = onQuickEditVariant,
                        onTakePhoto = { taskId -> onDashboardAction(DashboardAction.TakePhoto(taskId)) },
                        onPickPhoto = { taskId -> onDashboardAction(DashboardAction.PickPhoto(taskId)) },
                        onCompletePhotoTask = { taskId -> onDashboardAction(DashboardAction.CompletePhotoTask(taskId)) },
                    )
                    MobileAssistantTab.MESSAGES -> MessagesTab(
                        colors = colors,
                        dashboard = dashboard,
                        onOpenNotifications = { onDashboardAction(DashboardAction.OpenNotifications) },
                    )
                    MobileAssistantTab.MORE -> SettingsFeatureScreen(
                        colors = colors,
                        state = settingsState,
                        content = settingsContent,
                        onAction = onSettingsAction,
                    )
                }
            }
            if (shouldShowAssistantStatus(statusMessage)) {
                DlaFlowStatusStrip(colors, statusMessage)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun shouldShowAssistantStatus(message: String): Boolean {
    val normalized = message.trim()
    if (normalized.isBlank()) return false

    return normalized !in setOf(
        "Brak aktywnych zadań.",
        "Telefon działa normalnie.",
        "Połączono",
    )
}

@Composable
private fun ProductsTab(
    colors: DlaFlowComposeColors,
    mobileMediaClient: MobileApiClient,
    mobileToken: String,
    dashboard: DashboardContent?,
    photoTasks: List<MobilePhotoTask>,
    mobileProducts: List<MobileProduct>,
    mobileProductsNextCursor: String?,
    mobileProductsTotal: Int,
    mobileProductsLoading: Boolean,
    mobileProductsSearch: String,
    mobileProductsFilter: MobileProductFilter,
    mobileProductVariants: Map<String, List<MobileProductVariant>>,
    mobileProductVariantsLoading: Set<String>,
    mobileProductsReadOnly: Boolean,
    mobileProductsNoAccess: Boolean,
    onRefresh: () -> Unit,
    onProductsSearchChange: (String) -> Unit,
    onProductsFilterChange: (MobileProductFilter) -> Unit,
    onLoadMoreProducts: () -> Unit,
    onToggleProductVariants: (String) -> Unit,
    onQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
    onTakePhoto: (String) -> Unit,
    onPickPhoto: (String) -> Unit,
    onCompletePhotoTask: (String) -> Unit,
) {
    val dashboardTask = dashboard?.activePhotoTask?.toMobilePhotoTask()
    val visibleTasks = if (photoTasks.isNotEmpty()) photoTasks else listOfNotNull(dashboardTask)
    var quickEdit by remember { mutableStateOf<ProductQuickEditTarget?>(null) }

    if (visibleTasks.isEmpty()) {
        ProductPhotoTaskMicroNotice(colors, onRefresh)
    } else {
        SectionTitle(colors, "Zdjęcia z telefonu", "Zadania wysłane z panelu")
        visibleTasks.forEachIndexed { index, task ->
            AssistantPhotoTaskCard(colors, task, index == 0, onTakePhoto, onPickPhoto, onCompletePhotoTask)
        }
    }

    SectionTitle(colors, "Produkty", productsSummary(mobileProductsTotal, mobileProducts.size, mobileProductsLoading, mobileProductsReadOnly, mobileProductsNoAccess))
    ProductSearchField(
        colors = colors,
        value = mobileProductsSearch,
        onValueChange = onProductsSearchChange,
    )
    ProductFilterChips(
        colors = colors,
        selected = mobileProductsFilter,
        onFilterChange = onProductsFilterChange,
    )
    if (mobileProductsNoAccess) {
        ProductsNoAccessNotice(colors)
    } else if (mobileProductsReadOnly) {
        ProductsReadOnlyNotice(colors)
    }
    when {
        mobileProductsLoading && mobileProducts.isEmpty() -> ProductListSkeleton(colors)
        mobileProducts.isEmpty() -> ProductStateCard(
            colors = colors,
            icon = Icons.Rounded.Search,
            iconColor = colors.textMuted,
            title = "Brak produktów",
            description = "Zmień wyszukiwanie lub filtr, a potem odśwież listę.",
        )
        else -> {
            mobileProducts.forEach { product ->
                MobileProductCard(
                    colors = colors,
                    mobileMediaClient = mobileMediaClient,
                    mobileToken = mobileToken,
                    product = product,
                    variants = mobileProductVariants[product.id],
                    variantsLoading = product.id in mobileProductVariantsLoading,
                    readOnly = mobileProductsReadOnly || mobileProductsNoAccess,
                    quickEdit = quickEdit,
                    onToggleVariants = { onToggleProductVariants(product.id) },
                    onQuickEditProduct = { field ->
                        quickEdit = ProductQuickEditTarget.Product(product, field)
                    },
                    onQuickEditVariant = { variant, field ->
                        quickEdit = ProductQuickEditTarget.Variant(variant, field)
                    },
                    onCancelQuickEdit = { quickEdit = null },
                    onSaveQuickEditProduct = { editedProduct, field, value ->
                        onQuickEditProduct(editedProduct, field, value)
                        quickEdit = null
                    },
                    onSaveQuickEditVariant = { variant, field, value ->
                        onQuickEditVariant(variant, field, value)
                        quickEdit = null
                    },
                )
            }
        }
    }
    if (mobileProductsLoading && mobileProducts.isNotEmpty()) {
        ProductStateCard(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            iconColor = colors.primary,
            title = "Odświeżam listę",
            description = "Możesz dalej przeglądać widoczne produkty.",
        )
    }
    if (mobileProductsNextCursor != null && mobileProducts.isNotEmpty()) {
        DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            text = if (mobileProductsLoading) "Pobieram..." else "Pokaż więcej",
            onClick = {
                if (!mobileProductsLoading) {
                    onLoadMoreProducts()
                }
            },
        )
    }
}

@Composable
private fun ProductSearchField(
    colors: DlaFlowComposeColors,
    value: String,
    onValueChange: (String) -> Unit,
) {
    DlaFlowSearchField(
        colors = colors,
        value = value,
        placeholder = "Szukaj po nazwie, SKU lub EAN",
        onValueChange = onValueChange,
    )
}

@Composable
private fun ProductFilterChips(
    colors: DlaFlowComposeColors,
    selected: MobileProductFilter,
    onFilterChange: (MobileProductFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MobileProductFilter.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { filter ->
                    ProductFilterChip(
                        colors = colors,
                        label = filter.label,
                        selected = filter == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onFilterChange(filter) },
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProductFilterChip(
    colors: DlaFlowComposeColors,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    DlaFlowFilterChip(colors, label, selected, modifier, onClick)
}

@Composable
private fun ProductListSkeleton(colors: DlaFlowComposeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(3) { index ->
            ProductSkeletonCard(colors, compact = index > 0)
        }
    }
}

@Composable
private fun ProductSkeletonCard(colors: DlaFlowComposeColors, compact: Boolean) {
    DlaFlowCard(colors) {
        Row(verticalAlignment = Alignment.Top) {
            ProductSkeletonBlock(colors, Modifier.size(38.dp), radius = 8.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                ProductSkeletonBlock(colors, Modifier.fillMaxWidth(if (compact) 0.72f else 0.9f).height(16.dp))
                Spacer(Modifier.height(7.dp))
                ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.46f).height(10.dp))
                if (!compact) {
                    Spacer(Modifier.height(7.dp))
                    ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.78f).height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProductSkeletonBlock(colors, Modifier.weight(1f).height(56.dp))
            ProductSkeletonBlock(colors, Modifier.weight(1f).height(56.dp))
        }
        if (!compact) {
            Spacer(Modifier.height(8.dp))
            ProductSkeletonBlock(colors, Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

@Composable
private fun ProductVariantSkeleton(colors: DlaFlowComposeColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ProductSkeletonBlock(colors, Modifier.size(38.dp), radius = 8.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.68f).height(13.dp))
                Spacer(Modifier.height(6.dp))
                ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.42f).height(9.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProductSkeletonBlock(colors, Modifier.weight(1f).height(48.dp))
            ProductSkeletonBlock(colors, Modifier.weight(1f).height(48.dp))
        }
    }
}

@Composable
private fun ProductSkeletonBlock(colors: DlaFlowComposeColors, modifier: Modifier, radius: Dp = 8.dp) {
    DlaFlowSkeletonBlock(colors, modifier, radius)
}

@Composable
private fun ProductsReadOnlyNotice(colors: DlaFlowComposeColors) {
    ProductStateCard(
        colors = colors,
        icon = Icons.Rounded.Warning,
        iconColor = colors.orange,
        title = "Tylko podgląd",
        description = "To konto może sprawdzać produkty, ale nie może zmieniać cen ani stanów.",
    )
}

@Composable
private fun ProductsNoAccessNotice(colors: DlaFlowComposeColors) {
    ProductStateCard(
        colors = colors,
        icon = Icons.Rounded.Warning,
        iconColor = colors.danger,
        title = "Brak dostępu",
        description = "To konto nie ma dostępu do listy produktów w telefonie.",
    )
}

@Composable
private fun ProductStateCard(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
) {
    DlaFlowStateCard(colors, icon, iconColor, title, description)
}

@Composable
private fun MobileProductCard(
    colors: DlaFlowComposeColors,
    mobileMediaClient: MobileApiClient,
    mobileToken: String,
    product: MobileProduct,
    variants: List<MobileProductVariant>?,
    variantsLoading: Boolean,
    readOnly: Boolean,
    quickEdit: ProductQuickEditTarget?,
    onToggleVariants: () -> Unit,
    onQuickEditProduct: (MobileProductQuickEditField) -> Unit,
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField) -> Unit,
    onCancelQuickEdit: () -> Unit,
    onSaveQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onSaveQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
) {
    val status = productStatusLabel(product.status, product.lowStock)
    val stockDecision = canQuickEditProduct(product, MobileProductQuickEditField.STOCK)
    val priceDecision = canQuickEditProduct(product, MobileProductQuickEditField.GROSS_PRICE)
    val isVariantProduct = product.variantCount > 0

    DlaFlowCard(colors, accent = product.lowStock) {
        Row(verticalAlignment = Alignment.Top) {
            ProductThumbTile(
                colors = colors,
                mobileMediaClient = mobileMediaClient,
                mobileToken = mobileToken,
                thumbnailUrl = product.thumbnailUrl.ifBlank { product.image },
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            product.name,
                            color = colors.textStrong,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            productReference(product.sku, product.ean),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (status != null) {
                        Spacer(Modifier.width(6.dp))
                        ProductStatusBadge(
                            text = status,
                            tone = productStatusTone(status, product.lowStock, colors),
                        )
                    }
                }
                if (isVariantProduct) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Produkt ma ${variantCountLabel(product.variantCount)}. Zmiany wykonuj na wariantach.",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProductMetricBox(
                colors = colors,
                label = "Stan",
                value = "${product.stock} szt.",
                note = if (isVariantProduct) "Razem" else null,
                editable = !readOnly && stockDecision.allowed,
                modifier = Modifier.weight(1f),
                onEdit = { onQuickEditProduct(MobileProductQuickEditField.STOCK) },
            )
            ProductMetricBox(
                colors = colors,
                label = "Cena",
                value = formatMoney(product.grossPrice),
                note = if (isVariantProduct) "Od produktu" else null,
                editable = !readOnly && priceDecision.allowed,
                modifier = Modifier.weight(1f),
                onEdit = { onQuickEditProduct(MobileProductQuickEditField.GROSS_PRICE) },
            )
        }
        if (quickEdit is ProductQuickEditTarget.Product && quickEdit.product.id == product.id) {
            Spacer(Modifier.height(10.dp))
            ProductQuickEditPanel(
                colors = colors,
                target = quickEdit,
                onCancel = onCancelQuickEdit,
                onSaveProduct = onSaveQuickEditProduct,
                onSaveVariant = onSaveQuickEditVariant,
            )
        }
        if (isVariantProduct) {
            Spacer(Modifier.height(8.dp))
            ProductSmallActionButton(
                colors = colors,
                text = when {
                    variantsLoading -> "Pobieram warianty"
                    variants != null -> "Ukryj warianty"
                    else -> "Pokaż warianty"
                },
                enabled = !variantsLoading,
                modifier = Modifier.fillMaxWidth(),
                onClick = onToggleVariants,
            )
            if (variantsLoading) {
                Spacer(Modifier.height(7.dp))
                ProductVariantSkeleton(colors)
            }
            variants?.let { items ->
                Spacer(Modifier.height(8.dp))
                if (items.isEmpty()) {
                    ProductInlineNote(colors, "Brak wariantów do pokazania.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items.forEach { variant ->
                            MobileProductVariantRow(
                                colors = colors,
                                mobileMediaClient = mobileMediaClient,
                                mobileToken = mobileToken,
                                product = product,
                                variant = variant,
                                readOnly = readOnly,
                                quickEdit = quickEdit,
                                onQuickEditVariant = onQuickEditVariant,
                                onCancelQuickEdit = onCancelQuickEdit,
                                onSaveQuickEditProduct = onSaveQuickEditProduct,
                                onSaveQuickEditVariant = onSaveQuickEditVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductThumbTile(
    colors: DlaFlowComposeColors,
    mobileMediaClient: MobileApiClient,
    mobileToken: String,
    thumbnailUrl: String,
) {
    val loader = remember(mobileMediaClient, mobileToken) {
        DlaFlowThumbnailLoader { url ->
            loadMobileImageBitmap(mobileMediaClient, url, mobileToken)?.asImageBitmap()
        }
    }
    DlaFlowThumbnail(
        colors = colors,
        url = thumbnailUrl,
        loader = loader,
    )
}

private suspend fun loadMobileImageBitmap(
    mobileMediaClient: MobileApiClient,
    mediaUrl: String,
    mobileToken: String,
): Bitmap? = withContext(Dispatchers.IO) {
    val bytes = mobileMediaClient.getMobileMedia(mobileToken, mediaUrl) ?: return@withContext null

    decodeMobileImageBitmap(bytes)
}

@Composable
private fun ProductStatusBadge(
    text: String,
    tone: Color,
) {
    Text(
        text = text,
        color = tone,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.13f))
            .border(1.dp, tone.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun ProductMetricBox(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    note: String? = null,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
) {
    DlaFlowMetricBox(
        colors = colors,
        label = label,
        value = value,
        note = note,
        editable = editable,
        editLabel = "Zmień",
        modifier = modifier,
        onEdit = onEdit,
    )
}

@Composable
private fun MobileProductVariantRow(
    colors: DlaFlowComposeColors,
    mobileMediaClient: MobileApiClient,
    mobileToken: String,
    product: MobileProduct,
    variant: MobileProductVariant,
    readOnly: Boolean,
    quickEdit: ProductQuickEditTarget?,
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField) -> Unit,
    onCancelQuickEdit: () -> Unit,
    onSaveQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onSaveQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ProductThumbTile(
                colors = colors,
                mobileMediaClient = mobileMediaClient,
                mobileToken = mobileToken,
                thumbnailUrl = mobileVariantThumbnailUrl(variant, product),
            )
            Spacer(Modifier.width(8.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        variant.name,
                        color = colors.textStrong,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        productReference(variant.sku, variant.ean),
                        color = colors.textMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                productStatusLabel(variant.status, lowStock = false)?.let { status ->
                    Spacer(Modifier.width(8.dp))
                    ProductStatusBadge(status, productStatusTone(status, lowStock = false, colors))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProductMetricBox(
                colors = colors,
                label = "Stan",
                value = "${variant.stock} szt.",
                editable = !readOnly && variant.editableFields.stock,
                modifier = Modifier.weight(1f),
                onEdit = { onQuickEditVariant(variant, MobileVariantQuickEditField.STOCK) },
            )
            ProductMetricBox(
                colors = colors,
                label = "Cena",
                value = formatMoney(variant.price),
                editable = !readOnly && variant.editableFields.price,
                modifier = Modifier.weight(1f),
                onEdit = { onQuickEditVariant(variant, MobileVariantQuickEditField.PRICE) },
            )
        }
        if (quickEdit is ProductQuickEditTarget.Variant && quickEdit.variant.id == variant.id) {
            Spacer(Modifier.height(8.dp))
            ProductQuickEditPanel(
                colors = colors,
                target = quickEdit,
                onCancel = onCancelQuickEdit,
                onSaveProduct = onSaveQuickEditProduct,
                onSaveVariant = onSaveQuickEditVariant,
            )
        }
    }
}

@Composable
private fun ProductSmallActionButton(
    colors: DlaFlowComposeColors,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primarySoft,
            contentColor = colors.primary,
            disabledContainerColor = colors.surfaceSubtle,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = modifier.height(28.dp),
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProductInlineNote(colors: DlaFlowComposeColors, text: String) {
    Text(
        text = text,
        color = colors.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductQuickEditPanel(
    colors: DlaFlowComposeColors,
    target: ProductQuickEditTarget,
    onCancel: () -> Unit,
    onSaveProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onSaveVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
) {
    var value by remember(target) { mutableStateOf(target.initialInputValue()) }
    var error by remember(target) { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .imePadding()
                .padding(horizontal = 14.dp),
        ) {
            DlaFlowCard(colors, accent = true) {
                Text(
                    "Szybka zmiana",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    target.title(),
                    color = colors.textStrong,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = ""
                    },
                    label = { Text(target.fieldLabel()) },
                    singleLine = true,
                    isError = error.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.textStrong,
                        unfocusedTextColor = colors.textStrong,
                        focusedContainerColor = colors.surfaceSubtle,
                        unfocusedContainerColor = colors.surfaceSubtle,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = colors.danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DlaFlowSecondaryButton(colors, Icons.Rounded.Close, "Anuluj", modifier = Modifier.weight(1f), onClick = onCancel)
                    DlaFlowPrimaryButton(
                        colors = colors,
                        icon = Icons.Rounded.CheckCircle,
                        text = "Zapisz",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val parsed = parseQuickEditInput(value)
                            when {
                                parsed == null -> error = "Wpisz poprawną liczbę."
                                parsed < 0.0 -> error = "Wartość nie może być ujemna."
                                target.isStockField() && parsed % 1.0 != 0.0 -> error = "Stan wpisz jako pełną liczbę."
                                target.isStockField() && parsed > MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK.toDouble() -> error = "Stan jest zbyt wysoki."
                                !target.isStockField() && parsed > MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE -> error = "Cena jest zbyt wysoka."
                                else -> when (target) {
                                    is ProductQuickEditTarget.Product -> onSaveProduct(target.product, target.field, parsed)
                                    is ProductQuickEditTarget.Variant -> onSaveVariant(target.variant, target.field, parsed)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private sealed class ProductQuickEditTarget {
    data class Product(
        val product: MobileProduct,
        val field: MobileProductQuickEditField,
    ) : ProductQuickEditTarget()

    data class Variant(
        val variant: MobileProductVariant,
        val field: MobileVariantQuickEditField,
    ) : ProductQuickEditTarget()
}

private fun productsSummary(total: Int, visible: Int, loading: Boolean, readOnly: Boolean, noAccess: Boolean): String {
    val count = total.coerceAtLeast(visible)
    val base = when {
        loading && visible == 0 -> "ładowanie listy"
        count == 1 -> "1 produkt"
        count > 1 -> "$count produktów"
        else -> "lista produktów"
    }
    val mode = when {
        noAccess -> "brak dostępu"
        readOnly -> "tylko podgląd"
        else -> "edycja z telefonu"
    }

    return if (loading && visible > 0) {
        "$base · odświeżam · $mode"
    } else {
        "$base · $mode"
    }
}

private fun productReference(sku: String, ean: String): String {
    val parts = mutableListOf<String>()
    if (sku.isNotBlank()) {
        parts.add("SKU: $sku")
    }
    if (ean.isNotBlank()) {
        parts.add("EAN: $ean")
    }

    return if (parts.isEmpty()) "Bez SKU i EAN" else parts.joinToString(" · ")
}

private fun productStatusLabel(status: String, lowStock: Boolean): String? {
    return mobileProductStatusLabel(status, lowStock)
}

private fun productStatusTone(statusLabel: String, lowStock: Boolean, colors: DlaFlowComposeColors): Color {
    return when (statusLabel.lowercase(Locale.ROOT)) {
        "brak stanu" -> colors.danger
        "niski stan" -> colors.orange
        else -> if (lowStock) colors.orange else colors.success
    }
}

private fun variantCountLabel(count: Int): String {
    return when {
        count == 1 -> "1 wariant"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count warianty"
        else -> "$count wariantów"
    }
}

private fun ProductQuickEditTarget.title(): String {
    return when (this) {
        is ProductQuickEditTarget.Product -> product.name
        is ProductQuickEditTarget.Variant -> variant.name
    }
}

private fun ProductQuickEditTarget.fieldLabel(): String {
    return when (this) {
        is ProductQuickEditTarget.Product -> when (field) {
            MobileProductQuickEditField.GROSS_PRICE -> "Cena brutto"
            MobileProductQuickEditField.STOCK -> "Stan magazynowy"
        }
        is ProductQuickEditTarget.Variant -> when (field) {
            MobileVariantQuickEditField.PRICE -> "Cena wariantu"
            MobileVariantQuickEditField.STOCK -> "Stan wariantu"
        }
    }
}

private fun ProductQuickEditTarget.initialInputValue(): String {
    return when (this) {
        is ProductQuickEditTarget.Product -> when (field) {
            MobileProductQuickEditField.GROSS_PRICE -> decimalInputValue(product.grossPrice)
            MobileProductQuickEditField.STOCK -> product.stock.toString()
        }
        is ProductQuickEditTarget.Variant -> when (field) {
            MobileVariantQuickEditField.PRICE -> decimalInputValue(variant.price)
            MobileVariantQuickEditField.STOCK -> variant.stock.toString()
        }
    }
}

private fun ProductQuickEditTarget.isStockField(): Boolean {
    return when (this) {
        is ProductQuickEditTarget.Product -> field == MobileProductQuickEditField.STOCK
        is ProductQuickEditTarget.Variant -> field == MobileVariantQuickEditField.STOCK
    }
}

private fun decimalInputValue(value: Double): String {
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

private fun parseQuickEditInput(value: String): Double? {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isBlank()) {
        return null
    }

    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

private fun DashboardPhotoTask.toMobilePhotoTask(): MobilePhotoTask {
    return MobilePhotoTask(
        id = id,
        productName = productName,
        productSku = productSku,
        status = status,
        mediaCount = mediaCount,
        maxPhotos = maxPhotos,
        expiresAt = expiresAt,
    )
}

@Composable
private fun ProductPhotoTaskMicroNotice(
    colors: DlaFlowComposeColors,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onRefresh)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(colors.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddBox,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Zdjęcia z telefonu",
                color = colors.textStrong,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Brak zadań zdjęciowych.",
                color = colors.textMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "Odśwież",
            color = colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MessagesTab(colors: DlaFlowComposeColors, dashboard: DashboardContent?, onOpenNotifications: () -> Unit) {
    SectionTitle(colors, "Wiadomości", "Ostatnie sprawy klienta i operacji")
    LegacyNotificationsList(colors, dashboard?.notifications.orEmpty(), onOpenNotifications)
}

@Composable
private fun LegacyKpiGrid(
    colors: DlaFlowComposeColors,
    kpis: DashboardKpis?,
    onKpiClick: (MobileKpiDestination) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowKpiTile(colors, stringResource(R.string.dashboard_kpi_new_orders), (kpis?.newOrders ?: 0).toString(), Icons.Rounded.ShoppingCart, colors.primary, Modifier.weight(1f), onClick = { onKpiClick(MobileKpiDestination.NEW_ORDERS) })
        DlaFlowKpiTile(colors, stringResource(R.string.dashboard_kpi_to_ship), (kpis?.toShip ?: 0).toString(), Icons.Rounded.LocalShipping, colors.orange, Modifier.weight(1f), onClick = { onKpiClick(MobileKpiDestination.TO_SHIP) })
        DlaFlowKpiTile(colors, stringResource(R.string.dashboard_kpi_overdue), (kpis?.overdueOrProblems ?: 0).toString(), Icons.Rounded.Inventory2, colors.success, Modifier.weight(1f), onClick = { onKpiClick(MobileKpiDestination.OVERDUE) })
        DlaFlowKpiTile(colors, stringResource(R.string.dashboard_kpi_messages), (kpis?.messages ?: 0).toString(), Icons.Rounded.ChatBubbleOutline, colors.info, Modifier.weight(1f), onClick = { onKpiClick(MobileKpiDestination.MESSAGES) })
    }
}

@Composable
private fun LegacyNotificationsList(
    colors: DlaFlowComposeColors,
    notifications: List<DashboardNotification>,
    onOpenNotifications: () -> Unit,
) {
    DlaFlowNotificationPreviewCard(
        colors = colors,
        heading = stringResource(R.string.dashboard_notifications_heading),
        openAllLabel = stringResource(R.string.dashboard_notifications_open_all),
        emptyTitle = stringResource(R.string.dashboard_notifications_empty_title),
        emptySubtitle = stringResource(R.string.dashboard_notifications_empty_subtitle),
        isEmpty = notifications.isEmpty(),
        onOpenNotifications = onOpenNotifications,
    ) {
        Column {
            notifications.take(4).forEachIndexed { index, notification ->
                NotificationRow(colors, notification)
                if (index < notifications.take(4).lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
                            .height(1.dp)
                            .background(colors.borderSubtle),
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileAppUpdateDialog(
    colors: DlaFlowComposeColors,
    update: MobileAppUpdate,
    blocking: Boolean,
    dismissalsRemaining: Int,
    downloading: Boolean,
    downloadProgress: Int,
    error: String,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!blocking && !downloading) onDismiss() }) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, colors.primarySoftBorder),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DlaFlowIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (blocking) "Wymagana aktualizacja" else "Dostępna aktualizacja",
                            color = colors.textStrong,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 22.sp,
                        )
                        Text(
                            "Wersja ${update.latestVersionName} · ${formatMobileUpdateBytes(update.sizeBytes)}",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(update.releaseTitle, color = colors.textStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    update.releaseNotes.take(3).forEach { note ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(note, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
                        }
                    }
                }
                Text(
                    text = when {
                        blocking -> "Ta wersja jest potrzebna do dalszej pracy w aplikacji."
                        dismissalsRemaining == 1 -> "Możesz odłożyć aktualizację jeszcze 1 raz."
                        else -> "Możesz odłożyć aktualizację jeszcze $dismissalsRemaining razy."
                    },
                    color = if (blocking) colors.warning else colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                )
                if (downloading) {
                    MobileUpdateProgress(colors, downloadProgress)
                }
                if (error.isNotBlank()) {
                    Text(error, color = colors.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!blocking) {
                        DlaFlowSecondaryButton(
                            colors = colors,
                            icon = Icons.Rounded.Close,
                            text = "Później",
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                    }
                    DlaFlowPrimaryButton(
                        colors = colors,
                        icon = Icons.Rounded.Refresh,
                        text = if (downloading) "Pobieram" else "Zaktualizuj",
                        modifier = Modifier.weight(1f),
                        onClick = onInstall,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileUpdateProgress(colors: DlaFlowComposeColors, progress: Int) {
    val bounded = progress.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pobieranie $bounded%", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.borderSubtle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(bounded / 100f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.primary),
            )
        }
    }
}

private fun formatMobileUpdateBytes(bytes: Int): String {
    return if (bytes >= 1024 * 1024) {
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1024.0 / 1024.0)
    } else {
        "${(bytes / 1024).coerceAtLeast(1)} KB"
    }
}

@Composable
private fun AppHeader(
    colors: DlaFlowComposeColors,
    status: String,
    unreadCount: Int = 0,
    unreadAttentionCount: Int = 0,
    onScanPackage: (() -> Unit)? = null,
    onOpenNotifications: () -> Unit = {},
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(if (colors.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light),
            contentDescription = "DlaFlow",
            modifier = Modifier
                .height(25.dp)
                .width(96.dp),
        )
        Spacer(Modifier.weight(1f))
        if (status == "Połączono") {
            onScanPackage?.let { scan ->
                DlaFlowHeaderIconButton(
                    colors = colors,
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = stringResource(R.string.orders_scan_package),
                    onClick = scan,
                )
            }
            NotificationBell(
                colors = colors,
                unreadCount = unreadCount,
                unreadAttentionCount = unreadAttentionCount,
                onClick = onOpenNotifications,
            )
        } else {
            DlaFlowStatusBadge(colors, status)
        }
    }
}

@Composable
private fun NotificationBell(
    colors: DlaFlowComposeColors,
    unreadCount: Int,
    unreadAttentionCount: Int,
    onClick: () -> Unit,
) {
    val badgeState = notificationBadgeState(unreadCount, unreadAttentionCount)
    val badgeColor = if (badgeState == NotificationBadgeState.ATTENTION) colors.danger else colors.primary
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.NotificationsNone,
            contentDescription = "Powiadomienia",
            tint = colors.text,
            modifier = Modifier.size(25.dp),
        )
        if (badgeState != NotificationBadgeState.NONE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = unreadCount.coerceAtMost(99).toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    lineHeight = 8.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AssistantPhotoTaskCard(
    colors: DlaFlowComposeColors,
    task: MobilePhotoTask,
    highlighted: Boolean,
    onTakePhoto: (String) -> Unit,
    onPickPhoto: (String) -> Unit,
    onCompletePhotoTask: (String) -> Unit,
) {
    DlaFlowPhotoTaskCard(
        colors = colors,
        title = stringResource(R.string.dashboard_photo_task_title),
        productName = task.productName,
        skuText = if (task.productSku.isBlank()) "" else stringResource(R.string.dashboard_photo_task_sku, task.productSku),
        photosLabel = stringResource(R.string.dashboard_photo_task_photos),
        photosProgress = stringResource(R.string.dashboard_photo_task_photo_count, task.mediaCount, task.maxPhotos),
        mediaCount = task.mediaCount,
        maxPhotos = task.maxPhotos,
        takePhotoLabel = stringResource(R.string.dashboard_photo_task_take),
        pickPhotoLabel = stringResource(R.string.dashboard_photo_task_pick),
        completeTaskLabel = stringResource(R.string.dashboard_photo_task_complete),
        highlighted = highlighted,
        onTakePhoto = { onTakePhoto(task.id) },
        onPickPhoto = { onPickPhoto(task.id) },
        onCompletePhotoTask = { onCompletePhotoTask(task.id) },
    )
}

@Composable
private fun NotificationsScreen(
    colors: DlaFlowComposeColors,
    notifications: List<DashboardNotification>,
    loading: Boolean,
    selectedFilter: MobileNotificationFilter,
    onFilterChange: (MobileNotificationFilter) -> Unit,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.surfaceSubtle)
                    .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Wróć",
                    tint = colors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            DlaFlowScreenHeader(colors, "Powiadomienia", "Sprawy z panelu i telefonu")
        }
        NotificationFilterTabs(colors, selectedFilter, onFilterChange)
        val visible = filterDashboardNotifications(notifications, selectedFilter)
        DlaFlowCard(colors, accent = visible.any { toneColorKey(it.tone) == "attention" }) {
            if (loading && notifications.isEmpty()) {
                NotificationEmptyRow(colors, "Ładujemy powiadomienia", "Za chwilę pokażemy najnowsze sprawy z panelu.")
                return@DlaFlowCard
            }
            if (visible.isEmpty()) {
                NotificationEmptyRow(colors)
                return@DlaFlowCard
            }
            visible.forEachIndexed { index, notification ->
                NotificationRow(colors, notification)
                if (index < visible.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
                            .height(1.dp)
                            .background(colors.borderSubtle),
                    )
                }
            }
        }
        DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.CheckCircle,
            text = "Oznacz jako przeczytane",
            modifier = Modifier.fillMaxWidth(),
            enabled = visible.any { it.readAt.isNullOrBlank() },
            onClick = onMarkRead,
        )
    }
}

@Composable
private fun NotificationFilterTabs(
    colors: DlaFlowComposeColors,
    selectedFilter: MobileNotificationFilter,
    onFilterChange: (MobileNotificationFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MobileNotificationFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) colors.primary else Color.Transparent)
                    .clickable { onFilterChange(filter) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.label,
                    color = if (selected) Color.White else colors.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(colors: DlaFlowComposeColors, notification: DashboardNotification) {
    DlaFlowNotificationRow(
        colors = colors,
        title = notification.title,
        description = notification.description,
        tone = notification.tone,
        occurredLabel = relativeTime(notification.occurredAt),
    )
}

@Composable
private fun NotificationEmptyRow(
    colors: DlaFlowComposeColors,
    title: String = "Brak pilnych spraw",
    subtitle: String = "Gdy pojawi się wiadomość albo problem, zobaczysz go tutaj.",
) {
    DlaFlowNotificationEmptyRow(colors, title, subtitle)
}

@Composable
private fun SectionTitle(colors: DlaFlowComposeColors, title: String, subtitle: String) {
    DlaFlowScreenHeader(colors, title, subtitle)
}

@Composable
private fun TrendPill(colors: DlaFlowComposeColors, value: Double) {
    val positive = value >= 0.0
    val color = if (positive) colors.success else colors.danger
    Text(
        text = "${if (positive) "+" else ""}${String.format(Locale.US, "%.1f", value)}%",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun BottomNavigation(
    colors: DlaFlowComposeColors,
    selectedTab: MobileAssistantTab,
    dashboard: DashboardContent?,
    onSelectTab: (MobileAssistantTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderSubtle),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MobileAssistantTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { onSelectTab(tab) }
                        .padding(top = 2.dp, bottom = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(58.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        NavIcon(
                            colors = colors,
                            tab = tab,
                            selected = selected,
                            badge = navBadge(tab, dashboard),
                        )
                    }
                    Text(
                        text = tab.label,
                        color = if (selected) colors.primary else colors.textMuted,
                        fontSize = 9.4.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavIcon(colors: DlaFlowComposeColors, tab: MobileAssistantTab, selected: Boolean, badge: Int) {
    val tint = if (selected) colors.primary else colors.textMuted
    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = tabIcon(tab),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (selected) 27.dp else 24.dp),
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 7.dp, y = (-6).dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.coerceAtMost(99).toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 8.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun tabIcon(tab: MobileAssistantTab): ImageVector {
    return when (tab) {
        MobileAssistantTab.DASHBOARD -> Icons.Rounded.House
        MobileAssistantTab.ORDERS -> Icons.AutoMirrored.Rounded.ReceiptLong
        MobileAssistantTab.PRODUCTS -> Icons.Rounded.Inventory2
        MobileAssistantTab.MESSAGES -> Icons.Rounded.ChatBubbleOutline
        MobileAssistantTab.MORE -> Icons.Rounded.MoreHoriz
    }
}

private fun navBadge(tab: MobileAssistantTab, dashboard: DashboardContent?): Int {
    val kpis = dashboard?.kpis ?: return 0
    return when (tab) {
        MobileAssistantTab.ORDERS -> kpis.newOrders
        MobileAssistantTab.MESSAGES -> kpis.messages
        else -> 0
    }
}

private fun displayName(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) {
        return "DlaFlow"
    }

    return clean.substringBefore("@").replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

private fun formatMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(value)
}

private fun shortTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("")
}

private fun relativeTime(value: String): String {
    return runCatching {
        val minutes = Duration.between(OffsetDateTime.parse(value), OffsetDateTime.now()).toMinutes().coerceAtLeast(0)
        when {
            minutes < 1 -> "teraz"
            minutes < 60 -> "$minutes min temu"
            minutes < 24 * 60 -> "${minutes / 60}h"
            else -> "${minutes / (24 * 60)}d"
        }
    }.getOrDefault(shortTime(value))
}

private fun toneColor(colors: DlaFlowComposeColors, tone: String): Color {
    return when (tone.lowercase(Locale.ROOT)) {
        "error" -> colors.danger
        "success" -> colors.success
        "warning" -> colors.warning
        else -> colors.primary
    }
}
