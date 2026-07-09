package pl.dlaflow.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@OptIn(ExperimentalTextApi::class)
private fun dlaFlowInterFont(weight: FontWeight, axisWeight: Int): Font = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight)),
)

private val DlaFlowInter = FontFamily(
    dlaFlowInterFont(FontWeight.Normal, 400),
    dlaFlowInterFont(FontWeight.Medium, 500),
    dlaFlowInterFont(FontWeight.SemiBold, 600),
    dlaFlowInterFont(FontWeight.Bold, 700),
    dlaFlowInterFont(FontWeight.ExtraBold, 800),
)

private fun TextStyle.withDlaFlowTypography(fontFamily: FontFamily): TextStyle = copy(
    fontFamily = fontFamily,
    letterSpacing = 0.sp,
)

enum class MobileAssistantTab(val label: String, val symbol: String) {
    DASHBOARD("Pulpit", "P"),
    ORDERS("Zamówienia", "Z"),
    PRODUCTS("Produkty", "PR"),
    MESSAGES("Wiadomości", "W"),
    MORE("Więcej", "..."),
}

enum class MobileAssistantQuickAction {
    SCAN_PACKAGE,
    ADD_PRODUCT,
    STATS,
    PRODUCTS,
}

enum class MobileAssistantOverlayScreen {
    NONE,
    NOTIFICATIONS,
}

enum class MobileNotificationFilter(val label: String) {
    ALL("Wszystkie"),
    ATTENTION("Wymaga uwagi"),
    UNREAD("Nowe"),
}

sealed class MobilePackageScanUiState {
    data object Empty : MobilePackageScanUiState()
    data class Loading(val code: String) : MobilePackageScanUiState()
    data class Resolved(val result: MobilePackageScanLookupResult) : MobilePackageScanUiState()
    data class Failed(val code: String, val message: String) : MobilePackageScanUiState()
}

enum class MobileAssistantBackAction {
    NONE,
    CLOSE_PAIRING_HELP,
    CLOSE_ORDER_DETAIL,
    CLOSE_OVERLAY,
}

fun mobileAssistantBackAction(
    sessionConnected: Boolean,
    pairingHelpVisible: Boolean,
    overlayScreen: MobileAssistantOverlayScreen = MobileAssistantOverlayScreen.NONE,
    selectedTab: MobileAssistantTab,
    orderDetailVisible: Boolean,
): MobileAssistantBackAction {
    return when {
        !sessionConnected && pairingHelpVisible -> MobileAssistantBackAction.CLOSE_PAIRING_HELP
        sessionConnected && overlayScreen != MobileAssistantOverlayScreen.NONE -> MobileAssistantBackAction.CLOSE_OVERLAY
        sessionConnected && selectedTab == MobileAssistantTab.ORDERS && orderDetailVisible -> MobileAssistantBackAction.CLOSE_ORDER_DETAIL
        else -> MobileAssistantBackAction.NONE
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

private fun toneColorKey(tone: String): String {
    val normalized = tone.lowercase(Locale.ROOT)

    return if (normalized == "error" || normalized == "warning") "attention" else normalized
}

enum class MobileMoreSettingsKind {
    ACCOUNT,
    SECURITY,
    NOTIFICATIONS,
    PREFERENCES,
    INTEGRATIONS,
    TEAM,
    APP,
    CALLER_ID,
}

data class MobileMoreSettingsItem(
    val kind: MobileMoreSettingsKind,
    val title: String,
    val subtitle: String,
)

data class MobileMoreSettingsDetail(
    val kind: MobileMoreSettingsKind,
    val title: String,
    val description: String,
    val rows: List<Pair<String, String>>,
    val primaryActionLabel: String? = null,
    val secondaryActionLabel: String? = null,
    val dangerActionLabel: String? = null,
)

fun buildMobileMoreSettingsItems(
    appVersionName: String,
    callerIdLabel: String,
    canAutoOpenTasks: Boolean,
    updateAvailable: Boolean,
): List<MobileMoreSettingsItem> = listOf(
    MobileMoreSettingsItem(MobileMoreSettingsKind.ACCOUNT, "Dane konta", "Profil operatora i firma"),
    MobileMoreSettingsItem(MobileMoreSettingsKind.SECURITY, "Bezpieczeństwo", "Token telefonu chroniony"),
    MobileMoreSettingsItem(MobileMoreSettingsKind.NOTIFICATIONS, "Powiadomienia", "Zadania zdjęciowe i aktualizacje"),
    MobileMoreSettingsItem(
        MobileMoreSettingsKind.PREFERENCES,
        "Preferencje",
        if (canAutoOpenTasks) "Auto-otwieranie zadań" else "Przez powiadomienie",
    ),
    MobileMoreSettingsItem(MobileMoreSettingsKind.INTEGRATIONS, "Integracje", "Mobile Assistant w panelu"),
    MobileMoreSettingsItem(MobileMoreSettingsKind.TEAM, "Zespół", "Dostęp pracowników"),
    MobileMoreSettingsItem(
        MobileMoreSettingsKind.APP,
        "Aplikacja",
        if (updateAvailable) "Aktualizacja dostępna" else "Wersja $appVersionName",
    ),
    MobileMoreSettingsItem(MobileMoreSettingsKind.CALLER_ID, "Caller ID", callerIdLabel.ifBlank { "Do sprawdzenia" }),
)

fun buildMobileMoreSettingsDetail(
    kind: MobileMoreSettingsKind,
    userName: String,
    userEmail: String,
    tenantName: String,
    deviceName: String,
    appVersionName: String,
    callerIdLabel: String,
    notificationAllowed: Boolean,
    canAutoOpenTasks: Boolean,
    updateAvailable: Boolean,
): MobileMoreSettingsDetail {
    return when (kind) {
        MobileMoreSettingsKind.ACCOUNT -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Dane konta",
            description = "Podgląd operatora połączonego z panelem DlaFlow.",
            rows = listOf(
                "Operator" to userName.ifBlank { userEmail.substringBefore("@") },
                "E-mail" to userEmail,
                "Firma" to tenantName.ifBlank { "DlaFlow" },
                "Telefon" to deviceName.ifBlank { "Telefon DlaFlow" },
            ),
        )
        MobileMoreSettingsKind.SECURITY -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Bezpieczeństwo",
            description = "Telefon używa bezpiecznej sesji mobilnej. Gdy zgubisz urządzenie, odłącz je tutaj albo w panelu.",
            rows = listOf(
                "Sesja telefonu" to "Aktywna",
                "Token" to "Chroniony w pamięci systemowej Android",
                "Zakres dostępu" to "Tylko funkcje Mobile Assistant",
            ),
            dangerActionLabel = "Odłącz telefon",
        )
        MobileMoreSettingsKind.NOTIFICATIONS -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Powiadomienia",
            description = "Powiadomienia informują o zadaniach zdjęciowych, aktualizacjach i ważnych akcjach z panelu.",
            rows = listOf(
                "Status" to if (notificationAllowed) "Włączone" else "Wymagają zgody Androida",
                "Zadania zdjęciowe" to "Powiadomienie po wysłaniu z panelu",
                "Aktualizacje" to "Informacja o nowej wersji aplikacji",
            ),
            primaryActionLabel = "Ustawienia powiadomień",
        )
        MobileMoreSettingsKind.PREFERENCES -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Preferencje",
            description = "Ustaw sposób pracy telefonu z zadaniami i systemowymi zgodami Androida.",
            rows = listOf(
                "Auto-otwieranie zadań" to if (canAutoOpenTasks) "Włączone" else "Przez powiadomienie",
                "Motyw" to "Zgodny z ustawieniem systemu",
                "Układ" to "Standard DlaFlow Mobile",
            ),
            primaryActionLabel = if (canAutoOpenTasks) null else "Włącz auto-otwieranie",
        )
        MobileMoreSettingsKind.INTEGRATIONS -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Integracje",
            description = "Mobile Assistant jest zarządzany z panelu DlaFlow w sekcji Integracje i Wtyczki.",
            rows = listOf(
                "Wtyczka" to "Mobile Assistant",
                "Połączenie" to "Aktywne dla tej firmy",
                "Zarządzanie" to "Panel DlaFlow -> Integracje -> Wtyczki",
            ),
        )
        MobileMoreSettingsKind.TEAM -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Zespół",
            description = "Podgląd pracownika używającego telefonu. Uprawnienia i zespół zmieniaj w panelu.",
            rows = listOf(
                "Pracownik" to userName.ifBlank { userEmail.substringBefore("@") },
                "Firma" to tenantName.ifBlank { "DlaFlow" },
                "Zarządzanie" to "Panel DlaFlow -> Zespół",
            ),
        )
        MobileMoreSettingsKind.APP -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Aplikacja",
            description = "Sprawdź wersję aplikacji i pobierz aktualizację opublikowaną w panelu.",
            rows = listOf(
                "Wersja" to appVersionName,
                "Kanał" to "Production APK z panelu",
                "Status" to if (updateAvailable) "Aktualizacja dostępna" else "Masz aktualną wersję",
            ),
            primaryActionLabel = "Sprawdź aktualizację",
            secondaryActionLabel = "Ustawienia systemowe",
        )
        MobileMoreSettingsKind.CALLER_ID -> MobileMoreSettingsDetail(
            kind = kind,
            title = "Caller ID",
            description = "Caller ID pokazuje kontekst klienta i zamówienia przy połączeniu telefonicznym.",
            rows = listOf(
                "Status" to callerIdLabel.ifBlank { "Do sprawdzenia" },
                "Test numeru" to "Wpisz numer i sprawdź kartę klienta",
                "Połączenia" to "Działa dla zwykłych rozmów Android",
            ),
            primaryActionLabel = "Sprawdź numer",
            secondaryActionLabel = "Włącz Caller ID",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAssistantScreen(
    session: MobileSession?,
    dashboard: MobileAssistantDashboard?,
    photoTasks: List<MobilePhotoTask>,
    packageScanState: MobilePackageScanUiState,
    statusMessage: String,
    selectedTab: MobileAssistantTab,
    apiUrl: String,
    pairingCode: String,
    callerIdTestPhone: String,
    callerIdPreview: MobileCallerIdLookup?,
    callerIdOperational: Boolean,
    callerIdAvailable: Boolean,
    canAutoOpenTasks: Boolean,
    notificationAllowed: Boolean = true,
    appVersionName: String,
    appUpdate: MobileAppUpdate? = null,
    appUpdateDialogVisible: Boolean = false,
    appUpdateBlocking: Boolean = false,
    appUpdateDismissalsRemaining: Int = 0,
    appUpdateChecking: Boolean = false,
    appUpdateDownloading: Boolean = false,
    appUpdateDownloadProgress: Int = 0,
    appUpdateError: String = "",
    mobileOrders: List<MobileOrderListItem> = emptyList(),
    mobileOrdersNextOffset: Int? = null,
    mobileOrdersTotal: Int = 0,
    mobileOrdersLoading: Boolean = false,
    mobileOrdersSearch: String = "",
    mobileOrdersFilter: MobileOrderFilter = MobileOrderFilter.ALL,
    mobileOrdersNoAccess: Boolean = false,
    selectedMobileOrder: MobileOrderDetail? = null,
    selectedMobileOrderLoading: Boolean = false,
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
    onCallerIdTestPhoneChange: (String) -> Unit,
    onPairDevice: () -> Unit,
    onScanPairingQr: () -> Unit,
    onRefresh: () -> Unit,
    onQuickAction: (MobileAssistantQuickAction) -> Unit,
    onSelectTab: (MobileAssistantTab) -> Unit,
    onOrdersSearchChange: (String) -> Unit = {},
    onOrdersFilterChange: (MobileOrderFilter) -> Unit = {},
    onLoadMoreOrders: () -> Unit = {},
    onSelectOrder: (MobileOrderListItem) -> Unit = {},
    onOpenScannedOrder: (String) -> Unit = {},
    onCloseOrderDetail: () -> Unit = {},
    onProductsSearchChange: (String) -> Unit = {},
    onProductsFilterChange: (MobileProductFilter) -> Unit = {},
    onLoadMoreProducts: () -> Unit = {},
    onToggleProductVariants: (String) -> Unit = {},
    onQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit = { _, _, _ -> },
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit = { _, _, _ -> },
    onOpenNotifications: () -> Unit = {},
    onCloseOverlay: () -> Unit = {},
    onNotificationFilterChange: (MobileNotificationFilter) -> Unit = {},
    onMarkNotificationsRead: () -> Unit = {},
    onTakePhoto: (String) -> Unit,
    onPickPhoto: (String) -> Unit,
    onCompletePhotoTask: (String) -> Unit,
    onEnableCallerId: () -> Unit,
    onTestCallerId: () -> Unit,
    onShowCallerIdPreview: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onDismissAppUpdate: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAppSystemSettings: () -> Unit = {},
    onDisconnect: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val colors = dlaFlowColors(dark)
    val inter = DlaFlowInter
    var showPairingHelp by remember { mutableStateOf(false) }
    val backAction = mobileAssistantBackAction(
        sessionConnected = session != null,
        pairingHelpVisible = showPairingHelp,
        overlayScreen = mobileOverlayScreen,
        selectedTab = selectedTab,
        orderDetailVisible = selectedMobileOrder != null || selectedMobileOrderLoading,
    )

    MaterialTheme(
        colorScheme = colors.material,
        typography = MaterialTheme.typography.copy(
            displayLarge = MaterialTheme.typography.displayLarge.withDlaFlowTypography(inter),
            displayMedium = MaterialTheme.typography.displayMedium.withDlaFlowTypography(inter),
            displaySmall = MaterialTheme.typography.displaySmall.withDlaFlowTypography(inter),
            headlineLarge = MaterialTheme.typography.headlineLarge.withDlaFlowTypography(inter),
            headlineMedium = MaterialTheme.typography.headlineMedium.withDlaFlowTypography(inter),
            headlineSmall = MaterialTheme.typography.headlineSmall.withDlaFlowTypography(inter),
            titleLarge = MaterialTheme.typography.titleLarge.withDlaFlowTypography(inter),
            titleMedium = MaterialTheme.typography.titleMedium.withDlaFlowTypography(inter),
            titleSmall = MaterialTheme.typography.titleSmall.withDlaFlowTypography(inter),
            bodyLarge = MaterialTheme.typography.bodyLarge.withDlaFlowTypography(inter),
            bodyMedium = MaterialTheme.typography.bodyMedium.withDlaFlowTypography(inter),
            bodySmall = MaterialTheme.typography.bodySmall.withDlaFlowTypography(inter),
            labelLarge = MaterialTheme.typography.labelLarge.withDlaFlowTypography(inter),
            labelMedium = MaterialTheme.typography.labelMedium.withDlaFlowTypography(inter),
            labelSmall = MaterialTheme.typography.labelSmall.withDlaFlowTypography(inter),
        ),
    ) {
        BackHandler(enabled = backAction != MobileAssistantBackAction.NONE) {
            when (backAction) {
                MobileAssistantBackAction.CLOSE_PAIRING_HELP -> showPairingHelp = false
                MobileAssistantBackAction.CLOSE_ORDER_DETAIL -> onCloseOrderDetail()
                MobileAssistantBackAction.CLOSE_OVERLAY -> onCloseOverlay()
                MobileAssistantBackAction.NONE -> Unit
            }
        }
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
                    if (showPairingHelp) {
                        PairingHelpScreen(
                            colors = colors,
                            appVersionName = appVersionName,
                            onBack = { showPairingHelp = false },
                        )
                    } else {
                        PairingScreen(
                            colors = colors,
                            pairingCode = pairingCode,
                            appVersionName = appVersionName,
                            statusMessage = statusMessage,
                            onPairingCodeChange = onPairingCodeChange,
                            onPairDevice = onPairDevice,
                            onScanPairingQr = onScanPairingQr,
                            onShowPairingHelp = { showPairingHelp = true },
                        )
                    }
                } else {
                AssistantContent(
                    colors = colors,
                    apiUrl = apiUrl,
                    session = session,
                    dashboard = dashboard,
                    photoTasks = photoTasks,
                    packageScanState = packageScanState,
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
                        callerIdTestPhone = callerIdTestPhone,
                        callerIdPreview = callerIdPreview,
                    callerIdOperational = callerIdOperational,
                    callerIdAvailable = callerIdAvailable,
                    canAutoOpenTasks = canAutoOpenTasks,
                    notificationAllowed = notificationAllowed,
                    appVersionName = appVersionName,
                        appUpdate = appUpdate,
                        appUpdateChecking = appUpdateChecking,
                        appUpdateDownloading = appUpdateDownloading,
                        appUpdateDownloadProgress = appUpdateDownloadProgress,
                        appUpdateError = appUpdateError,
                        mobileOrders = mobileOrders,
                        mobileOrdersNextOffset = mobileOrdersNextOffset,
                        mobileOrdersTotal = mobileOrdersTotal,
                        mobileOrdersLoading = mobileOrdersLoading,
                        mobileOrdersSearch = mobileOrdersSearch,
                        mobileOrdersFilter = mobileOrdersFilter,
                        mobileOrdersNoAccess = mobileOrdersNoAccess,
                        selectedMobileOrder = selectedMobileOrder,
                        selectedMobileOrderLoading = selectedMobileOrderLoading,
                        onCallerIdTestPhoneChange = onCallerIdTestPhoneChange,
                        onRefresh = onRefresh,
                        onQuickAction = onQuickAction,
                        onOrdersSearchChange = onOrdersSearchChange,
                        onOrdersFilterChange = onOrdersFilterChange,
                        onLoadMoreOrders = onLoadMoreOrders,
                        onSelectOrder = onSelectOrder,
                        onOpenScannedOrder = onOpenScannedOrder,
                        onCloseOrderDetail = onCloseOrderDetail,
                        onProductsSearchChange = onProductsSearchChange,
                        onProductsFilterChange = onProductsFilterChange,
                        onLoadMoreProducts = onLoadMoreProducts,
                        onToggleProductVariants = onToggleProductVariants,
                        onQuickEditProduct = onQuickEditProduct,
                        onQuickEditVariant = onQuickEditVariant,
                        onOpenNotifications = onOpenNotifications,
                        onCloseOverlay = onCloseOverlay,
                        onNotificationFilterChange = onNotificationFilterChange,
                        onMarkNotificationsRead = onMarkNotificationsRead,
                        onTakePhoto = onTakePhoto,
                        onPickPhoto = onPickPhoto,
                        onCompletePhotoTask = onCompletePhotoTask,
                        onEnableCallerId = onEnableCallerId,
                        onTestCallerId = onTestCallerId,
                        onShowCallerIdPreview = onShowCallerIdPreview,
                        onCheckAppUpdate = onCheckAppUpdate,
                        onInstallAppUpdate = onInstallAppUpdate,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onOpenAppSystemSettings = onOpenAppSystemSettings,
                        onDisconnect = onDisconnect,
                    )
                }
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

@Composable
private fun PairingScreen(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    appVersionName: String,
    statusMessage: String,
    onPairingCodeChange: (String) -> Unit,
    onPairDevice: () -> Unit,
    onScanPairingQr: () -> Unit,
    onShowPairingHelp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Image(
            painter = painterResource(if (colors.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light),
            contentDescription = "DlaFlow",
            modifier = Modifier
                .height(42.dp)
                .width(168.dp),
        )
        Text(
            text = "Połącz z panelem DlaFlow",
            color = colors.textStrong,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 27.sp,
            fontFamily = DlaFlowInter,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Połącz aplikację mobilną z panelem, aby zarządzać zamówieniami, produktami i operacjami w swoim biznesie.",
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PairingQrCard(colors = colors, onScanPairingQr = onScanPairingQr)
        PairingDivider(colors)
        PairingManualCodeCard(
            colors = colors,
            pairingCode = pairingCode,
            onPairingCodeChange = onPairingCodeChange,
            onPairDevice = onPairDevice,
        )
        PairingHelpLink(colors, onShowPairingHelp)
        Text(
            text = "Wersja $appVersionName",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
        )
        if (statusMessage.isNotBlank()) {
            StatusStrip(colors, statusMessage)
        }
    }
}

@Composable
private fun PairingQrCard(colors: DlaFlowComposeColors, onScanPairingQr: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Skanuj kod QR",
                    color = colors.textStrong,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Zeskanuj kod QR wyświetlany w panelu DlaFlow.",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
            DlaIcon(Icons.Rounded.QrCodeScanner, colors.primary, modifier = Modifier.size(26.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = if (colors.dark) 0.18f else 0.08f),
                            colors.primarySoft.copy(alpha = if (colors.dark) 0.2f else 0.55f),
                            colors.surface,
                        ),
                    ),
                )
                .clickable(onClick = onScanPairingQr),
            contentAlignment = Alignment.Center,
        ) {
            PairingDotPattern(colors, Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
            PairingDotPattern(colors, Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(124.dp)) {
                PairingQrPreview(colors)
                PairingScanCorners(colors)
            }
            Text(
                "Skanuj kod QR",
                color = colors.primary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PairingDotPattern(colors: DlaFlowComposeColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(56.dp)) {
        val dotColor = colors.primary.copy(alpha = if (colors.dark) 0.16f else 0.14f)
        val gap = size.minDimension / 6f
        repeat(5) { row ->
            repeat(5) { column ->
                drawCircle(
                    color = dotColor,
                    radius = 2.5f,
                    center = Offset(column * gap + gap, row * gap + gap),
                )
            }
        }
    }
}

@Composable
private fun PairingQrPreview(colors: DlaFlowComposeColors) {
    val previewSize = if (colors.dark) 104.dp else 116.dp
    val previewBackground = if (colors.dark) Color(0xFFF1EEFF) else Color.White
    Canvas(
        modifier = Modifier
            .size(previewSize)
            .clip(RoundedCornerShape(8.dp))
            .background(previewBackground)
            .padding(10.dp),
    ) {
        val dark = Color(0xFF151A2E)
        val cells = 21
        val cell = size.minDimension / cells
        val moduleInset = cell * 0.08f

        fun module(x: Int, y: Int, width: Int = 1, color: Color = dark) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x * cell + moduleInset, y * cell + moduleInset),
                size = androidx.compose.ui.geometry.Size(width * cell - moduleInset * 2, width * cell - moduleInset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.08f),
            )
        }

        fun finder(x: Int, y: Int) {
            module(x, y, 7)
            module(x + 1, y + 1, 5, Color.White)
            module(x + 2, y + 2, 3)
        }

        fun reservedByFinder(x: Int, y: Int): Boolean {
            return (x <= 7 && y <= 7) || (x >= 13 && y <= 7) || (x <= 7 && y >= 13)
        }

        finder(0, 0)
        finder(14, 0)
        finder(0, 14)

        for (index in 8..12) {
            if (index % 2 == 0) {
                module(index, 6)
                module(6, index)
            }
        }

        repeat(cells) { y ->
            repeat(cells) { x ->
                if (!reservedByFinder(x, y) && x != 6 && y != 6) {
                    val value = x * 13 + y * 17 + x * y * 7 + (x xor y) * 5
                    val denseBlock = (x in 10..17 && y in 9..18 && value % 13 in 0..5)
                    val quietBreak = x in 8..10 && y in 8..10
                    val shouldDraw = !quietBreak && (value % 11 in 0..3 || denseBlock || ((x + y) % 7 == 0 && value % 5 == 0))
                    if (shouldDraw) {
                        module(x, y)
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingScanCorners(colors: DlaFlowComposeColors) {
    Canvas(modifier = Modifier.size(124.dp)) {
        val strokeWidth = 4.dp.toPx()
        val corner = 23.dp.toPx()
        val inset = 1.dp.toPx()
        val maxX = size.width - inset
        val maxY = size.height - inset
        drawLine(colors.primary, Offset(inset, corner), Offset(inset, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, inset), Offset(corner, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX - corner, inset), Offset(maxX, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX, inset), Offset(maxX, corner), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, maxY - corner), Offset(inset, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, maxY), Offset(corner, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX - corner, maxY), Offset(maxX, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX, maxY - corner), Offset(maxX, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun PairingDivider(colors: DlaFlowComposeColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
        Text("LUB", color = colors.textMuted, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
private fun PairingManualCodeCard(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
    onPairDevice: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Wpisz kod połączenia",
                    color = colors.textStrong,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Wpisz 6-znakowy kod wyświetlany w panelu.",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
            DlaIcon(Icons.Rounded.Keyboard, colors.primary, modifier = Modifier.size(25.dp))
        }
        PairingCodeBoxes(colors, pairingCode, onPairingCodeChange)
        Button(
            onClick = onPairDevice,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                "Połącz",
                fontSize = 15.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PairingCodeBoxes(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
) {
    val normalized = normalizePairingCodeInput(pairingCode)
    BasicTextField(
        value = normalized,
        onValueChange = { onPairingCodeChange(formatPairingCodeInput(it)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        ),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(6) { index ->
                    val char = normalized.getOrNull(index)?.toString()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.surfaceSubtle)
                            .border(1.dp, colors.border, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char ?: "•",
                            color = if (char == null) colors.textMuted.copy(alpha = 0.75f) else colors.textStrong,
                            fontSize = if (char == null) 18.sp else 17.sp,
                            fontFamily = DlaFlowInter,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PairingHelpLink(colors: DlaFlowComposeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.dp, colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.QuestionMark,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Gdzie znaleźć kod połączenia?",
            color = colors.primary,
            fontSize = 14.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PairingHelpScreen(
    colors: DlaFlowComposeColors,
    appVersionName: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Wróć",
                    color = colors.primary,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
        Image(
            painter = painterResource(if (colors.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light),
            contentDescription = "DlaFlow",
            modifier = Modifier
                .height(34.dp)
                .width(136.dp)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = "Gdzie znaleźć kod połączenia?",
            color = colors.textStrong,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Kod i QR są w panelu DlaFlow, w konfiguracji wtyczki Mobile Assistant.",
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PairingHelpPathCard(colors)
        PairingHelpStepCard(
            colors = colors,
            step = "1",
            title = "Otwórz panel DlaFlow",
            description = "Wejdź na panel.dlayou.pl i zaloguj się na konto firmy.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_login,
                darkRes = R.drawable.help_pairing_panel_login_dark,
                height = 212.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "2",
            title = "Kliknij Integracje",
            description = "W lewym menu panelu wybierz pozycję Integracje.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_integracje,
                darkRes = R.drawable.help_pairing_panel_integracje_dark,
                height = 178.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "3",
            title = "Wybierz Wtyczki",
            description = "Na stronie integracji przełącz kategorię na Wtyczki.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_wtyczki,
                darkRes = R.drawable.help_pairing_panel_wtyczki_dark,
                height = 156.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "4",
            title = "Otwórz Asystenta mobilnego",
            description = "Na karcie Asystent mobilny DlaFlow kliknij Konfiguruj.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_mobile_card,
                darkRes = R.drawable.help_pairing_panel_mobile_card_dark,
                height = 166.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "5",
            title = "Kliknij Sparuj telefon",
            description = "W oknie Mobile Assistant użyj przycisku Sparuj telefon w sekcji Połącz telefon.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_pair_button,
                darkRes = R.drawable.help_pairing_panel_pair_button_dark,
                height = 188.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "6",
            title = "Zeskanuj QR albo wpisz kod",
            description = "Zeskanuj kod QR z panelu. Jeśli aparat nie złapie kodu, przepisz 6-znakowy kod z tej samej sekcji.",
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_pairing_qr,
                darkRes = R.drawable.help_pairing_panel_pairing_qr_dark,
                height = 212.dp,
            )
        }
        Text(
            text = "Wersja $appVersionName",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PairingHelpPathCard(colors: DlaFlowComposeColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primarySoft)
            .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Najkrótsza ścieżka",
            color = colors.primary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "Panel DlaFlow → Integracje → Wtyczki → Asystent mobilny DlaFlow → Konfiguruj → Sparuj telefon",
            color = colors.textStrong,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PairingHelpStepCard(
    colors: DlaFlowComposeColors,
    step: String,
    title: String,
    description: String,
    graphic: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    step,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textStrong,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        graphic()
    }
}

@Composable
private fun HelpPanelScreenshot(
    colors: DlaFlowComposeColors,
    lightRes: Int,
    darkRes: Int,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (colors.dark) darkRes else lightRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun HelpGraphicFrame(colors: DlaFlowComposeColors, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        content()
    }
}

@Composable
private fun HelpLoginGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            HelpMiniTopBar(colors, "panel.dlayou.pl")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Text("DlaFlow", color = colors.primary, fontSize = 13.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
                HelpClickButton(colors, "Zaloguj się", Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
private fun HelpSidebarGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HelpMenuItem(colors, "Pulpit", false)
                HelpMenuItem(colors, "Zamówienia", false)
                HelpMenuItem(colors, "Produkty", false)
                HelpMenuItem(colors, "Integracje", true)
            }
            HelpPanelBlank(colors, "Kliknij Integracje")
        }
    }
}

@Composable
private fun HelpPluginCategoryGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpMiniTopBar(colors, "Integracje")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                HelpChip(colors, "Marketplace", false, Modifier.weight(1f))
                HelpChip(colors, "Kurierzy", false, Modifier.weight(1f))
                HelpChip(colors, "Wtyczki", true, Modifier.weight(1f))
            }
            HelpPanelBlank(colors, "Wybierz Wtyczki")
        }
    }
}

@Composable
private fun HelpPluginCardGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            DlaIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(34.dp))
            Column(modifier = Modifier.padding(start = 44.dp, end = 96.dp)) {
                Text("Asystent mobilny DlaFlow", color = colors.textStrong, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Telefon", color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Medium)
            }
            HelpClickButton(colors, "Konfiguruj", Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun HelpPairingButtonGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Text("Połącz telefon", color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
                Text("Kod pojawi się po kliknięciu.", color = colors.textStrong, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                HelpClickButton(colors, "Sparuj telefon", Modifier.align(Alignment.BottomCenter).fillMaxWidth())
            }
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primarySoft)
                    .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Mobile\nAssistant", color = colors.primary, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HelpCodeGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                PairingQrPreview(colors.copyForQrPreview())
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("Zeskanuj QR w aplikacji", color = colors.textStrong, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Kod awaryjny", color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("ABC-123", color = colors.primary, fontSize = 21.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

private fun DlaFlowComposeColors.copyForQrPreview(): DlaFlowComposeColors = copy(dark = false)

@Composable
private fun HelpMiniTopBar(colors: DlaFlowComposeColors, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = colors.textStrong, fontSize = 11.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HelpMenuItem(colors: DlaFlowComposeColors, text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) colors.primary else colors.surfaceSubtle)
            .border(1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = if (active) Color.White else colors.textMuted, fontSize = 9.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HelpChip(colors: DlaFlowComposeColors, text: String, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) colors.primary else colors.surface)
            .border(1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (active) Color.White else colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun HelpPanelBlank(colors: DlaFlowComposeColors, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.primary, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HelpClickButton(colors: DlaFlowComposeColors, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(31.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.primary)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun normalizePairingCodeInput(value: String): String {
    return value.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(6)
}

private fun formatPairingCodeInput(value: String): String {
    val normalized = normalizePairingCodeInput(value)
    return if (normalized.length > 3) {
        "${normalized.take(3)}-${normalized.drop(3)}"
    } else {
        normalized
    }
}

@Composable
private fun AssistantContent(
    colors: DlaFlowComposeColors,
    apiUrl: String,
    session: MobileSession,
    dashboard: MobileAssistantDashboard?,
    photoTasks: List<MobilePhotoTask>,
    packageScanState: MobilePackageScanUiState,
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
    callerIdTestPhone: String,
    callerIdPreview: MobileCallerIdLookup?,
    callerIdOperational: Boolean,
    callerIdAvailable: Boolean,
    canAutoOpenTasks: Boolean,
    notificationAllowed: Boolean,
    appVersionName: String,
    appUpdate: MobileAppUpdate?,
    appUpdateChecking: Boolean,
    appUpdateDownloading: Boolean,
    appUpdateDownloadProgress: Int,
    appUpdateError: String,
    mobileOrders: List<MobileOrderListItem>,
    mobileOrdersNextOffset: Int?,
    mobileOrdersTotal: Int,
    mobileOrdersLoading: Boolean,
    mobileOrdersSearch: String,
    mobileOrdersFilter: MobileOrderFilter,
    mobileOrdersNoAccess: Boolean,
    selectedMobileOrder: MobileOrderDetail?,
    selectedMobileOrderLoading: Boolean,
    onCallerIdTestPhoneChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onQuickAction: (MobileAssistantQuickAction) -> Unit,
    onOrdersSearchChange: (String) -> Unit,
    onOrdersFilterChange: (MobileOrderFilter) -> Unit,
    onLoadMoreOrders: () -> Unit,
    onSelectOrder: (MobileOrderListItem) -> Unit,
    onOpenScannedOrder: (String) -> Unit,
    onCloseOrderDetail: () -> Unit,
    onProductsSearchChange: (String) -> Unit,
    onProductsFilterChange: (MobileProductFilter) -> Unit,
    onLoadMoreProducts: () -> Unit,
    onToggleProductVariants: (String) -> Unit,
    onQuickEditProduct: (MobileProduct, MobileProductQuickEditField, Double) -> Unit,
    onQuickEditVariant: (MobileProductVariant, MobileVariantQuickEditField, Double) -> Unit,
    onOpenNotifications: () -> Unit,
    onCloseOverlay: () -> Unit,
    onNotificationFilterChange: (MobileNotificationFilter) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onTakePhoto: (String) -> Unit,
    onPickPhoto: (String) -> Unit,
    onCompletePhotoTask: (String) -> Unit,
    onEnableCallerId: () -> Unit,
    onTestCallerId: () -> Unit,
    onShowCallerIdPreview: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSystemSettings: () -> Unit,
    onDisconnect: () -> Unit,
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
            onOpenNotifications = onOpenNotifications,
        )
        if (mobileOverlayScreen == MobileAssistantOverlayScreen.NOTIFICATIONS) {
            NotificationsScreen(
                colors = colors,
                notifications = mobileNotifications.ifEmpty { dashboard?.notifications.orEmpty() },
                loading = mobileNotificationsLoading,
                selectedFilter = mobileNotificationFilter,
                onFilterChange = onNotificationFilterChange,
                onBack = onCloseOverlay,
                onMarkRead = onMarkNotificationsRead,
            )
        } else {
            when (selectedTab) {
                MobileAssistantTab.DASHBOARD -> DashboardTab(colors, session, dashboard, photoTasks, onRefresh, onQuickAction, onOpenNotifications, onTakePhoto, onPickPhoto, onCompletePhotoTask)
                MobileAssistantTab.ORDERS -> OrdersTab(
                    colors = colors,
                    apiUrl = apiUrl,
                    mobileToken = session.token,
                    dashboard = dashboard,
                    packageScanState = packageScanState,
                    mobileOrders = mobileOrders,
                    mobileOrdersNextOffset = mobileOrdersNextOffset,
                    mobileOrdersTotal = mobileOrdersTotal,
                    mobileOrdersLoading = mobileOrdersLoading,
                    mobileOrdersSearch = mobileOrdersSearch,
                    mobileOrdersFilter = mobileOrdersFilter,
                    mobileOrdersNoAccess = mobileOrdersNoAccess,
                    selectedMobileOrder = selectedMobileOrder,
                    selectedMobileOrderLoading = selectedMobileOrderLoading,
                    onQuickAction = onQuickAction,
                    onOrdersSearchChange = onOrdersSearchChange,
                    onOrdersFilterChange = onOrdersFilterChange,
                        onLoadMoreOrders = onLoadMoreOrders,
                        onSelectOrder = onSelectOrder,
                        onOpenScannedOrder = onOpenScannedOrder,
                        onCloseOrderDetail = onCloseOrderDetail,
                )
                MobileAssistantTab.PRODUCTS -> ProductsTab(
                    colors = colors,
                    apiUrl = apiUrl,
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
                    onRefresh = onRefresh,
                    onProductsSearchChange = onProductsSearchChange,
                    onProductsFilterChange = onProductsFilterChange,
                    onLoadMoreProducts = onLoadMoreProducts,
                    onToggleProductVariants = onToggleProductVariants,
                    onQuickEditProduct = onQuickEditProduct,
                    onQuickEditVariant = onQuickEditVariant,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onCompletePhotoTask = onCompletePhotoTask,
                )
                MobileAssistantTab.MESSAGES -> MessagesTab(colors, dashboard, onOpenNotifications)
                MobileAssistantTab.MORE -> MoreTab(
                    colors = colors,
                    session = session,
                    dashboard = dashboard,
                    statusMessage = statusMessage,
                    callerIdTestPhone = callerIdTestPhone,
                    callerIdPreview = callerIdPreview,
                    callerIdOperational = callerIdOperational,
                    callerIdAvailable = callerIdAvailable,
                    canAutoOpenTasks = canAutoOpenTasks,
                    notificationAllowed = notificationAllowed,
                    appVersionName = appVersionName,
                    appUpdate = appUpdate,
                    appUpdateChecking = appUpdateChecking,
                    appUpdateDownloading = appUpdateDownloading,
                    appUpdateDownloadProgress = appUpdateDownloadProgress,
                    appUpdateError = appUpdateError,
                    onCallerIdTestPhoneChange = onCallerIdTestPhoneChange,
                    onEnableCallerId = onEnableCallerId,
                    onTestCallerId = onTestCallerId,
                    onShowCallerIdPreview = onShowCallerIdPreview,
                    onCheckAppUpdate = onCheckAppUpdate,
                    onInstallAppUpdate = onInstallAppUpdate,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAppSystemSettings = onOpenAppSystemSettings,
                    onDisconnect = onDisconnect,
                )
            }
        }
        if (shouldShowAssistantStatus(statusMessage)) {
            StatusStrip(colors, statusMessage)
        }
        Spacer(Modifier.height(8.dp))
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
private fun DashboardTab(
    colors: DlaFlowComposeColors,
    session: MobileSession,
    dashboard: MobileAssistantDashboard?,
    photoTasks: List<MobilePhotoTask>,
    onRefresh: () -> Unit,
    onQuickAction: (MobileAssistantQuickAction) -> Unit,
    onOpenNotifications: () -> Unit,
    onTakePhoto: (String) -> Unit,
    onPickPhoto: (String) -> Unit,
    onCompletePhotoTask: (String) -> Unit,
) {
    GreetingRow(colors, dashboard?.userName ?: session.userEmail, onRefresh)
    RevenueCard(colors, dashboard)
    KpiGrid(colors, dashboard)
    NotificationsList(colors, dashboard?.notifications.orEmpty(), onOpenNotifications)
    QuickActions(colors, onQuickAction)
    val activeTask = dashboard?.activePhotoTask
    if (activeTask != null) {
        AssistantPhotoTaskCard(
            colors = colors,
            task = MobilePhotoTask(
                id = activeTask.id,
                productName = activeTask.productName,
                productSku = activeTask.productSku,
                status = activeTask.status,
                mediaCount = activeTask.mediaCount,
                maxPhotos = activeTask.maxPhotos,
                expiresAt = activeTask.expiresAt,
            ),
            highlighted = true,
            onTakePhoto = onTakePhoto,
            onPickPhoto = onPickPhoto,
            onCompletePhotoTask = onCompletePhotoTask,
        )
    } else if (photoTasks.isNotEmpty()) {
        AssistantPhotoTaskCard(colors, photoTasks.first(), true, onTakePhoto, onPickPhoto, onCompletePhotoTask)
    }
}

@Composable
private fun OrdersTab(
    colors: DlaFlowComposeColors,
    apiUrl: String,
    mobileToken: String,
    dashboard: MobileAssistantDashboard?,
    packageScanState: MobilePackageScanUiState,
    mobileOrders: List<MobileOrderListItem>,
    mobileOrdersNextOffset: Int?,
    mobileOrdersTotal: Int,
    mobileOrdersLoading: Boolean,
    mobileOrdersSearch: String,
    mobileOrdersFilter: MobileOrderFilter,
    mobileOrdersNoAccess: Boolean,
    selectedMobileOrder: MobileOrderDetail?,
    selectedMobileOrderLoading: Boolean,
    onQuickAction: (MobileAssistantQuickAction) -> Unit,
    onOrdersSearchChange: (String) -> Unit,
    onOrdersFilterChange: (MobileOrderFilter) -> Unit,
    onLoadMoreOrders: () -> Unit,
    onSelectOrder: (MobileOrderListItem) -> Unit,
    onOpenScannedOrder: (String) -> Unit,
    onCloseOrderDetail: () -> Unit,
) {
    SectionTitle(colors, "Zamówienia", ordersSummary(mobileOrdersTotal, mobileOrders.size, mobileOrdersLoading, mobileOrdersNoAccess))
    OrderSearchField(colors, mobileOrdersSearch, onOrdersSearchChange)
    OrderFilterChips(colors, mobileOrdersFilter, onOrdersFilterChange)

    if (selectedMobileOrder != null || selectedMobileOrderLoading) {
        MobileOrderDetailPanel(
            colors = colors,
            order = selectedMobileOrder,
            loading = selectedMobileOrderLoading,
            onClose = onCloseOrderDetail,
        )
        return
    }

    if (mobileOrdersNoAccess) {
        ProductStateCard(
            colors = colors,
            icon = Icons.Rounded.Warning,
            iconColor = colors.danger,
            title = "Brak dostępu",
            description = "To konto nie ma dostępu do zamówień w telefonie.",
        )
        return
    }

    PackageScannerCard(
        colors = colors,
        scanState = packageScanState,
        onOpenOrder = onOpenScannedOrder,
        onScanAgain = { onQuickAction(MobileAssistantQuickAction.SCAN_PACKAGE) },
    )
    KpiGrid(colors, dashboard)

    when {
        mobileOrdersLoading && mobileOrders.isEmpty() -> OrderListSkeleton(colors)
        mobileOrders.isEmpty() -> ProductStateCard(
            colors = colors,
            icon = Icons.Rounded.Search,
            iconColor = colors.textMuted,
            title = "Brak zamówień",
            description = "Zmień wyszukiwanie lub filtr, a potem odśwież listę.",
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            mobileOrders.forEach { order ->
                MobileOrderCard(
                    colors = colors,
                    apiUrl = apiUrl,
                    mobileToken = mobileToken,
                    order = order,
                    onClick = { onSelectOrder(order) },
                )
            }
        }
    }

    if (mobileOrdersLoading && mobileOrders.isNotEmpty()) {
        ProductStateCard(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            iconColor = colors.primary,
            title = "Odświeżam zamówienia",
            description = "Możesz dalej przeglądać widoczną listę.",
        )
    }
    if (mobileOrdersNextOffset != null && mobileOrders.isNotEmpty()) {
        SecondaryActionButton(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            text = if (mobileOrdersLoading) "Pobieram..." else "Pokaż więcej",
            onClick = {
                if (!mobileOrdersLoading) {
                    onLoadMoreOrders()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderSearchField(
    colors: DlaFlowComposeColors,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(19.dp),
            )
        },
        placeholder = {
            Text("Szukaj po numerze, kliencie, telefonie lub produkcie", color = colors.textMuted, fontSize = 12.sp)
        },
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
}

@Composable
private fun OrderFilterChips(
    colors: DlaFlowComposeColors,
    selected: MobileOrderFilter,
    onFilterChange: (MobileOrderFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MobileOrderFilter.entries.chunked(3).forEach { row ->
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
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OrderListSkeleton(colors: DlaFlowComposeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(4) {
            PanelCard(colors) {
                Row(verticalAlignment = Alignment.Top) {
                    ProductSkeletonBlock(colors, Modifier.size(38.dp), radius = 8.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.68f).height(15.dp))
                        Spacer(Modifier.height(7.dp))
                        ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.44f).height(10.dp))
                        Spacer(Modifier.height(7.dp))
                        ProductSkeletonBlock(colors, Modifier.fillMaxWidth(0.88f).height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileOrderCard(
    colors: DlaFlowComposeColors,
    apiUrl: String,
    mobileToken: String,
    order: MobileOrderListItem,
    onClick: () -> Unit,
) {
    val statusColor = orderToneColor(colors, order.statusTone)
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        PanelCard(colors, accent = mobileOrderUiTone(order.statusTone) == MobileOrderUiTone.WARNING) {
            Row(verticalAlignment = Alignment.Top) {
                if (order.thumbnailUrl.isNotBlank()) {
                    ProductThumbTile(
                        colors = colors,
                        apiUrl = apiUrl,
                        mobileToken = mobileToken,
                        thumbnailUrl = order.thumbnailUrl,
                    )
                } else {
                    DlaIcon(orderIcon(order), statusColor, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                order.customer,
                                color = colors.textStrong,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "#${order.orderNumber} · ${order.channel.ifBlank { "Panel" }}",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatMoney(order.amount),
                            color = colors.textStrong,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        order.productSummary.ifBlank { "${order.itemCount} produktów" },
                        color = colors.text,
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OrderTinyPill(colors, mobileOrderStatusLabel(order.status), statusColor)
                        OrderTinyPill(colors, order.paymentStatus.ifBlank { "Płatność" }, orderToneColor(colors, order.paymentTone))
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            orderQuickInfo(order),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            orderBadgeSummary(order),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTinyPill(
    colors: DlaFlowComposeColors,
    text: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = tone,
        fontSize = 8.8.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun MobileOrderDetailPanel(
    colors: DlaFlowComposeColors,
    order: MobileOrderDetail?,
    loading: Boolean,
    onClose: () -> Unit,
) {
    PanelCard(colors, accent = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Szczegóły zamówienia",
                color = colors.textStrong,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) {
                Text("Wróć", color = colors.primary, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (loading && order == null) {
            Spacer(Modifier.height(10.dp))
            OrderListSkeleton(colors)
            return@PanelCard
        }
        if (order == null) {
            Text("Nie udało się pobrać zamówienia.", color = colors.textMuted, fontSize = 12.sp)
            return@PanelCard
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "#${order.orderNumber}",
            color = colors.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            order.customer.name,
            color = colors.textStrong,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProductMetricBox(
                colors = colors,
                label = "Wartość",
                value = formatMoney(order.amount),
                editable = false,
                modifier = Modifier.weight(1f),
                onEdit = {},
            )
            ProductMetricBox(
                colors = colors,
                label = "Status",
                value = mobileOrderStatusLabel(order.status),
                editable = false,
                modifier = Modifier.weight(1f),
                onEdit = {},
            )
        }
        Spacer(Modifier.height(10.dp))
        MobileOrderDetailSection(colors, "Klient") {
            KeyValue(colors, "Telefon", order.customer.phone.ifBlank { "Brak" })
            KeyValue(colors, "E-mail", order.customer.email.ifBlank { "Brak" })
            KeyValue(colors, "Login", order.customer.nick.ifBlank { "Brak" })
        }
        MobileOrderDetailSection(colors, "Płatność") {
            KeyValue(colors, "Status", order.payment.status.ifBlank { "Do sprawdzenia" })
            KeyValue(colors, "Metoda", order.payment.method.ifBlank { "Brak" })
            KeyValue(colors, "Zapłacono", formatMoney(order.payment.paidAmount))
        }
        MobileOrderDetailSection(colors, "Dostawa") {
            KeyValue(colors, "Metoda", order.delivery.method.ifBlank { "Dostawa" })
            Text(orderAddressLabel(order.delivery.address), color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
        }
        MobileOrderDetailSection(colors, "Produkty") {
            order.items.ifEmpty {
                listOf(MobileOrderItem("", "", "", "", 0.0, order.productSummary.ifBlank { "Produkt" }, "", "", order.itemCount, "", order.amount, ""))
            }.forEach { item ->
                MobileOrderDetailListRow(
                    colors = colors,
                    title = item.name,
                    subtitle = listOfNotNull(
                        item.sku.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                        "${item.quantity} szt.",
                    ).joinToString(" · "),
                    value = formatMoney(item.lineTotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity.coerceAtLeast(1))),
                )
            }
        }
        if (order.shipments.isNotEmpty()) {
            MobileOrderDetailSection(colors, "Przesyłki") {
                order.shipments.forEach { shipment ->
                    MobileOrderDetailListRow(
                        colors = colors,
                        title = shipment.carrier.ifBlank { "Przesyłka" },
                        subtitle = shipment.trackingNumber.ifBlank { shipment.status },
                        value = if (shipment.labelReady) "Etykieta" else shipment.status,
                    )
                }
            }
        }
        if (order.messages.isNotEmpty()) {
            MobileOrderDetailSection(colors, "Wiadomości") {
                order.messages.take(3).forEach { message ->
                    MobileOrderDetailListRow(
                        colors = colors,
                        title = message.author.ifBlank { "Klient" },
                        subtitle = message.body,
                        value = relativeTime(message.messageAt),
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileOrderDetailSection(
    colors: DlaFlowComposeColors,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
    ) {
        Text(title, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 16.sp)
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun MobileOrderDetailListRow(
    colors: DlaFlowComposeColors,
    title: String,
    subtitle: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(value, color = colors.textStrong, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun ProductsTab(
    colors: DlaFlowComposeColors,
    apiUrl: String,
    mobileToken: String,
    dashboard: MobileAssistantDashboard?,
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
                    apiUrl = apiUrl,
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
        SecondaryActionButton(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSearchField(
    colors: DlaFlowComposeColors,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(19.dp),
            )
        },
        placeholder = {
            Text("Szukaj po nazwie, SKU lub EAN", color = colors.textMuted, fontSize = 12.sp)
        },
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
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.primarySoft else colors.surface)
            .border(
                1.dp,
                if (selected) colors.primarySoftBorder else colors.border,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) colors.primary else colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
    PanelCard(colors) {
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.55f), RoundedCornerShape(radius)),
    )
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
    PanelCard(colors) {
        Row(verticalAlignment = Alignment.Top) {
            DlaIcon(icon, iconColor, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textStrong,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun MobileProductCard(
    colors: DlaFlowComposeColors,
    apiUrl: String,
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

    PanelCard(colors, accent = product.lowStock) {
        Row(verticalAlignment = Alignment.Top) {
            ProductThumbTile(
                colors = colors,
                apiUrl = apiUrl,
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
                                apiUrl = apiUrl,
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
    apiUrl: String,
    mobileToken: String,
    thumbnailUrl: String,
) {
    val bitmap = rememberProductThumbnail(
        apiUrl = apiUrl,
        mobileToken = mobileToken,
        thumbnailUrl = thumbnailUrl,
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primarySoft)
            .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun rememberProductThumbnail(
    apiUrl: String,
    mobileToken: String,
    thumbnailUrl: String,
): Bitmap? {
    val resolvedUrl = resolveMobileImageUrl(apiUrl, thumbnailUrl)
    var bitmap by remember(resolvedUrl, mobileToken) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(resolvedUrl, mobileToken) {
        bitmap = null
        if (resolvedUrl != null && mobileToken.isNotBlank()) {
            bitmap = runCatching { loadMobileImageBitmap(resolvedUrl, mobileToken) }.getOrNull()
        }
    }

    return bitmap
}

private fun resolveMobileImageUrl(apiUrl: String, thumbnailUrl: String): String? {
    val trimmed = thumbnailUrl.trim()
    if (trimmed.isBlank()) {
        return null
    }
    if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
        return trimmed
    }
    if (!trimmed.startsWith("/")) {
        return null
    }

    return "${apiUrl.trim().removeSuffix("/")}$trimmed"
}

private suspend fun loadMobileImageBitmap(url: String, mobileToken: String): Bitmap? = withContext(Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8_000
        readTimeout = 8_000
        requestMethod = "GET"
        setRequestProperty("Accept", "image/*")
        setRequestProperty("Authorization", "Bearer $mobileToken")
    }

    try {
        if (connection.responseCode !in 200..299) {
            return@withContext null
        }

        connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } finally {
        connection.disconnect()
    }
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
    val boxModifier = if (editable) {
        modifier.clickable(onClick = onEdit)
    } else {
        modifier
    }
    Column(
        modifier = boxModifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.textMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (editable) {
                Text("Zmień", color = colors.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = colors.textStrong,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (note != null) {
            Spacer(Modifier.height(1.dp))
            Text(note, color = colors.textMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun MobileProductVariantRow(
    colors: DlaFlowComposeColors,
    apiUrl: String,
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
                apiUrl = apiUrl,
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
            PanelCard(colors, accent = true) {
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
                    SecondaryActionButton(colors, Icons.Rounded.Close, "Anuluj", modifier = Modifier.weight(1f), onClick = onCancel)
                    PrimaryActionButton(
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

private fun ordersSummary(total: Int, visible: Int, loading: Boolean, noAccess: Boolean): String {
    val count = total.coerceAtLeast(visible)
    val base = when {
        loading && visible == 0 -> "ładowanie listy"
        count == 1 -> "1 zamówienie"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count zamówienia"
        count > 1 -> "$count zamówień"
        else -> "lista zamówień"
    }

    return when {
        noAccess -> "$base · brak dostępu"
        loading && visible > 0 -> "$base · odświeżam"
        else -> "$base · podgląd"
    }
}

private fun orderBadgeSummary(order: MobileOrderListItem): String {
    val parts = mutableListOf<String>()
    if (order.badges.messages > 0) {
        parts.add("${order.badges.messages} wiad.")
    }
    if (order.badges.shipments > 0) {
        parts.add("${order.badges.shipments} pacz.")
    }
    if (order.badges.documents > 0) {
        parts.add("${order.badges.documents} dok.")
    }

    return parts.ifEmpty { listOf(shortTime(order.createdAt).ifBlank { "Szczegóły" }) }.joinToString(" · ")
}

private fun orderQuickInfo(order: MobileOrderListItem): String {
    val parts = mutableListOf<String>()
    if (order.shippingMethod.isNotBlank()) {
        parts.add(order.shippingMethod)
    }
    if (order.phone.isNotBlank()) {
        parts.add("tel. ${order.phone}")
    }

    return parts.ifEmpty { listOf("${order.itemCount.coerceAtLeast(1)} prod.") }.joinToString(" · ")
}

private fun orderAddressLabel(address: MobileOrderAddress): String {
    return listOf(
        address.name,
        address.company,
        address.pointName,
        address.street,
        listOf(address.postalCode, address.city).filter { it.isNotBlank() }.joinToString(" "),
        address.country,
    ).filter { it.isNotBlank() }.joinToString("\n").ifBlank { "Brak adresu" }
}

private fun orderIcon(order: MobileOrderListItem): ImageVector {
    return when {
        mobileOrderUiTone(order.statusTone) == MobileOrderUiTone.WARNING -> Icons.Rounded.Warning
        order.badges.messages > 0 -> Icons.Rounded.ChatBubbleOutline
        order.badges.shipments > 0 -> Icons.Rounded.LocalShipping
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }
}

private fun orderToneColor(colors: DlaFlowComposeColors, tone: String): Color {
    return when (mobileOrderUiTone(tone)) {
        MobileOrderUiTone.BRAND -> colors.primary
        MobileOrderUiTone.INFO -> colors.info
        MobileOrderUiTone.SUCCESS -> colors.success
        MobileOrderUiTone.WARNING -> colors.orange
        MobileOrderUiTone.NEUTRAL -> colors.textMuted
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

private fun MobileAssistantPhotoTask.toMobilePhotoTask(): MobilePhotoTask {
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
private fun PackageScannerCard(
    colors: DlaFlowComposeColors,
    scanState: MobilePackageScanUiState,
    onOpenOrder: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    PanelCard(colors, accent = scanState !is MobilePackageScanUiState.Empty) {
        Row(verticalAlignment = Alignment.Top) {
            DlaIcon(Icons.Rounded.QrCodeScanner, colors.primary, modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                when (scanState) {
                    MobilePackageScanUiState.Empty -> {
                        Text("Skaner paczek", color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(3.dp))
                        Text("Zeskanuj etykietę albo numer nadania. DlaFlow znajdzie pasujące zamówienie.", color = colors.textMuted, fontSize = 12.sp)
                    }
                    is MobilePackageScanUiState.Loading -> {
                        Text("Sprawdzam paczkę", color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(3.dp))
                        Text(scanState.code, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    is MobilePackageScanUiState.Resolved -> {
                        if (scanState.result.matched && scanState.result.order != null) {
                            Text("Paczka znaleziona", color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(3.dp))
                            Text(scanState.result.order.customer, color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(3.dp))
                            Text("#${scanState.result.order.orderNumber} · ${scanState.result.order.status}", color = colors.textMuted, fontSize = 12.sp)
                            scanState.result.shipment?.let { shipment ->
                                Spacer(Modifier.height(3.dp))
                                Text("${shipment.carrier} · ${shipment.status}", color = colors.textMuted, fontSize = 12.sp)
                            }
                        } else {
                            Text("Nie znaleziono paczki", color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                scanState.result.message.ifBlank { "Ten kod nie pasuje do żadnej paczki w DlaFlow." },
                                color = colors.textMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    is MobilePackageScanUiState.Failed -> {
                        Text("Nie udało się sprawdzić paczki", color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(3.dp))
                        Text(scanState.message, color = colors.textMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        if (scanState is MobilePackageScanUiState.Resolved && scanState.result.matched && scanState.result.order != null) {
            Spacer(Modifier.height(12.dp))
            PrimaryActionButton(colors, Icons.AutoMirrored.Rounded.ReceiptLong, "Otwórz zamówienie") {
                onOpenOrder(scanState.result.order.orderNumber)
            }
        }

        Spacer(Modifier.height(12.dp))
        SecondaryActionButton(
            colors = colors,
            icon = Icons.Rounded.QrCodeScanner,
            text = if (scanState is MobilePackageScanUiState.Empty) "Skanuj kod paczki" else "Skanuj kolejny kod",
            onClick = onScanAgain,
        )
    }
}

@Composable
private fun MessagesTab(colors: DlaFlowComposeColors, dashboard: MobileAssistantDashboard?, onOpenNotifications: () -> Unit) {
    SectionTitle(colors, "Wiadomości", "Ostatnie sprawy klienta i operacji")
    NotificationsList(colors, dashboard?.notifications.orEmpty(), onOpenNotifications)
}

@Composable
private fun MoreTab(
    colors: DlaFlowComposeColors,
    session: MobileSession,
    dashboard: MobileAssistantDashboard?,
    statusMessage: String,
    callerIdTestPhone: String,
    callerIdPreview: MobileCallerIdLookup?,
    callerIdOperational: Boolean,
    callerIdAvailable: Boolean,
    canAutoOpenTasks: Boolean,
    notificationAllowed: Boolean,
    appVersionName: String,
    appUpdate: MobileAppUpdate?,
    appUpdateChecking: Boolean,
    appUpdateDownloading: Boolean,
    appUpdateDownloadProgress: Int,
    appUpdateError: String,
    onCallerIdTestPhoneChange: (String) -> Unit,
    onEnableCallerId: () -> Unit,
    onTestCallerId: () -> Unit,
    onShowCallerIdPreview: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSystemSettings: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val callerIdLabel = dashboard?.callerIdStatus?.label ?: if (callerIdOperational) "Włączone" else "Do włączenia"
    var selectedSettingsKind by remember { mutableStateOf<MobileMoreSettingsKind?>(null) }
    val settingsItems = buildMobileMoreSettingsItems(
        appVersionName = appVersionName,
        callerIdLabel = callerIdLabel,
        canAutoOpenTasks = canAutoOpenTasks,
        updateAvailable = appUpdate != null,
    )
    val displayName = dashboard?.userName?.takeIf { it.isNotBlank() }
        ?: session.userEmail.substringBefore("@").replaceFirstChar { char -> char.uppercase(Locale("pl", "PL")) }
    val tenantName = dashboard?.tenantName?.takeIf { it.isNotBlank() } ?: session.tenantName.ifBlank { "DlaFlow" }

    BackHandler(enabled = selectedSettingsKind != null) {
        selectedSettingsKind = null
    }

    val selectedKind = selectedSettingsKind
    if (selectedKind != null) {
        val detail = buildMobileMoreSettingsDetail(
            kind = selectedKind,
            userName = displayName,
            userEmail = session.userEmail,
            tenantName = tenantName,
            deviceName = session.deviceName,
            appVersionName = appVersionName,
            callerIdLabel = callerIdLabel,
            notificationAllowed = notificationAllowed,
            canAutoOpenTasks = canAutoOpenTasks,
            updateAvailable = appUpdate != null,
        )

        MoreSettingsDetailScreen(
            colors = colors,
            detail = detail,
            callerIdTestPhone = callerIdTestPhone,
            callerIdPreview = callerIdPreview,
            appVersionName = appVersionName,
            appUpdate = appUpdate,
            appUpdateChecking = appUpdateChecking,
            appUpdateDownloading = appUpdateDownloading,
            appUpdateDownloadProgress = appUpdateDownloadProgress,
            appUpdateError = appUpdateError,
            statusMessage = statusMessage,
            callerIdAvailable = callerIdAvailable,
            callerIdOperational = callerIdOperational,
            onBack = { selectedSettingsKind = null },
            onCallerIdTestPhoneChange = onCallerIdTestPhoneChange,
            onEnableCallerId = onEnableCallerId,
            onTestCallerId = onTestCallerId,
            onShowCallerIdPreview = onShowCallerIdPreview,
            onCheckAppUpdate = onCheckAppUpdate,
            onInstallAppUpdate = onInstallAppUpdate,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onOpenAppSystemSettings = onOpenAppSystemSettings,
            onDisconnect = onDisconnect,
        )
        return
    }

    SectionTitle(colors, "Ustawienia", "Konto, telefon i aplikacja")
    MoreAccountCard(colors, session, dashboard)
    MoreSettingsList(colors, settingsItems, onSelect = { selectedSettingsKind = it })
    PanelCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DlaIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Status telefonu", color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(statusMessage.ifBlank { "Telefon działa normalnie." }, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        MoreDangerButton(colors, "Wyloguj się", onDisconnect)
    }
    Spacer(Modifier.height(92.dp))
}

@Composable
private fun MoreSettingsDetailScreen(
    colors: DlaFlowComposeColors,
    detail: MobileMoreSettingsDetail,
    callerIdTestPhone: String,
    callerIdPreview: MobileCallerIdLookup?,
    appVersionName: String,
    appUpdate: MobileAppUpdate?,
    appUpdateChecking: Boolean,
    appUpdateDownloading: Boolean,
    appUpdateDownloadProgress: Int,
    appUpdateError: String,
    statusMessage: String,
    callerIdAvailable: Boolean,
    callerIdOperational: Boolean,
    onBack: () -> Unit,
    onCallerIdTestPhoneChange: (String) -> Unit,
    onEnableCallerId: () -> Unit,
    onTestCallerId: () -> Unit,
    onShowCallerIdPreview: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSystemSettings: () -> Unit,
    onDisconnect: () -> Unit,
) {
    TextButton(
        onClick = onBack,
        colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Wróć", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
    Text(
        text = detail.title,
        color = colors.textStrong,
        fontSize = 21.sp,
        fontFamily = DlaFlowInter,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
        lineHeight = 26.sp,
    )
    Text(
        text = detail.description,
        color = colors.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 17.sp,
    )

    PanelCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DlaIcon(moreSettingsIcon(detail.kind), colors.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.title, color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(detail.description, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        detail.rows.forEachIndexed { index, row ->
            MoreDetailRow(colors, row.first, row.second)
            if (index < detail.rows.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
        if (detail.kind == MobileMoreSettingsKind.APP) {
            CompactAppSettingsActions(
                colors = colors,
                detail = detail,
                appUpdate = appUpdate,
                checking = appUpdateChecking,
                downloading = appUpdateDownloading,
                downloadProgress = appUpdateDownloadProgress,
                error = appUpdateError,
                onCheckAppUpdate = onCheckAppUpdate,
                onInstallAppUpdate = onInstallAppUpdate,
                onOpenAppSystemSettings = onOpenAppSystemSettings,
            )
        }
    }

    when (detail.kind) {
        MobileMoreSettingsKind.NOTIFICATIONS -> {
            PrimaryActionButton(colors, Icons.Rounded.NotificationsNone, "Ustawienia powiadomień", onClick = onOpenNotificationSettings)
        }
        MobileMoreSettingsKind.PREFERENCES -> {
            if (detail.primaryActionLabel != null) {
                PrimaryActionButton(colors, Icons.Rounded.Tune, detail.primaryActionLabel, onClick = onOpenOverlaySettings)
            }
        }
        MobileMoreSettingsKind.APP -> Unit
        MobileMoreSettingsKind.CALLER_ID -> {
            PanelCard(colors) {
                if (callerIdAvailable && !callerIdOperational) {
                    PrimaryActionButton(colors, Icons.Rounded.Call, "Włącz Caller ID", onClick = onEnableCallerId)
                    Spacer(Modifier.height(10.dp))
                }
                DlaFlowTextField(colors, "Numer telefonu", callerIdTestPhone, onCallerIdTestPhoneChange)
                Spacer(Modifier.height(10.dp))
                SecondaryActionButton(colors, Icons.Rounded.Call, "Sprawdź numer", onClick = onTestCallerId)
                val preview = callerIdPreview
                if (preview != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = preview.primaryOrder?.let { "${preview.displayName.ifBlank { preview.phone }} · #${it.orderNumber} · ${it.status}" }
                            ?: "Brak zamówienia dla ${preview.phone}.",
                        color = colors.textStrong,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (preview.primaryOrder != null) {
                        Spacer(Modifier.height(10.dp))
                        PrimaryActionButton(colors, Icons.Rounded.Call, "Pokaż kartę połączenia", onClick = onShowCallerIdPreview)
                    }
                }
            }
        }
        MobileMoreSettingsKind.SECURITY -> {
            PanelCard(colors) {
                Text("Odłączenie telefonu usuwa lokalną sesję i wyrejestruje urządzenie w panelu.", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                Spacer(Modifier.height(12.dp))
                MoreDangerButton(colors, detail.dangerActionLabel ?: "Odłącz telefon", onDisconnect)
            }
        }
        MobileMoreSettingsKind.ACCOUNT,
        MobileMoreSettingsKind.INTEGRATIONS,
        MobileMoreSettingsKind.TEAM -> {
            PanelCard(colors) {
                Text(
                    text = when (detail.kind) {
                        MobileMoreSettingsKind.INTEGRATIONS -> "Zmiany integracji wykonuj w panelu DlaFlow. Telefon pokazuje tutaj status połączenia z wtyczką."
                        MobileMoreSettingsKind.TEAM -> "Role, uprawnienia i skład zespołu zmieniaj w panelu. Aplikacja pokazuje tylko konto używane na tym telefonie."
                        else -> "Dane konta są pobierane z panelu DlaFlow. Edycja profilu pozostaje po stronie panelu."
                    },
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                )
            }
        }
    }

    if (detail.kind == MobileMoreSettingsKind.CALLER_ID && statusMessage.isNotBlank()) {
        StatusStrip(colors, statusMessage)
    }
    Spacer(Modifier.height(92.dp))
}

@Composable
private fun CompactAppSettingsActions(
    colors: DlaFlowComposeColors,
    detail: MobileMoreSettingsDetail,
    appUpdate: MobileAppUpdate?,
    checking: Boolean,
    downloading: Boolean,
    downloadProgress: Int,
    error: String,
    onCheckAppUpdate: () -> Unit,
    onInstallAppUpdate: () -> Unit,
    onOpenAppSystemSettings: () -> Unit,
) {
    Spacer(Modifier.height(12.dp))
    val update = appUpdate
    if (update != null) {
        Text(update.releaseTitle, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Dostępna wersja ${update.latestVersionName} · ${formatMobileUpdateBytes(update.sizeBytes)}",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(10.dp))
    }
    if (downloading) {
        MobileUpdateProgress(colors, downloadProgress)
        Spacer(Modifier.height(10.dp))
    }
    if (error.isNotBlank()) {
        Text(error, color = colors.danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
        Spacer(Modifier.height(10.dp))
    }
    val primaryLabel = when {
        downloading -> "Pobieranie..."
        checking -> "Sprawdzam..."
        update != null -> "Zaktualizuj"
        else -> detail.primaryActionLabel ?: "Sprawdź aktualizację"
    }
    PrimaryActionButton(
        colors = colors,
        icon = Icons.Rounded.Refresh,
        text = primaryLabel,
        modifier = Modifier.fillMaxWidth(),
        enabled = !checking && !downloading,
        onClick = if (update != null) onInstallAppUpdate else onCheckAppUpdate,
    )
    Spacer(Modifier.height(8.dp))
    SecondaryActionButton(
        colors = colors,
        icon = Icons.Rounded.Settings,
        text = detail.secondaryActionLabel ?: "Ustawienia systemowe",
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenAppSystemSettings,
    )
}

@Composable
private fun MoreDetailRow(colors: DlaFlowComposeColors, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = colors.textStrong,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoreAccountCard(
    colors: DlaFlowComposeColors,
    session: MobileSession,
    dashboard: MobileAssistantDashboard?,
) {
    val displayName = dashboard?.userName?.takeIf { it.isNotBlank() }
        ?: session.userEmail.substringBefore("@").replaceFirstChar { char -> char.uppercase(Locale("pl", "PL")) }
    val tenantName = dashboard?.tenantName?.takeIf { it.isNotBlank() } ?: session.tenantName.ifBlank { "DlaFlow" }

    PanelCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.primary.copy(alpha = 0.72f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.take(1).uppercase(Locale("pl", "PL")),
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = colors.textStrong,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tenantName,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = session.userEmail,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(colors, "Połączono")
        }
    }
}

@Composable
private fun MoreSettingsList(
    colors: DlaFlowComposeColors,
    items: List<MobileMoreSettingsItem>,
    onSelect: (MobileMoreSettingsKind) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
    ) {
        items.forEachIndexed { index, item ->
            MoreSettingsRow(colors, item, onClick = { onSelect(item.kind) })
            if (index < items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 58.dp)
                        .background(colors.border.copy(alpha = 0.72f)),
                )
            }
        }
    }
}

@Composable
private fun MoreSettingsRow(colors: DlaFlowComposeColors, item: MobileMoreSettingsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DlaIcon(moreSettingsIcon(item.kind), colors.primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = colors.textStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                color = colors.textMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun moreSettingsIcon(kind: MobileMoreSettingsKind): ImageVector = when (kind) {
    MobileMoreSettingsKind.ACCOUNT -> Icons.Rounded.AccountCircle
    MobileMoreSettingsKind.SECURITY -> Icons.Rounded.Security
    MobileMoreSettingsKind.NOTIFICATIONS -> Icons.Rounded.NotificationsNone
    MobileMoreSettingsKind.PREFERENCES -> Icons.Rounded.Tune
    MobileMoreSettingsKind.INTEGRATIONS -> Icons.Rounded.Settings
    MobileMoreSettingsKind.TEAM -> Icons.Rounded.Groups
    MobileMoreSettingsKind.APP -> Icons.Rounded.PhoneAndroid
    MobileMoreSettingsKind.CALLER_ID -> Icons.Rounded.Call
}

@Composable
private fun MoreDangerButton(colors: DlaFlowComposeColors, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.danger.copy(alpha = if (colors.dark) 0.14f else 0.06f),
            contentColor = colors.danger,
        ),
        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        DlaIcon(Icons.AutoMirrored.Rounded.Logout, colors.danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    DlaIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(44.dp))
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
                        SecondaryActionButton(
                            colors = colors,
                            icon = Icons.Rounded.Close,
                            text = "Później",
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                    }
                    PrimaryActionButton(
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
            NotificationBell(
                colors = colors,
                unreadCount = unreadCount,
                unreadAttentionCount = unreadAttentionCount,
                onClick = onOpenNotifications,
            )
        } else {
            StatusPill(colors, status)
        }
    }
}

@Composable
private fun GreetingRow(colors: DlaFlowComposeColors, userName: String, onRefresh: () -> Unit) {
    ScreenHeader(
        colors = colors,
        title = "Dzień dobry, ${displayFirstName(userName)}! 👋",
        subtitle = "Oto co dzieje się w Twoim sklepie",
    )
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
private fun RevenueCard(colors: DlaFlowComposeColors, dashboard: MobileAssistantDashboard?) {
    val changePercent = dashboard?.revenueChangePercent ?: 0.0
    val positive = changePercent >= 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.primaryDeep, colors.primary, colors.primaryGlow),
                    start = Offset(0f, 0f),
                    end = Offset(760f, 440f),
                ),
            )
            .padding(horizontal = 17.dp, vertical = 13.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Przychód dzisiaj",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatMoney(dashboard?.todayRevenue ?: 0.0),
                color = Color.White,
                fontSize = 21.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (positive) {
                        "↑ ${String.format(Locale.US, "%.1f", changePercent)}%"
                    } else {
                        "↓ ${String.format(Locale.US, "%.1f", kotlin.math.abs(changePercent))}%"
                    },
                    color = if (positive) Color(0xFF3CF2B1) else Color(0xFFFFB4B4),
                    fontSize = 8.5.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 11.sp,
                    maxLines = 1,
                )
                Text(
                    text = if (positive) " więcej niż wczoraj" else " mniej niż wczoraj",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 8.5.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(76.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.13f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Szczegóły", color = Color.White, fontSize = 8.5.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(1.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
        RevenueTrendChart(
            colors = colors,
            trend = dashboard?.trend.orEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(154.dp)
                .height(44.dp)
                .padding(end = 12.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun RevenueTrendChart(colors: DlaFlowComposeColors, trend: List<MobileAssistantTrendPoint>, modifier: Modifier = Modifier) {
    val points = revenueSparklinePoints(trend)

    Canvas(modifier = modifier) {
        val topInset = 3.dp.toPx()
        val bottomInset = 7.dp.toPx()
        val drawingHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)
        val baselineY = size.height - 3.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx(),
        )
        if (points.isEmpty()) {
            return@Canvas
        }
        val step = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = if (points.size == 1) size.width else step * index
            val y = topInset + drawingHeight - (drawingHeight * value.toFloat())
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val previousX = step * (index - 1)
                val previousY = topInset + drawingHeight - (drawingHeight * points[index - 1].toFloat())
                val controlOffset = step * 0.45f
                path.cubicTo(previousX + controlOffset, previousY, x - controlOffset, y, x, y)
            }
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 1.55.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White,
            radius = 2.35.dp.toPx(),
            center = Offset(
                if (points.size == 1) size.width else step * (points.size - 1),
                topInset + drawingHeight - (drawingHeight * points.last().toFloat()),
            ),
        )
    }
}

private fun revenueSparklinePoints(trend: List<MobileAssistantTrendPoint>): List<Double> {
    val values = trend.takeLast(14).map { it.revenue.coerceAtLeast(0.0) }

    if (values.isEmpty() || values.all { it == 0.0 }) {
        return emptyList()
    }

    val min = values.minOrNull() ?: return emptyList()
    val max = values.maxOrNull() ?: return emptyList()
    if (max <= min) {
        return values.map { 0.56 }
    }

    return values.map { value ->
        val normalized = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        (0.18 + sqrt(normalized) * 0.56).coerceIn(0.18, 0.74)
    }
}

@Composable
private fun KpiGrid(colors: DlaFlowComposeColors, dashboard: MobileAssistantDashboard?) {
    val kpis = dashboard?.kpis ?: MobileAssistantKpis(0, 0, 0, 0)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        KpiTile(colors, "Nowe zamówienia", kpis.newOrders.toString(), Icons.Rounded.ShoppingCart, colors.primary, Modifier.weight(1f))
        KpiTile(colors, "Do wysyłki", kpis.toShip.toString(), Icons.Rounded.LocalShipping, colors.orange, Modifier.weight(1f))
        KpiTile(colors, "Po terminie", kpis.overdueOrProblems.toString(), Icons.Rounded.Inventory2, colors.success, Modifier.weight(1f))
        KpiTile(colors, "Wiadomości", kpis.messages.toString(), Icons.Rounded.ChatBubbleOutline, colors.info, Modifier.weight(1f))
    }
}

@Composable
private fun KpiTile(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(98.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
    ) {
        DlaIcon(icon, iconColor, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = colors.textStrong, fontSize = 18.5.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = colors.textMuted, fontSize = 8.4.sp, fontWeight = FontWeight.SemiBold, lineHeight = 9.2.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickActions(colors: DlaFlowComposeColors, onQuickAction: (MobileAssistantQuickAction) -> Unit) {
    Text(
        text = "Szybkie akcje",
        color = colors.textStrong,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 15.5.sp,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        QuickActionButton(colors, "Skanuj paczkę", "Szybkie nadanie", Icons.Rounded.QrCodeScanner, colors.primary, Modifier.weight(1f)) { onQuickAction(MobileAssistantQuickAction.SCAN_PACKAGE) }
        QuickActionButton(colors, "Dodaj produkt", "Nowa oferta", Icons.Rounded.AddBox, colors.success, Modifier.weight(1f)) { onQuickAction(MobileAssistantQuickAction.ADD_PRODUCT) }
        QuickActionButton(colors, "Statystyki", "Zobacz wyniki", Icons.AutoMirrored.Rounded.ShowChart, colors.orange, Modifier.weight(1f)) { onQuickAction(MobileAssistantQuickAction.STATS) }
        QuickActionButton(colors, "Produkty", "Zarządzaj", Icons.Rounded.Inventory2, colors.info, Modifier.weight(1f)) { onQuickAction(MobileAssistantQuickAction.PRODUCTS) }
    }
}

@Composable
private fun QuickActionButton(
    colors: DlaFlowComposeColors,
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surface,
            contentColor = colors.textStrong,
        ),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.76f)),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DlaIcon(icon, iconColor, modifier = Modifier.size(37.dp))
            Spacer(Modifier.height(7.dp))
            Text(label, color = colors.textStrong, fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 9.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.textMuted, fontSize = 7.4.sp, fontWeight = FontWeight.SemiBold, lineHeight = 8.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    PanelCard(colors, accent = highlighted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                DlaIcon(Icons.Rounded.PhotoCamera, colors.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Zadanie zdjęciowe", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(task.productName, color = colors.textStrong, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (task.productSku.isNotBlank()) {
                    Text("SKU: ${task.productSku}", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(colors, task.mediaCount, task.maxPhotos)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryActionButton(colors, Icons.Rounded.PhotoCamera, "Zrób", modifier = Modifier.weight(1f)) { onTakePhoto(task.id) }
            SecondaryActionButton(colors, Icons.Rounded.PhotoLibrary, "Wybierz", modifier = Modifier.weight(1f)) { onPickPhoto(task.id) }
        }
        Spacer(Modifier.height(10.dp))
        SecondaryActionButton(colors, Icons.Rounded.CheckCircle, "Zakończ zadanie") { onCompletePhotoTask(task.id) }
    }
}

@Composable
private fun ProgressLine(colors: DlaFlowComposeColors, current: Int, max: Int) {
    val safeMax = max.coerceAtLeast(1)
    val ratio = current.coerceIn(0, safeMax).toFloat() / safeMax.toFloat()
    Column {
        Row {
            Text("Zdjęcia", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("$current/$max", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.borderSubtle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary),
            )
        }
    }
}

@Composable
private fun NotificationsList(colors: DlaFlowComposeColors, notifications: List<MobileAssistantNotification>, onOpenNotifications: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ostatnie powiadomienia", color = colors.textStrong, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenNotifications() }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Zobacz wszystkie", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (notifications.isEmpty()) {
            NotificationEmptyRow(colors)
            return
        }
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
private fun NotificationsScreen(
    colors: DlaFlowComposeColors,
    notifications: List<MobileAssistantNotification>,
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
            ScreenHeader(colors, "Powiadomienia", "Sprawy z panelu i telefonu")
        }
        NotificationFilterTabs(colors, selectedFilter, onFilterChange)
        val visible = filterNotifications(notifications, selectedFilter)
        PanelCard(colors, accent = visible.any { toneColorKey(it.tone) == "attention" }) {
            if (loading && notifications.isEmpty()) {
                NotificationEmptyRow(colors, "Ładujemy powiadomienia", "Za chwilę pokażemy najnowsze sprawy z panelu.")
                return@PanelCard
            }
            if (visible.isEmpty()) {
                NotificationEmptyRow(colors)
                return@PanelCard
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
        SecondaryActionButton(
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
private fun NotificationRow(colors: DlaFlowComposeColors, notification: MobileAssistantNotification) {
    val color = toneColor(colors, notification.tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NotificationToneIcon(notificationIcon(notification), color)
        Spacer(Modifier.width(11.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
        ) {
            Text(
                notification.title,
                color = colors.textStrong,
                fontSize = 10.4.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                notification.description,
                color = colors.textMuted,
                fontSize = 8.3.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 10.3.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .height(42.dp)
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(6.5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(relativeTime(notification.occurredAt), color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun NotificationToneIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.5.dp),
        )
    }
}

@Composable
private fun NotificationEmptyRow(
    colors: DlaFlowComposeColors,
    title: String = "Brak pilnych spraw",
    subtitle: String = "Gdy pojawi się wiadomość albo problem, zobaczysz go tutaj.",
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DlaIcon(Icons.Rounded.CheckCircle, colors.success, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = colors.textMuted, fontSize = 11.sp, maxLines = 2, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun SectionTitle(colors: DlaFlowComposeColors, title: String, subtitle: String) {
    ScreenHeader(colors, title, subtitle)
}

@Composable
private fun ScreenHeader(colors: DlaFlowComposeColors, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.textStrong,
            fontSize = 16.5.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 10.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PanelCard(colors: DlaFlowComposeColors, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, if (accent) colors.primarySoftBorder else colors.border, RoundedCornerShape(8.dp))
            .padding(14.dp),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DlaFlowTextField(colors: DlaFlowComposeColors, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
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
}

@Composable
private fun PrimaryActionButton(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
        modifier = modifier.height(48.dp),
    ) {
        DlaIcon(icon, Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecondaryActionButton(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primarySoft, contentColor = colors.primary),
        modifier = modifier.height(48.dp),
    ) {
        DlaIcon(icon, colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DlaIcon(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StatusPill(colors: DlaFlowComposeColors, text: String) {
    Text(
        text = text,
        color = colors.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.primarySoft)
            .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
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
private fun StatusStrip(colors: DlaFlowComposeColors, message: String) {
    if (message.isBlank()) {
        return
    }
    Text(
        text = message,
        color = colors.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp),
    )
}

@Composable
private fun KeyValue(colors: DlaFlowComposeColors, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BottomNavigation(
    colors: DlaFlowComposeColors,
    selectedTab: MobileAssistantTab,
    dashboard: MobileAssistantDashboard?,
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

private fun navBadge(tab: MobileAssistantTab, dashboard: MobileAssistantDashboard?): Int {
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

private fun displayFirstName(value: String): String {
    val name = displayName(value)
    return name.substringBefore(" ").ifBlank { "Maciek" }
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

private fun notificationIcon(notification: MobileAssistantNotification): ImageVector {
    val text = "${notification.title} ${notification.description}".lowercase(Locale.ROOT)
    return when {
        "nowe zamówienie" in text || "nowe zamowienie" in text -> Icons.Rounded.ShoppingCart
        "wiadomo" in text || "klient" in text -> Icons.Rounded.ChatBubbleOutline
        "wysy" in text || "pacz" in text -> Icons.Rounded.LocalShipping
        "problem" in text || "błąd" in text || "blad" in text -> Icons.Rounded.Warning
        else -> Icons.Rounded.ShoppingCart
    }
}

private fun toneColor(colors: DlaFlowComposeColors, tone: String): Color {
    return when (tone.lowercase(Locale.ROOT)) {
        "error" -> colors.danger
        "success" -> colors.success
        "warning" -> colors.warning
        else -> colors.primary
    }
}

private data class DlaFlowComposeColors(
    val dark: Boolean,
    val appBg: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val textStrong: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primaryGlow: Color,
    val primarySoft: Color,
    val primarySoftBorder: Color,
    val info: Color,
    val success: Color,
    val orange: Color,
    val warning: Color,
    val danger: Color,
    val material: androidx.compose.material3.ColorScheme,
)

@Composable
private fun dlaFlowColors(dark: Boolean): DlaFlowComposeColors {
    val material = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF9B83FF),
            background = Color(0xFF0F131D),
            surface = Color(0xFF171C27),
            onPrimary = Color.White,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF7B5CF6),
            background = Color(0xFFF8F9FC),
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
        )
    }

    return if (dark) {
        DlaFlowComposeColors(
            dark = true,
            appBg = Color(0xFF0F131D),
            surface = Color(0xFF171C27),
            surfaceSubtle = Color(0xFF151A24),
            border = Color(0xFF2A3342),
            borderSubtle = Color(0xFF202735),
            textStrong = Color(0xFFF8FAFC),
            text = Color(0xFFD7DEEA),
            textMuted = Color(0xFF9AA7BA),
            primary = Color(0xFF9B83FF),
            primaryDeep = Color(0xFF5F47D8),
            primaryGlow = Color(0xFFA78BFA),
            primarySoft = Color(0x297B5CF6),
            primarySoftBorder = Color(0x669B83FF),
            info = Color(0xFF60A5FA),
            success = Color(0xFF5EEAD4),
            orange = Color(0xFFF59E0B),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
            material = material,
        )
    } else {
        DlaFlowComposeColors(
            dark = false,
            appBg = Color.White,
            surface = Color.White,
            surfaceSubtle = Color(0xFFFBFCFE),
            border = Color(0xFFDFE4EC),
            borderSubtle = Color(0xFFEDF0F5),
            textStrong = Color(0xFF0F172A),
            text = Color(0xFF334155),
            textMuted = Color(0xFF64748B),
            primary = Color(0xFF7B5CF6),
            primaryDeep = Color(0xFF5B3FE0),
            primaryGlow = Color(0xFFA78BFA),
            primarySoft = Color(0xFFF1ECFF),
            primarySoftBorder = Color(0xFFE4DCFF),
            info = Color(0xFF2563EB),
            success = Color(0xFF0B8F78),
            orange = Color(0xFFF97316),
            warning = Color(0xFFC2410C),
            danger = Color(0xFFDC2626),
            material = material,
        )
    }
}
