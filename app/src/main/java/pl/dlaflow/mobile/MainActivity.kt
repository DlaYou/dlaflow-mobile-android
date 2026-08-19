package pl.dlaflow.mobile

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.doOnPreDraw
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import pl.dlaflow.mobile.app.navigation.MobileAssistantOverlayScreen
import pl.dlaflow.mobile.app.navigation.MobileAssistantTab
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.feature.dashboard.DashboardAction
import pl.dlaflow.mobile.feature.dashboard.DashboardCoordinator
import pl.dlaflow.mobile.feature.dashboard.DashboardFeedback
import pl.dlaflow.mobile.feature.dashboard.DashboardGateway
import pl.dlaflow.mobile.feature.dashboard.DashboardStateHolder
import pl.dlaflow.mobile.feature.dashboard.contentOrNull
import pl.dlaflow.mobile.feature.orders.MobileApiOrdersGateway
import pl.dlaflow.mobile.feature.orders.OrdersAction
import pl.dlaflow.mobile.feature.orders.OrdersCoordinator
import pl.dlaflow.mobile.feature.orders.OrdersFeedback
import pl.dlaflow.mobile.feature.orders.OrdersLoadOperation
import pl.dlaflow.mobile.feature.orders.OrdersQuery
import pl.dlaflow.mobile.feature.orders.OrdersRoute
import pl.dlaflow.mobile.feature.orders.OrdersStateHolder
import pl.dlaflow.mobile.feature.products.MobileApiPhotoTasksGateway
import pl.dlaflow.mobile.feature.products.MobileApiProductsGateway
import pl.dlaflow.mobile.feature.products.PhotoTasksAction
import pl.dlaflow.mobile.feature.products.PhotoTasksCoordinator
import pl.dlaflow.mobile.feature.products.PhotoTasksEffect
import pl.dlaflow.mobile.feature.products.PhotoTasksStateHolder
import pl.dlaflow.mobile.feature.products.PhotoTaskStatus
import pl.dlaflow.mobile.feature.products.ProductPhotoTask
import pl.dlaflow.mobile.feature.products.ProductsAction
import pl.dlaflow.mobile.feature.products.ProductsCoordinator
import pl.dlaflow.mobile.feature.products.ProductsFeedback
import pl.dlaflow.mobile.feature.products.ProductsOperation
import pl.dlaflow.mobile.feature.products.ProductsScheduledTask
import pl.dlaflow.mobile.feature.products.ProductsSearchScheduler
import pl.dlaflow.mobile.feature.products.ProductsStateHolder
import pl.dlaflow.mobile.feature.products.choosePhotoTaskFocus
import pl.dlaflow.mobile.feature.pairing.PairingCoordinator
import pl.dlaflow.mobile.feature.pairing.PairingGateway
import pl.dlaflow.mobile.feature.pairing.PairingStateHolder
import pl.dlaflow.mobile.feature.pairing.pairingSmokeSeed
import pl.dlaflow.mobile.feature.products.PhotoCaptureKind
import pl.dlaflow.mobile.feature.products.PhotoCaptureStateHolder
import pl.dlaflow.mobile.feature.products.orderedTasks
import pl.dlaflow.mobile.feature.notifications.MobileApiNotificationsGateway
import pl.dlaflow.mobile.feature.notifications.NotificationDestination
import pl.dlaflow.mobile.feature.notifications.NotificationFilter
import pl.dlaflow.mobile.feature.notifications.NotificationItem
import pl.dlaflow.mobile.feature.notifications.NotificationTone
import pl.dlaflow.mobile.feature.notifications.NotificationsCoordinator
import pl.dlaflow.mobile.feature.notifications.NotificationsEffect
import pl.dlaflow.mobile.feature.notifications.NotificationsStateHolder
import pl.dlaflow.mobile.feature.notifications.canonicalContentOrNull
import pl.dlaflow.mobile.feature.scanner.MobileApiScannerGateway
import pl.dlaflow.mobile.feature.scanner.ScannerAction
import pl.dlaflow.mobile.feature.scanner.ScannerCoordinator
import pl.dlaflow.mobile.feature.scanner.ScannerFeedback
import pl.dlaflow.mobile.feature.scanner.ScannerStateHolder
import pl.dlaflow.mobile.feature.settings.SettingsAction
import pl.dlaflow.mobile.feature.settings.SettingsCallerIdLookupRequest
import pl.dlaflow.mobile.feature.settings.SettingsCallerIdOrder
import pl.dlaflow.mobile.feature.settings.SettingsCallerIdPreview
import pl.dlaflow.mobile.feature.settings.SettingsCoordinator
import pl.dlaflow.mobile.feature.settings.SettingsDisconnectRequest
import pl.dlaflow.mobile.feature.settings.SettingsEffect
import pl.dlaflow.mobile.feature.settings.SettingsInput
import pl.dlaflow.mobile.feature.settings.SettingsNotificationPreference
import pl.dlaflow.mobile.feature.settings.SettingsStateHolder
import pl.dlaflow.mobile.feature.settings.SettingsTextResolver
import pl.dlaflow.mobile.feature.settings.SettingsUpdateInfo
import pl.dlaflow.mobile.feature.settings.SettingsUpdateOperation
import pl.dlaflow.mobile.feature.settings.buildSettingsContent
import pl.dlaflow.mobile.feature.settings.launchFirstResolvedSettingsTarget
import pl.dlaflow.mobile.feature.settings.normalizeSettingsCallerIdPhone
import pl.dlaflow.mobile.feature.settings.settingsSignerSetsMatch

class MainActivity : ComponentActivity() {
    private val cameraPermissionRequestCode = 4103
    private val callerIdRoleRequestCode = 4104
    private val notificationPermissionRequestCode = 4105
    private val phoneStatePermissionRequestCode = 4106
    private val executor = Executors.newSingleThreadExecutor()
    private val dispatchHandler = Handler(Looper.getMainLooper())
    private val dispatchRunnable = object : Runnable {
        override fun run() {
            checkPhotoTaskDispatch()
            dispatchHandler.postDelayed(this, dispatchPollIntervalMs)
        }
    }
    private lateinit var sessionStore: MobileSessionStore
    private val settingsStateHolder = SettingsStateHolder()
    private val settingsCoordinator by lazy {
        SettingsCoordinator(settingsStateHolder, ::handleSettingsEffect)
    }
    private var settingsSessionEpoch = 0L
    private var nextCallerIdLookupRequestId = 0L
    private var activeCallerIdLookupRequest: SettingsCallerIdLookupRequest? = null
    private val settingsLifecycleId = System.nanoTime()
    private var nextSettingsUpdateOperationId = 0L
    private var activeSettingsUpdateOperation: SettingsUpdateOperation? = null
    private val scannerStateHolder = ScannerStateHolder()
    private val scannerCoordinator by lazy {
        ScannerCoordinator(
            stateHolder = scannerStateHolder,
            gateway = MobileApiScannerGateway { mobileApiClientForSession(sessionStore) },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onFeedback = ::handleScannerFeedback,
            onRequestCapture = ::scanPackageCode,
            onOpenOrder = { orderNumber ->
                selectedTab = MobileAssistantTab.ORDERS
                handleOrdersAction(OrdersAction.OpenOrder(orderNumber))
            },
            onUnauthorized = ::handleScannerUnauthorized,
        )
    }
    private val dashboardStateHolder = DashboardStateHolder()
    private val dashboardCoordinator by lazy {
        DashboardCoordinator(
            stateHolder = dashboardStateHolder,
            gateway = DashboardGateway { token ->
                mobileApiClientForSession(sessionStore).getAssistantDashboard(token)
            },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onFeedback = ::handleDashboardFeedback,
            onUnauthorized = ::handleDashboardUnauthorized,
        )
    }
    private val ordersStateHolder = OrdersStateHolder()
    private val ordersCoordinator by lazy {
        OrdersCoordinator(
            stateHolder = ordersStateHolder,
            gateway = MobileApiOrdersGateway {
                mobileApiClientForSession(sessionStore)
            },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onFeedback = ::handleOrdersFeedback,
            onUnauthorized = ::handleOrdersUnauthorized,
        )
    }
    private val pairingStateHolder = PairingStateHolder()
    private val photoCaptureStateHolder = PhotoCaptureStateHolder()
    private val notificationsStateHolder = NotificationsStateHolder()
    private val notificationsCoordinator by lazy {
        NotificationsCoordinator(
            stateHolder = notificationsStateHolder,
            gateway = MobileApiNotificationsGateway { mobileApiClientForSession(sessionStore) },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onEffect = ::handleNotificationsEffect,
            onReadStateChanged = {
                session?.token?.let { token -> dashboardCoordinator.refresh(token, showFeedback = false) }
            },
            onUnauthorized = ::handleNotificationsUnauthorized,
            onStateChanged = {
                syncNotificationsUiState()
                render()
                if (markNotificationsReadOnOpen && notificationsStateHolder.state.canonicalContentOrNull() != null) {
                    markNotificationsReadOnOpen = false
                    markVisibleNotificationsRead()
                }
            },
        )
    }
    private val pairingCoordinator by lazy {
        PairingCoordinator(
            stateHolder = pairingStateHolder,
            gateway = PairingGateway { baseUrl, submission ->
                mobileApiClientForBaseUrl(baseUrl).completePairing(submission.code, submission.deviceName)
            },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onStarted = { showSessionTransition(activeStepIndex = 0, progress = 18) },
            onSuccess = ::handlePairingSuccess,
            onFailure = { hideSessionTransition() },
        )
    }
    private lateinit var root: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var contentView: View
    private lateinit var screenView: FrameLayout
    private lateinit var statusView: TextView
    private lateinit var callerIdTestPhoneInput: EditText
    private var sessionTransitionOverlay: DlaFlowSessionTransitionOverlay? = null
    private var sessionTransitionStartedAt: Long = 0L
    private var callerIdPreview by mutableStateOf<MobileCallerIdLookup?>(null)
    private var focusedPhotoTaskId: String? = null
    private var focusedPhotoTaskView: View? = null
    private var photoTasks by mutableStateOf<List<MobilePhotoTask>>(emptyList())
    private var apiUrlValue by mutableStateOf("")
    private var callerIdTestPhoneValue by mutableStateOf("")
    private var statusMessage by mutableStateOf("")
    private var selectedTab by mutableStateOf(MobileAssistantTab.DASHBOARD)
    private var mobileProducts by mutableStateOf<List<MobileProduct>>(emptyList())
    private var mobileProductsNextCursor by mutableStateOf<String?>(null)
    private var mobileProductsTotal by mutableStateOf(0)
    private var mobileProductsLoading by mutableStateOf(false)
    private var mobileProductsSearch by mutableStateOf("")
    private var mobileProductsFilter by mutableStateOf(MobileProductFilter.ALL)
    private var mobileProductVariants by mutableStateOf<Map<String, List<MobileProductVariant>>>(emptyMap())
    private var mobileProductVariantsLoading by mutableStateOf<Set<String>>(emptySet())
    private var mobileProductsReadOnly by mutableStateOf(false)
    private var mobileProductsNoAccess by mutableStateOf(false)
    private var mobileOverlayScreen by mutableStateOf(MobileAssistantOverlayScreen.NONE)
    private var mobileNotifications by mutableStateOf<List<MobileAssistantNotification>>(emptyList())
    private var mobileNotificationsLoading by mutableStateOf(false)
    private var mobileNotificationFilter by mutableStateOf(MobileNotificationFilter.ALL)
    private var markNotificationsReadOnOpen = false
    private var notificationPreferences by mutableStateOf(MobileNotificationPreferences.defaults())
    private var hostRenderVersion by mutableStateOf(0)
    private var appUpdate by mutableStateOf<MobileAppUpdate?>(null)
    private var appUpdateDismissalState by mutableStateOf(MobileAppUpdateDismissalState())
    private var appUpdateDialogVisible by mutableStateOf(false)
    private var appUpdateChecking by mutableStateOf(false)
    private var appUpdateDownloading by mutableStateOf(false)
    private var appUpdateDownloadProgress by mutableStateOf(0)
    private var appUpdateError by mutableStateOf("")
    private var dismissedAppUpdateVersionInRuntime: Int? = null
    private var pendingInstallApkFile: File? = null
    private var pendingInstallUpdate: MobileAppUpdate? = null
    private var mobileProductsRequestVersion = 0
    private var mobileProductsStateVersion = 0
    private var pendingQrScanMode = QrScanMode.PAIRING
    private var pendingCameraPhotoFile: File? = null
    private var pendingCameraPhotoUri: Uri? = null
    private var pendingPhotoTaskId: String? = null
    private var pendingPhotoResultRequestCode: Int? = null
    private var pendingPhotoResultSourceId: String? = null
    private var nextPhotoResultRequestCode = photoResultRequestCodeMin
    private var pendingSmokeApiUrl: String? = null
    private var pendingSmokePairingCode: String? = null
    private var pendingSmokePairingDeviceName: String? = null
    private var contentReadyForDisplay = false
    private var keepSystemSplashVisible = true
    private var initialContentStarted = false
    private var startupHasSavedSession = false
    private var session by mutableStateOf<MobileSession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplashVisible }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        super.onCreate(savedInstanceState)
        showSessionTransitionShell()
        showSessionTransition(activeStepIndex = 0, progress = 18, animateIn = false)
        releaseSystemSplash()
        dispatchHandler.postDelayed(::releaseSystemSplash, systemSplashFallbackDelayMs)
        screenView.doOnPreDraw {
            if (!initialContentStarted) {
                initialContentStarted = true
                dispatchHandler.post(::startInitialContent)
            }
        }
    }

    private fun startInitialContent() {
        if (!::sessionStore.isInitialized) {
            sessionStore = MobileSessionStore(this)
            apiUrlValue = sessionStore.readBaseUrl()
            notificationPreferences = sessionStore.readNotificationPreferences()
            DlaFlowNotifications.ensureChannels(this)
            handleLaunchIntent(intent)
            startupHasSavedSession = sessionStore.readToken().isNotBlank()
        }
        releaseSystemSplash()
        render()
        if (consumeSmokePairingIntent()) {
            return
        } else if (startupHasSavedSession) {
            verifySavedSession(showInitialTransition = false)
        } else {
            setStatus("Przygotowujemy aplikację...")
            completeSessionTransition { setStatus("") }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
        render()
        if (consumeSmokePairingIntent()) {
            return
        }
        session?.token?.let(photoTasksCoordinator::refresh)
        if (selectedTab == MobileAssistantTab.ORDERS) {
            ensureOrdersLoaded()
        }
    }

    override fun onDestroy() {
        stopPhotoTaskDispatchPolling()
        photoTasksCoordinator.reset()
        productsCoordinator.reset()
        notificationsCoordinator.reset()
        settingsCoordinator.reset()
        activeCallerIdLookupRequest = null
        activeSettingsUpdateOperation = null
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (!::sessionStore.isInitialized) {
            return
        }
        render()
        val pendingFile = pendingInstallApkFile
        val pendingUpdate = pendingInstallUpdate
        if (pendingFile != null && pendingUpdate != null && canInstallMobileUpdates()) {
            pendingInstallApkFile = null
            pendingInstallUpdate = null
            openMobileUpdateInstaller(pendingFile, pendingUpdate)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val pairingScan = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (pairingScan != null) {
            if (pendingQrScanMode == QrScanMode.PACKAGE) {
                handlePackageScanCapture(pairingScan.contents)
            } else {
                handlePairingQrResult(pairingScan.contents)
            }
            pendingQrScanMode = QrScanMode.PAIRING
            return
        }

        if (requestCode == callerIdRoleRequestCode) {
            render()
            setStatus(
                when {
                    isCallerIdOperational() -> "Caller ID włączony."
                    isCallerIdRoleHeld() -> callerIdMissingPermissionMessage(
                        needsPhoneState = !hasPhoneStatePermission(),
                        needsContacts = !hasContactsPermission(),
                    )
                    else -> "Caller ID nie jest jeszcze włączony w systemie."
                },
            )
            return
        }

        if (requestCode != pendingPhotoResultRequestCode) {
            return
        }

        val capture = photoCaptureStateHolder.pending
        if (
            capture == null ||
            capture.sourceId != pendingPhotoResultSourceId ||
            capture.sessionKey != session?.token ||
            capture.taskId != pendingPhotoTaskId
        ) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Sesja telefonu zmieniła się. Wybierz zdjęcie ponownie.")
            return
        }

        if (resultCode != RESULT_OK) {
            setStatus("Nie wybrano zdjęcia.")
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            return
        }

        val taskId = pendingPhotoTaskId ?: return
        when (capture.kind) {
            PhotoCaptureKind.CAMERA -> uploadCameraResult(taskId)
            PhotoCaptureKind.GALLERY -> uploadGalleryResult(taskId, data?.data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraPermissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingPhotoTaskId?.let { openCamera(it) }
        } else if (requestCode == cameraPermissionRequestCode) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Aparat nie ma zgody. Możesz wybrać zdjęcie z telefonu.")
        } else if (requestCode == notificationPermissionRequestCode) {
            setStatus(if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) "Powiadomienia zadań włączone." else "Bez powiadomień otwórz aplikację, żeby zobaczyć zadania.")
        } else if (requestCode == phoneStatePermissionRequestCode) {
            if (hasCallerIdRuntimePermissions()) {
                requestCallerIdRole()
            } else {
                setStatus(
                    callerIdMissingPermissionMessage(
                        needsPhoneState = !hasPhoneStatePermission(),
                        needsContacts = !hasContactsPermission(),
                    ),
                )
                render()
            }
        }
    }
    private val productsStateHolder = ProductsStateHolder()
    private val productsSearchScheduler = ProductsSearchScheduler { delayMillis, action ->
        val runnable = Runnable(action)
        dispatchHandler.postDelayed(runnable, delayMillis)
        ProductsScheduledTask { dispatchHandler.removeCallbacks(runnable) }
    }
    private val productsCoordinator by lazy {
        ProductsCoordinator(
            stateHolder = productsStateHolder,
            gateway = MobileApiProductsGateway { mobileApiClientForSession(sessionStore) },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            searchScheduler = productsSearchScheduler,
            onFeedback = ::handleProductsFeedback,
            onUnauthorized = ::handleProductsUnauthorized,
        )
    }
    private val photoTasksStateHolder = PhotoTasksStateHolder()
    private val photoTasksCoordinator by lazy {
        PhotoTasksCoordinator(
            stateHolder = photoTasksStateHolder,
            gateway = MobileApiPhotoTasksGateway { mobileApiClientForSession(sessionStore) },
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            onEffect = ::handlePhotoTasksEffect,
            onUnauthorized = ::handlePhotoTasksUnauthorized,
            onTasksChanged = {
                syncPhotoTasksUiState()
                render()
            },
        )
    }

    private fun replaceSettingsSession() {
        settingsSessionEpoch += 1L
        settingsCoordinator.replaceSession(settingsSessionEpoch)
        activeCallerIdLookupRequest = null
        callerIdPreview = null
        clearPendingCameraPhoto()
        clearAppUpdateState()
    }

    private fun settingsContent() = buildSettingsContent(
        SettingsInput(
            displayName = dashboardStateHolder.state.contentOrNull()?.userName.orEmpty(),
            userEmail = session?.userEmail.orEmpty(),
            tenantName = dashboardStateHolder.state.contentOrNull()?.tenantName ?: session?.tenantName.orEmpty(),
            deviceName = session?.deviceName.orEmpty(),
            phoneStatusMessage = statusMessage,
            callerIdLabel = dashboardStateHolder.state.contentOrNull()?.callerIdStatus?.label
                ?: getString(if (isCallerIdOperational()) R.string.settings_value_enabled else R.string.settings_caller_id_fallback),
            callerIdPreview = callerIdPreview?.let { preview ->
                SettingsCallerIdPreview(
                    displayName = preview.displayName,
                    phone = preview.phone,
                    primaryOrder = preview.primaryOrder?.let { order -> SettingsCallerIdOrder(order.orderNumber, order.status) },
                )
            },
            callerIdAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isCallerIdRoleAvailable(),
            callerIdOperational = isCallerIdOperational(),
            canAutoOpenTasks = canDrawOverOtherApps(),
            notificationAllowed = areNotificationsAllowed(),
            appVersionName = currentAppVersionName(),
            update = appUpdate?.let { SettingsUpdateInfo(it.releaseTitle, it.latestVersionName, it.sizeBytes.toLong()) },
            updateChecking = appUpdateChecking,
            updateDownloading = appUpdateDownloading,
            updateDownloadProgress = appUpdateDownloadProgress,
            updateError = appUpdateError,
            textResolver = SettingsTextResolver { resourceId, arguments -> getString(resourceId, *arguments) },
            notificationPreferenceSummary = mobileNotificationPreferenceSummary(notificationPreferences),
            notificationPreferences = MobileNotificationCategory.entries.map { category ->
                SettingsNotificationPreference(category.key, category.label, category.description, notificationPreferences.isEnabled(category))
            },
        ),
    ).also {
        // Keep lifecycle refreshes on the existing Compose root instead of replacing the view.
        hostRenderVersion.let { }
    }

    private fun handleSettingsAction(action: SettingsAction) {
        settingsCoordinator.onAction(action, settingsContent())
    }

    private fun handleSettingsEffect(effect: SettingsEffect) {
        when (effect) {
            is SettingsEffect.CallerIdPhoneChanged -> {
                activeCallerIdLookupRequest = null
                callerIdPreview = null
            }
            SettingsEffect.EnableCallerId -> requestCallerIdRole()
            is SettingsEffect.TestCallerId -> testCallerIdLookup(effect.phone)
            SettingsEffect.ShowCallerIdPreview -> callerIdPreview?.let { preview ->
                runCatching { startActivity(CallerIdActivity.createIntent(this, preview)) }
                    .onFailure { setStatus(getString(R.string.settings_host_caller_card_open_failed)) }
            }
            SettingsEffect.CheckAppUpdate -> refreshAppUpdate(showStatus = true)
            SettingsEffect.InstallAppUpdate -> installAppUpdate()
            SettingsEffect.OpenNotificationSettings -> openNotificationSettings()
            SettingsEffect.OpenOverlaySettings -> requestOverlayPermission()
            SettingsEffect.OpenAppSystemSettings -> openAppSystemSettings()
            is SettingsEffect.NotificationPreferenceChanged -> {
                val category = MobileNotificationCategory.entries.firstOrNull { it.key == effect.key } ?: return
                notificationPreferences = notificationPreferences.withEnabled(category, effect.enabled)
                sessionStore.saveNotificationPreferences(notificationPreferences)
            }
            is SettingsEffect.Disconnect -> disconnectLocalPhone(effect.request)
        }
    }

    private fun render() {
        val theme = mobileTheme()
        window.statusBarColor = theme.appBg
        window.navigationBarColor = theme.appBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (theme.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (::contentView.isInitialized) {
            hostRenderVersion += 1
            return
        }
        focusedPhotoTaskView = null
        val composeView = ComposeView(this).apply {
            alpha = if (contentReadyForDisplay) 1f else 0f
            fitsSystemWindows = true
            setBackgroundColor(theme.appBg)
            setContent {
                MobileAssistantScreen(
                    session = session,
                    dashboardState = dashboardStateHolder.state,
                    photoTasks = orderedPhotoTasks(),
                    scannerState = scannerStateHolder.state,
                    statusMessage = statusMessage,
                    selectedTab = selectedTab,
                    apiUrl = apiUrlValue.ifBlank { sessionStore.readBaseUrl() },
                    pairingState = pairingStateHolder.state,
                    settingsState = settingsStateHolder.state,
                    settingsContent = settingsContent(),
                    appVersionName = currentAppVersionName(),
                    appUpdate = appUpdate,
                    appUpdateDialogVisible = appUpdateDialogVisible,
                    appUpdateBlocking = mobileAppUpdateIsBlocking(appUpdate, appUpdateDismissalState),
                    appUpdateDismissalsRemaining = mobileAppUpdateDismissalsRemaining(appUpdate, appUpdateDismissalState),
                    appUpdateChecking = appUpdateChecking,
                    appUpdateDownloading = appUpdateDownloading,
                    appUpdateDownloadProgress = appUpdateDownloadProgress,
                    appUpdateError = appUpdateError,
                    ordersState = ordersStateHolder.state,
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
                    onPairingCodeChange = pairingStateHolder::updateCode,
                    onContinuePairing = { pairingStateHolder.continueToName() },
                    onScanPairingQr = { scanPairingQr() },
                    onPairingDeviceNameChange = pairingStateHolder::updateDeviceName,
                    onSubmitPairing = { submitPairing() },
                    onShowPairingHelp = pairingStateHolder::showHelp,
                    onPairingBack = { pairingStateHolder.back() },
                    onSettingsAction = ::handleSettingsAction,
                    onDashboardAction = ::handleDashboardAction,
                    onSelectTab = {
                        selectedTab = it
                        if (it == MobileAssistantTab.ORDERS) {
                            ensureOrdersLoaded()
                        }
                        if (it == MobileAssistantTab.PRODUCTS) {
                            ensureProductsLoaded()
                        }
                    },
                    onOrdersAction = ::handleOrdersAction,
                    onProductsSearchChange = {
                        if (mobileProductsSearch != it) {
                            mobileProductsSearch = it
                            handleProductsAction(ProductsAction.SearchChanged(it))
                        }
                    },
                    onProductsFilterChange = {
                        if (mobileProductsFilter != it) {
                            mobileProductsFilter = it
                            handleProductsAction(ProductsAction.FilterChanged(it.toFeatureProductsFilter()))
                        }
                    },
                    onLoadMoreProducts = { handleProductsAction(ProductsAction.LoadMore) },
                    onToggleProductVariants = { productId -> handleProductsAction(ProductsAction.ToggleVariants(productId)) },
                    onQuickEditProduct = { product, field, value ->
                        session?.token?.let { token -> productsCoordinator.quickEditProduct(token, product.id, field.toFeatureField(), value, true) }
                    },
                    onQuickEditVariant = { variant, field, value ->
                        session?.token?.let { token -> productsCoordinator.quickEditVariant(token, variant.productId, variant.id, field.toFeatureField(), value, true) }
                    },
                    onCloseOverlay = { mobileOverlayScreen = MobileAssistantOverlayScreen.NONE },
                    onNotificationFilterChange = ::selectMobileNotificationFilter,
                    onMarkNotificationsRead = { markVisibleNotificationsRead() },
                    onInstallAppUpdate = { installAppUpdate() },
                    onDismissAppUpdate = { dismissAppUpdate() },
                )
            }
        }
        contentView = composeView
        screenView.addView(composeView, 0,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
    }

    private fun showSessionTransitionShell() {
        screenView = FrameLayout(this).apply {
            setBackgroundColor(mobileTheme().appBg)
            sessionTransitionOverlay = DlaFlowSessionTransitionOverlay(this@MainActivity).also { overlay ->
                addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }
        setContentView(screenView)
    }

    private fun renderHeader() {
        val theme = mobileTheme()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        header.addView(ImageView(this).apply {
            setImageResource(resources.getIdentifier("dlaflow_app_icon", "drawable", packageName))
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = rounded(theme.surface, dp(10), theme.borderSubtle, dp(1))
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withLeft(10)
            addView(label("DlaFlow", size = 20f, color = theme.strong, bold = true))
            addView(label("Mobile Assistant", size = 11f, color = theme.muted, bold = true, top = 2))
        })
        header.addView(pill(if (session == null) "Do sparowania" else "Połączono", if (session == null) theme.warningText else theme.successText, if (session == null) theme.warningBg else theme.surface))
        root.addView(header)
    }

    private fun renderConnectedCard() {
        val theme = mobileTheme()
        val card = card()
        card.addView(label("Telefon DlaFlow", size = 15f, color = theme.strong, bold = true))
        val currentSession = session

        if (currentSession == null) {
            card.addView(label("Telefon nie jest jeszcze sparowany.", size = 12f, color = theme.muted, top = 6))
        } else {
            card.addView(label(currentSession.deviceName, size = 13f, color = theme.successText, bold = true, top = 6))
            card.addView(label(currentSession.tenantName.ifBlank { "Konto firmowe" }, size = 12f, color = theme.muted, top = 4))
            card.addView(label(currentSession.userEmail, size = 12f, color = theme.muted, top = 2))
            card.addView(secondaryButton("Odłącz ten telefon") { disconnectLocalPhone() })
        }

        root.addView(card)
    }

    private fun renderPhotoTaskHero() {
        val theme = mobileTheme()
        val activeTask = orderedPhotoTasks().firstOrNull()
        val title = if (activeTask == null) "Zadania produktu gotowe do wykonania" else "Zdjęcia produktu gotowe do wykonania"
        val count = activeTask?.let { "${it.mediaCount}/${it.maxPhotos}" } ?: "0/0"
        val hero = card()
        hero.addView(sectionLabel("ZADANIE Z PANELU"))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(8)
        }
        row.addView(label(title, size = 17f, color = theme.strong, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 2
        })
        row.addView(label(count, size = 17f, color = theme.primaryDark, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(theme.primarySoft, dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(42)).withLeft(10)
        })
        hero.addView(row)
        root.addView(hero)
    }

    private fun renderQuickStatusGrid() {
        val theme = mobileTheme()
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(10)
        }
        grid.addView(statusTile("Telefon DlaFlow", session?.tenantName?.ifBlank { "DlaYou" } ?: "DlaYou", theme).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withRight(5)
        })
        grid.addView(statusTile("Auto-otwieranie", if (canDrawOverOtherApps()) "Włączone" else "Powiadomienie", theme).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).withLeft(5)
        })
        root.addView(grid)
    }

    private fun renderPhotoTasksCard() {
        val theme = mobileTheme()
        val card = card(prominent = true)
        card.addView(pill("Aparat telefonu", theme.primaryDark, theme.primarySoft))

        if (session == null) {
            card.addView(label("Sparuj telefon, żeby odbierać zadania zdjęciowe z panelu.", size = 12f, color = theme.muted, top = 10))
            root.addView(card)
            return
        }

        if (!canDrawOverOtherApps()) {
            card.addView(secondaryButton("Pozwól pokazywać zadania od razu") { requestOverlayPermission() })
        }

        if (photoTasks.isEmpty()) {
            card.addView(label("Brak aktywnych zadań.", size = 18f, color = theme.strong, bold = true, top = 12))
            card.addView(label("Gdy klikniesz w panelu Wyślij z telefonu, zadanie pojawi się tutaj automatycznie.", size = 13f, color = theme.muted, top = 8))
            card.addView(secondaryButton("Odśwież zadania") { session?.token?.let(photoTasksCoordinator::refresh) })
        } else {
            orderedPhotoTasks().forEach { task ->
                val taskCard = innerCard(focused = task.id == focusedPhotoTaskId)
                val focused = task.id == focusedPhotoTaskId
                if (focused) {
                    focusedPhotoTaskView = taskCard
                }
                taskCard.addView(label(task.productName, size = 19f, color = theme.strong, bold = true, top = 8).apply {
                    maxLines = 3
                })
                if (task.productSku.isNotBlank()) {
                    taskCard.addView(twoColumnText("SKU: ${task.productSku}", "${task.mediaCount} z ${task.maxPhotos} zdjęć", theme, top = 10))
                } else {
                    taskCard.addView(twoColumnText("Produkt z panelu", "${task.mediaCount} z ${task.maxPhotos} zdjęć", theme, top = 10))
                }
                taskCard.addView(progressTrack(task.mediaCount, task.maxPhotos, theme))
                taskCard.addView(primaryButton("Zrób zdjęcie") { handlePhotoTasksAction(PhotoTasksAction.RequestCamera(task.id)) })
                taskCard.addView(secondaryButton("Wybierz zdjęcie") { handlePhotoTasksAction(PhotoTasksAction.RequestGallery(task.id)) })
                taskCard.addView(secondaryButton("Zakończ zadanie") { handlePhotoTasksAction(PhotoTasksAction.Complete(task.id)) })
                card.addView(taskCard)
            }
        }

        root.addView(card)
    }

    private fun renderCallerIdCard() {
        val theme = mobileTheme()
        val card = card()
        card.addView(label("Caller ID", size = 16f, color = theme.strong, bold = true))
        card.addView(label("Włączone. Karta klienta pojawi się przy zwykłym połączeniu.", size = 12f, color = theme.muted, bold = true, top = 6))

        if (session == null) {
            card.addView(label("Sparuj telefon, żeby włączyć Caller ID.", size = 12f, color = theme.muted, top = 10))
            root.addView(card)
            return
        }

        val roleStatus = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> "Na tym Androidzie włącz Caller ID w domyślnych aplikacjach systemu."
            isCallerIdOperational() -> "Caller ID jest włączony dla DlaFlow."
            isCallerIdRoleHeld() -> callerIdMissingPermissionMessage(
                needsPhoneState = !hasPhoneStatePermission(),
                needsContacts = !hasContactsPermission(),
            )
            isCallerIdRoleAvailable() -> "Telefon wymaga zgody systemowej dla DlaFlow Caller ID."
            else -> "Ten telefon nie udostępnia roli Caller ID dla aplikacji."
        }

        card.addView(label(roleStatus, size = 12f, color = theme.text, top = 10))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isCallerIdRoleAvailable() && !isCallerIdOperational()) {
            card.addView(primaryButton("Włącz Caller ID") { requestCallerIdRole() })
        }

        callerIdTestPhoneInput = input("Numer do testu Caller ID", "")
        card.addView(callerIdTestPhoneInput)
        card.addView(secondaryButton("Testuj numer") { testCallerIdLookup() })

        val preview = callerIdPreview
        if (preview != null) {
            val order = preview.primaryOrder
            val text = if (order == null) {
                "Brak zamówienia dla ${preview.phone}."
            } else {
                "${preview.displayName.ifBlank { preview.phone }}\n#${order.orderNumber} | ${order.status} | ${order.productSummary}"
            }
            card.addView(label(text, size = 12f, color = theme.strong, bold = order != null, top = 10))
            if (order != null) {
                card.addView(primaryButton("Pokaż kartę połączenia") {
                    startActivity(CallerIdActivity.createIntent(this, preview))
                })
            }
        }

        root.addView(card)
    }

    private fun verifySavedSession(showInitialTransition: Boolean = true) {
        val token = sessionStore.readToken()
        val baseUrl = sessionStore.readBaseUrl()

        if (token.isBlank()) {
            return
        }

        if (showInitialTransition) {
            showSessionTransition(activeStepIndex = 0, progress = 18)
        }
        setStatus("Sprawdzam zapisane połączenie...")
        executor.execute {
            runCatching {
                mobileApiClientForBaseUrl(baseUrl, sessionStore).verifySession(token)
            }.onSuccess { verifiedSession ->
                runOnUiThread {
                    if (session?.token != verifiedSession.token) {
                        replaceSettingsSession()
                        dashboardCoordinator.reset()
                        ordersCoordinator.reset()
                        scannerCoordinator.reset()
                        productsCoordinator.reset()
                        photoTasksCoordinator.reset()
                        notificationsCoordinator.reset()
                        clearMobileProductsState()
                        clearMobileNotificationsState()
                    }
                    session = verifiedSession
                    scannerCoordinator.resumePendingLaunch(verifiedSession.token)
                    syncPushInstallation(verifiedSession)
                    render()
                    showSessionTransition(activeStepIndex = 2, progress = 78)
                    setStatus("Telefon jest połączony.")
                    completeSessionTransition {
                        requestNotificationPermissionIfNeeded()
                        DlaFlowBackgroundSyncService.start(this)
                        startPhotoTaskDispatchPolling()
                        dashboardCoordinator.refresh(verifiedSession.token, showFeedback = false)
                        photoTasksCoordinator.refresh(verifiedSession.token)
                        refreshAppUpdate(showStatus = false)
                        if (selectedTab == MobileAssistantTab.ORDERS) {
                            ensureOrdersLoaded()
                        }
                    }
                }
            }.onFailure {
                runOnUiThread {
                    if (!handleMobileApiFailure(it, "Zapisane połączenie wygasło. Sparuj telefon ponownie.", confirmUnauthorized = false)) {
                        hideSessionTransition()
                    }
                    scannerCoordinator.failPendingLaunch()
                }
            }
        }
    }

    private fun syncPushInstallation(activeSession: MobileSession) {
        DlaFlowPushInstallation.refreshAndReceive(this) { installationId ->
            executor.execute {
                runCatching {
                    mobileApiClientForSession(sessionStore).updatePushInstallation(
                        token = activeSession.token,
                        deviceId = activeSession.deviceId,
                        installationId = installationId,
                    )
                }
            }
        }
    }

    private fun submitPairing() {
        val baseUrl = apiUrlValue.trim().ifBlank { sessionStore.readBaseUrl() }
        apiUrlValue = baseUrl
        pairingCoordinator.submit(baseUrl)
    }

    private fun handlePairingSuccess(baseUrl: String, nextSession: MobileSession) {
        sessionStore.saveSession(baseUrl, nextSession)
        replaceSettingsSession()
        updateSessionTransition(activeStepIndex = 1, progress = 46)
        dashboardCoordinator.reset()
        ordersCoordinator.reset()
        scannerCoordinator.reset()
        productsCoordinator.reset()
        photoTasksCoordinator.reset()
        notificationsCoordinator.reset()
        clearMobileProductsState()
        clearMobileNotificationsState()
        session = nextSession
        syncPushInstallation(nextSession)
        pairingStateHolder.reset()
        render()
        showSessionTransition(activeStepIndex = 2, progress = 78)
        setStatus("Telefon połączony z panelem.")
        completeSessionTransition {
            requestNotificationPermissionIfNeeded()
            DlaFlowBackgroundSyncService.start(this)
            startPhotoTaskDispatchPolling()
            dashboardCoordinator.refresh(nextSession.token, showFeedback = false)
            photoTasksCoordinator.refresh(nextSession.token)
            refreshAppUpdate(showStatus = false)
            if (selectedTab == MobileAssistantTab.ORDERS) {
                ensureOrdersLoaded()
            }
        }
    }

    private fun scanPairingQr() {
        pendingQrScanMode = QrScanMode.PAIRING
        IntentIntegrator(this).apply {
            setCaptureActivity(DlaFlowQrScanActivity::class.java)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Zeskanuj kod QR z panelu DlaFlow")
            setBeepEnabled(false)
            setOrientationLocked(true)
            initiateScan()
        }
    }

    private fun scanPackageCode() {
        pendingQrScanMode = QrScanMode.PACKAGE
        IntentIntegrator(this).apply {
            setCaptureActivity(DlaFlowQrScanActivity::class.java)
            setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            setPrompt("Zeskanuj kod paczki")
            setBeepEnabled(false)
            setOrientationLocked(true)
            initiateScan()
        }
    }

    private fun handlePackageScanCapture(rawValue: String?) {
        selectedTab = MobileAssistantTab.ORDERS
        scannerCoordinator.acceptCapture(
            rawCode = rawValue,
            token = session?.token,
            hasSavedSession = sessionStore.readToken().isNotBlank(),
        )
    }

    private fun handleScannerAction(action: ScannerAction) {
        scannerCoordinator.handleAction(action)
    }

    private fun handleScannerFeedback(feedback: ScannerFeedback) {
        setStatus(
            when (feedback) {
                ScannerFeedback.CAPTURE_EMPTY -> "Nie odczytano kodu paczki."
                ScannerFeedback.WAITING_FOR_SESSION,
                ScannerFeedback.LOOKUP_LOADING -> "Sprawdzam paczkę w DlaFlow."
                ScannerFeedback.MATCHED -> "Paczka znaleziona w DlaFlow."
                ScannerFeedback.AMBIGUOUS -> "Znaleziono kilka możliwych paczek w DlaFlow."
                ScannerFeedback.NO_MATCH -> "Nie znaleziono paczki w DlaFlow."
                ScannerFeedback.LOAD_FAILED -> "Nie udało się sprawdzić paczki."
            },
        )
        render()
    }

    private fun handleScannerUnauthorized(
        error: Throwable,
        allowRetry: Boolean,
        retryConfirmedRequest: () -> Unit,
        finishUnconfirmedRequest: () -> Unit,
    ) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się sprawdzić paczki.",
            showNonAuthStatus = false,
            onSessionValid = { if (allowRetry) retryConfirmedRequest() },
            onSessionUnconfirmed = finishUnconfirmedRequest,
        )
    }

    private fun handleDashboardAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> {
                val currentSession = session ?: return
                dashboardCoordinator.refresh(currentSession.token, showFeedback = true)
                photoTasksCoordinator.refresh(currentSession.token)
                if (selectedTab == MobileAssistantTab.ORDERS) {
                    ordersCoordinator.refreshList(currentSession.token, showFeedback = false)
                }
                refreshAppUpdate(showStatus = false)
            }
            DashboardAction.ScanPackage -> handleScannerAction(ScannerAction.RequestCapture)
            DashboardAction.OpenProductWork -> {
                val decision = choosePhotoTaskFocus(
                    activeTaskIds = photoTasksStateHolder.state.orderedTasks().map { it.id },
                    dashboardActiveTaskId = dashboardStateHolder.state.contentOrNull()?.activePhotoTask?.id,
                )
                photoTasksCoordinator.focus(decision.focusedTaskId)
                focusedPhotoTaskId = decision.focusedTaskId
                selectedTab = MobileAssistantTab.PRODUCTS
                setStatus(decision.statusMessage)
                if (decision.shouldRefreshTasks) {
                    session?.token?.let { token ->
                        dashboardCoordinator.refresh(token, showFeedback = false)
                    }
                    session?.token?.let(photoTasksCoordinator::refresh)
                }
                if (decision.focusedTaskId == null) {
                    ensureProductsLoaded()
                }
            }
            DashboardAction.OpenStatistics -> {
                selectedTab = MobileAssistantTab.ORDERS
                ensureOrdersLoaded()
                setStatus("Pokazuję szybkie statystyki z dzisiejszego dashboardu.")
            }
            DashboardAction.OpenProducts -> {
                selectedTab = MobileAssistantTab.PRODUCTS
                setStatus("Pokazuję produkty.")
                ensureProductsLoaded()
            }
            DashboardAction.OpenNotifications -> openMobileNotifications()
            is DashboardAction.TakePhoto -> handlePhotoTasksAction(PhotoTasksAction.RequestCamera(action.taskId))
            is DashboardAction.PickPhoto -> handlePhotoTasksAction(PhotoTasksAction.RequestGallery(action.taskId))
            is DashboardAction.CompletePhotoTask -> handlePhotoTasksAction(PhotoTasksAction.Complete(action.taskId))
        }
    }

    private fun handleDashboardFeedback(feedback: DashboardFeedback) {
        setStatus(
            when (feedback) {
                DashboardFeedback.REFRESHING -> "Odświeżam pulpit asystenta..."
                DashboardFeedback.REFRESHED -> "Pulpit odświeżony."
                DashboardFeedback.LOAD_FAILED -> "Nie udało się pobrać pulpitu."
            },
        )
    }

    private fun handleDashboardUnauthorized(error: Throwable, allowRetry: Boolean) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się odświeżyć pulpitu.",
            showNonAuthStatus = true,
            onSessionValid = {
                if (allowRetry) {
                    retryDashboardAfterConfirmedSession()
                }
            },
        )
    }

    private fun retryDashboardAfterConfirmedSession() {
        val currentSession = session ?: return
        dashboardCoordinator.refresh(
            token = currentSession.token,
            showFeedback = true,
            allowUnauthorizedRetry = false,
        )
    }

    private fun handleOrdersFeedback(feedback: OrdersFeedback) {
        setStatus(
            when (feedback) {
                OrdersFeedback.LIST_LOADING -> "Odświeżam zamówienia..."
                OrdersFeedback.LIST_READY -> "Zamówienia gotowe."
                OrdersFeedback.LIST_EMPTY -> "Brak zamówień dla wybranego filtra."
                OrdersFeedback.DETAIL_LOADING -> "Pobieram zamówienie..."
                OrdersFeedback.DETAIL_READY -> "Zamówienie gotowe."
                OrdersFeedback.DETAIL_CLOSED -> "Pokazuję listę zamówień."
                OrdersFeedback.LOAD_FAILED -> "Nie udało się pobrać zamówień."
            },
        )
    }

    private fun handleOrdersUnauthorized(
        error: Throwable,
        operation: OrdersLoadOperation,
        allowRetry: Boolean,
        onSessionUnconfirmed: () -> Unit,
    ) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się pobrać zamówień.",
            showNonAuthStatus = true,
            onSessionValid = {
                if (allowRetry) {
                    session?.token?.let { token ->
                        ordersCoordinator.retry(token, operation, showFeedback = true)
                    }
                }
            },
            onSessionUnconfirmed = onSessionUnconfirmed,
        )
    }

    private fun handlePairingQrResult(rawValue: String?) {
        pairingStateHolder.acceptQrResult(rawValue)
    }

    private fun ensureOrdersLoaded() {
        val currentSession = session ?: return
        val state = ordersStateHolder.state
        if (state.activeListRequestId == null && state.listState == pl.dlaflow.mobile.core.state.DlaFlowUiState.Loading) {
            ordersCoordinator.resetList(
                token = currentSession.token,
                query = state.query,
                showFeedback = true,
            )
        }
    }

    private fun handleOrdersAction(action: OrdersAction) {
        val currentSession = session ?: return
        when (action) {
            is OrdersAction.SearchChanged -> {
                if (action.search != ordersStateHolder.state.query.search) {
                    ordersCoordinator.resetList(
                        token = currentSession.token,
                        query = ordersStateHolder.state.query.copy(search = action.search),
                        showFeedback = true,
                    )
                }
            }

            is OrdersAction.FilterChanged -> {
                if (action.filter != ordersStateHolder.state.query.filter) {
                    ordersCoordinator.resetList(
                        token = currentSession.token,
                        query = ordersStateHolder.state.query.copy(filter = action.filter),
                        showFeedback = true,
                    )
                }
            }

            OrdersAction.Refresh -> ordersCoordinator.refreshList(currentSession.token, showFeedback = true)
            OrdersAction.LoadMore -> ordersCoordinator.loadMore(currentSession.token, showFeedback = true)
            is OrdersAction.OpenOrder -> ordersCoordinator.loadDetail(
                token = currentSession.token,
                orderNumber = action.orderNumber,
                showFeedback = true,
            )

            OrdersAction.CloseDetail -> ordersCoordinator.closeDetail()
            OrdersAction.Retry -> when (val route = ordersStateHolder.state.route) {
                OrdersRoute.List -> ordersCoordinator.refreshList(currentSession.token, showFeedback = true)
                is OrdersRoute.Detail -> ordersCoordinator.loadDetail(
                    token = currentSession.token,
                    orderNumber = route.orderNumber,
                    showFeedback = true,
                )
            }
        }
    }

    private fun openMobileNotifications() {
        mobileOverlayScreen = MobileAssistantOverlayScreen.NOTIFICATIONS
        markNotificationsReadOnOpen = true
        session?.token?.let(notificationsCoordinator::refresh)
    }

    private fun selectMobileNotificationFilter(filter: MobileNotificationFilter) {
        mobileNotificationFilter = filter
        notificationsCoordinator.selectFilter(filter.toFeatureNotificationFilter())
    }

    private fun markVisibleNotificationsRead() {
        session?.token?.let(notificationsCoordinator::markVisibleRead)
    }

    private fun handleNotificationsEffect(effect: NotificationsEffect) {
        when (effect) {
            NotificationsEffect.OpenOrders -> {
                mobileOverlayScreen = MobileAssistantOverlayScreen.NONE
                selectedTab = MobileAssistantTab.ORDERS
                ensureOrdersLoaded()
            }
            NotificationsEffect.OpenProducts,
            NotificationsEffect.OpenPhotoTasks -> {
                mobileOverlayScreen = MobileAssistantOverlayScreen.NONE
                selectedTab = MobileAssistantTab.PRODUCTS
                ensureProductsLoaded()
            }
            NotificationsEffect.OpenMessages -> setStatus("Wiadomości są dostępne w panelu.")
            is NotificationsEffect.ShowSafeExplanation -> setStatus("Ta sprawa wymaga działania w panelu.")
        }
        render()
    }

    private fun handleNotificationsUnauthorized(
        error: Throwable,
        allowRetry: Boolean,
        retryConfirmedRequest: () -> Unit,
        finishUnconfirmedRequest: () -> Unit,
    ) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się pobrać powiadomień.",
            showNonAuthStatus = false,
            onSessionValid = { if (allowRetry) retryConfirmedRequest() },
            onSessionUnconfirmed = finishUnconfirmedRequest,
        )
    }

    private fun syncNotificationsUiState() {
        val state = notificationsStateHolder.state
        mobileNotificationsLoading = state.isRefreshing || state.notificationsState is pl.dlaflow.mobile.core.state.DlaFlowUiState.Loading
        mobileNotifications = state.canonicalContentOrNull()?.items.orEmpty().map(NotificationItem::toMobileAssistantNotification)
    }

    private fun refreshAppUpdate(showStatus: Boolean = false) {
        val currentSession = session ?: return
        if (appUpdateChecking) {
            return
        }
        appUpdateChecking = true
        appUpdateError = ""
        if (showStatus) {
            setStatus("Sprawdzam aktualizację aplikacji...")
        }

        executor.execute {
            runCatching {
                mobileApiClientForSession(sessionStore).checkAppUpdate(
                    token = currentSession.token,
                    currentVersionCode = currentAppVersionCode(),
                    currentVersionName = currentAppVersionName(),
                )
            }.onSuccess { update ->
                runOnUiThread {
                    if (!isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    appUpdateChecking = false
                    appUpdate = update
                    appUpdateDismissalState = sessionStore.readUpdateDismissalState()
                    appUpdateDialogVisible = shouldShowAppUpdateDialog(update)
                    if (showStatus) {
                        setStatus(if (update == null) "Masz aktualną wersję aplikacji." else "Dostępna jest nowa wersja aplikacji.")
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    if (!isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    appUpdateChecking = false
                    if (!handleMobileApiFailure(error, "Nie udało się sprawdzić aktualizacji.", showNonAuthStatus = showStatus)) {
                        appUpdateError = mobileApiBusinessMessage(error, "Nie udało się sprawdzić aktualizacji.")
                    }
                }
            }
        }
    }

    private fun shouldShowAppUpdateDialog(update: MobileAppUpdate?): Boolean {
        if (update == null) {
            return false
        }

        return mobileAppUpdateIsBlocking(update, appUpdateDismissalState) || dismissedAppUpdateVersionInRuntime != update.latestVersionCode
    }

    private fun dismissAppUpdate() {
        val update = appUpdate ?: return
        if (appUpdateDownloading) {
            return
        }
        if (mobileAppUpdateIsBlocking(update, appUpdateDismissalState)) {
            appUpdateDialogVisible = true
            return
        }

        val nextState = nextMobileAppUpdateDismissalState(update, appUpdateDismissalState)
        sessionStore.saveUpdateDismissalState(nextState)
        appUpdateDismissalState = nextState
        if (mobileAppUpdateIsBlocking(update, nextState)) {
            appUpdateDialogVisible = true
            setStatus("Aktualizacja jest teraz wymagana do dalszej pracy.")
        } else {
            dismissedAppUpdateVersionInRuntime = update.latestVersionCode
            appUpdateDialogVisible = false
            setStatus("Przypomnimy o aktualizacji przy kolejnym uruchomieniu aplikacji.")
        }
    }

    private fun installAppUpdate() {
        val update = appUpdate ?: return
        if (appUpdateDownloading) {
            return
        }
        if (update.downloadUrl.isBlank()) {
            appUpdateError = "Brak linku pobrania. Sprawdź aktualizację ponownie."
            return
        }

        appUpdateDialogVisible = true
        appUpdateDownloading = true
        appUpdateDownloadProgress = 0
        appUpdateError = ""
        setStatus("Pobieram aktualizację aplikacji...")

        executor.execute {
            runCatching {
                downloadMobileAppUpdateApk(update) { progress ->
                    runOnUiThread {
                        appUpdateDownloadProgress = progress.coerceIn(0, 99)
                    }
                }
            }.onSuccess { apkFile ->
                runOnUiThread {
                    appUpdateDownloading = false
                    appUpdateDownloadProgress = 100
                    openMobileUpdateInstaller(apkFile, update)
                }
            }.onFailure { error ->
                runOnUiThread {
                    appUpdateDownloading = false
                    appUpdateError = "Nie udało się pobrać aktualizacji."
                    setStatus(appUpdateError)
                }
            }
        }
    }

    private fun downloadMobileAppUpdateApk(update: MobileAppUpdate, onProgress: (Int) -> Unit): File {
        val updatesDir = File(cacheDir, "mobile-updates").apply {
            mkdirs()
        }
        updatesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
        val apkFile = File(updatesDir, "dlaflow-mobile-assistant-${update.latestVersionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(update.downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 45_000
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive")

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("Nie udało się pobrać aktualizacji.")
            }

            val expectedBytes = update.sizeBytes.takeIf { it > 0 }?.toLong()
                ?: connection.contentLengthLong.takeIf { it > 0 }
                ?: 0L
            var downloadedBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) {
                            break
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        downloadedBytes += read.toLong()
                        if (expectedBytes > 0L) {
                            onProgress(((downloadedBytes * 100L) / expectedBytes).toInt())
                        }
                    }
                }
            }

            val actualSha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            if (!actualSha256.equals(update.sha256, ignoreCase = true)) {
                apkFile.delete()
                throw IllegalStateException("Pobrany plik nie przeszedł kontroli bezpieczeństwa.")
            }

            return apkFile
        } finally {
            connection.disconnect()
        }
    }

    private fun canInstallMobileUpdates(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
    }

    private fun currentAppVersionCode(): Int {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    private fun currentAppVersionName(): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

        return packageInfo.versionName ?: ""
    }

    private fun openMobileUpdateInstaller(apkFile: File, update: MobileAppUpdate) {
        if (!apkFile.isFile) {
            appUpdateError = "Pobrany plik aktualizacji jest niedostępny."
            return
        }

        val apkIdentityError = verifyMobileUpdateApkIdentity(apkFile, update)
        if (apkIdentityError != null) {
            appUpdateError = apkIdentityError
            apkFile.delete()
            setStatus(apkIdentityError)
            return
        }

        if (!canInstallMobileUpdates()) {
            pendingInstallApkFile = apkFile
            pendingInstallUpdate = update
            setStatus("Włącz zgodę na instalację aplikacji DlaFlow, a potem wróć do aplikacji.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            }
            return
        }

        pendingInstallApkFile = null
        pendingInstallUpdate = null
        val apkUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(installIntent)
            setStatus("Otworzyłem instalator aktualizacji.")
        } catch (_: ActivityNotFoundException) {
            appUpdateError = "Nie udało się otworzyć instalatora Androida."
            setStatus(appUpdateError)
        }
    }

    private fun verifyMobileUpdateApkIdentity(apkFile: File, update: MobileAppUpdate): String? {
        val archiveInfo = archivePackageInfo(apkFile)
            ?: return "Pobrana aplikacja nie wygląda jak poprawna paczka DlaFlow."

        if (archiveInfo.packageName != packageName) {
            return "Pobrana aplikacja ma niezgodny identyfikator pakietu."
        }

        if (packageVersionCode(archiveInfo) < update.latestVersionCode) {
            return "Pobrana aplikacja ma starszą wersję niż opublikowana aktualizacja."
        }

        val currentFingerprints = signingFingerprints(currentPackageInfoWithSignatures())
        val archiveFingerprints = signingFingerprints(archiveInfo)
        if (currentFingerprints.isNotEmpty() && archiveFingerprints.isNotEmpty() && currentFingerprints.intersect(archiveFingerprints).isEmpty()) {
            return "Pobrana aplikacja ma inny podpis bezpieczeństwa niż obecna instalacja."
        }

        return null
    }

    private fun archivePackageInfo(apkFile: File): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun currentPackageInfoWithSignatures(): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun packageVersionCode(packageInfo: PackageInfo): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    private fun signingFingerprints(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return emptySet()

        return signatures
            .map { signature ->
                MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .joinToString("") { byte -> "%02x".format(byte) }
            }
            .toSet()
    }

    private fun startPhotoTaskDispatchPolling() {
        dispatchHandler.removeCallbacks(dispatchRunnable)
        dispatchHandler.post(dispatchRunnable)
    }

    private fun stopPhotoTaskDispatchPolling() {
        dispatchHandler.removeCallbacks(dispatchRunnable)
    }

    private fun checkPhotoTaskDispatch() {
        session?.token?.let(photoTasksCoordinator::pollDispatch)
    }

    private fun openPhotoTaskIfAllowed(task: MobilePhotoTask) {
        if (!hasWindowFocus() && !canDrawOverOtherApps()) {
            setStatus("Zadanie czeka w powiadomieniu telefonu.")
            return
        }

        val intent = createPhotoTaskIntent(task.id)
        startActivity(intent)
        setStatus("Otwieram zadanie z panelu.")
    }

    private fun showPhotoTaskNotification(task: MobilePhotoTask) {
        if (sessionStore.readLastBackgroundPhotoTaskId() == task.id) {
            return
        }

        sessionStore.saveLastBackgroundPhotoTaskId(task.id)
        if (shouldShowNativePhotoTaskNotification(notificationPreferences)) {
            DlaFlowNotifications.showPhotoTaskNotification(this, task)
        }
    }

    private fun createPhotoTaskIntent(taskId: String): Intent {
        return DlaFlowDeepLinks.photoTaskIntent(this, taskId)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(DlaFlowDeepLinks.extraOpenOrders, false) == true) {
            selectedTab = MobileAssistantTab.ORDERS
            statusMessage = "Otwieram zamówienia z powiadomienia."
        }
        val taskId = intent?.getStringExtra(DlaFlowDeepLinks.extraFocusPhotoTaskId).orEmpty()
        if (taskId.isNotBlank()) {
            photoTasksStateHolder.consumeExternalFocus(taskId)
            focusedPhotoTaskId = taskId
            selectedTab = MobileAssistantTab.PRODUCTS
            statusMessage = "Otwieram zadanie zdjęciowe z panelu."
        }
        val smokePackageCode = if (BuildConfig.DEBUG) {
            intent?.getStringExtra(DlaFlowDeepLinks.extraSmokePackageCode).orEmpty()
        } else {
            ""
        }
        if (smokePackageCode.isNotBlank()) {
            intent?.removeExtra(DlaFlowDeepLinks.extraSmokePackageCode)
            selectedTab = MobileAssistantTab.ORDERS
            scannerCoordinator.acceptLaunch(
                rawCode = smokePackageCode,
                activeToken = session?.token,
                hasSavedSession = sessionStore.readToken().isNotBlank(),
            )
        }
        val smokeApiUrl = intent?.getStringExtra(extraSmokeApiUrl).orEmpty()
        val smokePairingCode = intent?.getStringExtra(extraSmokePairingCode).orEmpty()
        val smokePairingDeviceName = intent?.getStringExtra(extraSmokePairingDeviceName)
        if (smokeApiUrl.isNotBlank() && smokePairingCode.isNotBlank()) {
            pendingSmokeApiUrl = smokeApiUrl
            pendingSmokePairingCode = smokePairingCode
            pendingSmokePairingDeviceName = smokePairingDeviceName
        }
    }

    private fun consumeSmokePairingIntent(): Boolean {
        val seed = pairingSmokeSeed(
            apiUrl = pendingSmokeApiUrl,
            pairingCode = pendingSmokePairingCode,
            deviceName = pendingSmokePairingDeviceName,
        ) ?: return false

        pendingSmokeApiUrl = null
        pendingSmokePairingCode = null
        pendingSmokePairingDeviceName = null
        apiUrlValue = seed.baseUrl
        pairingStateHolder.updateCode(seed.pairingCode)
        pairingStateHolder.continueToName()
        if (seed.shouldAutoSubmit) {
            pairingStateHolder.updateDeviceName(seed.deviceName.orEmpty())
            submitPairing()
        } else {
            completeSessionTransition()
        }
        return true
    }

    private fun orderedPhotoTasks(): List<MobilePhotoTask> {
        val focus = focusedPhotoTaskId

        return if (focus.isNullOrBlank()) {
            photoTasks
        } else {
            photoTasks.sortedBy { if (it.id == focus) 0 else 1 }
        }
    }

    private fun scrollFocusedPhotoTaskIntoView() {
        val view = focusedPhotoTaskView ?: return
        scrollView.post {
            val taskTop = topRelativeToRoot(view)
            val targetY = (taskTop - ((scrollView.height - view.height) / 2)).coerceAtLeast(0)
            scrollView.scrollTo(0, targetY)
        }
    }

    private fun topRelativeToRoot(view: View): Int {
        var top = 0
        var current: View? = view

        while (current != null && current != root) {
            top += current.top
            current = current.parent as? View
        }

        return top
    }

    private fun requestCamera(taskId: String) {
        if (!beginPendingPhoto(taskId, PhotoCaptureKind.CAMERA)) return
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(taskId)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
        }
    }

    private fun openCamera(taskId: String) {
        val photoFile = runCatching { createCameraPhotoFile() }.getOrNull()
        if (photoFile == null) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Nie udało się przygotować pliku zdjęcia.")
            return
        }

        val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        pendingCameraPhotoFile = photoFile
        pendingCameraPhotoUri = photoUri

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (intent.resolveActivity(packageManager) == null) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Nie znaleziono aplikacji aparatu.")
            return
        }
        startActivityForResult(intent, pendingPhotoResultRequestCode ?: return)
    }

    private fun openGallery(taskId: String) {
        if (!beginPendingPhoto(taskId, PhotoCaptureKind.GALLERY)) return
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, pendingPhotoResultRequestCode ?: return)
    }

    private fun uploadCameraResult(taskId: String) {
        val uri = pendingCameraPhotoUri
        val file = pendingCameraPhotoFile
        val input = when {
            uri != null -> contentResolver.openInputStream(uri)
            file != null && file.exists() -> file.inputStream()
            else -> null
        }
        if (input == null) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Aparat nie zapisał pełnego zdjęcia.")
            return
        }
        uploadPhoto(taskId, input, file?.name ?: "zdjecie-z-telefonu.jpg", "image/jpeg")
    }

    private fun uploadGalleryResult(taskId: String, uri: Uri?) {
        if (uri == null) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Nie wybrano zdjęcia.")
            return
        }

        val input = contentResolver.openInputStream(uri)
        if (input == null) {
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            setStatus("Nie udało się odczytać zdjęcia.")
            return
        }
        uploadPhoto(taskId, input, "zdjecie-z-telefonu", contentResolver.getType(uri) ?: "image/jpeg")
    }

    private fun uploadPhoto(taskId: String, input: java.io.InputStream, fileName: String, mimeType: String) {
        val capture = photoCaptureStateHolder.pending
        val currentSession = session ?: run {
            input.close()
            return
        }
        if (capture == null || capture.taskId != taskId || capture.sessionKey != currentSession.token) {
            input.close()
            clearPendingCameraPhoto()
            photoTasksCoordinator.mediaSelectionCancelled()
            return
        }
        if (!photoCaptureStateHolder.advance(capture, pl.dlaflow.mobile.feature.products.PhotoCapturePhase.PREPARING)) {
            input.close()
            return
        }
        val preparingCapture = capture.copy(phase = pl.dlaflow.mobile.feature.products.PhotoCapturePhase.PREPARING)
        setStatus("Wysyłam pełne zdjęcie...")
        executor.execute {
            val destination = File(cacheDir, "mobile-photo-uploads/${capture.sourceId}.bin")
            val prepared = input.use { stream ->
                prepareMobilePhotoUpload(stream, destination, capture.sourceId, fileName, mimeType)
            }
            runOnUiThread {
                val source = (prepared as? MobilePhotoUploadPreparationResult.Ready)?.source
                if (!photoCaptureStateHolder.matches(preparingCapture) || !isCurrentSessionToken(currentSession.token)) {
                    source?.let(photoTasksCoordinator::discardUploadSource)
                    return@runOnUiThread
                }
                clearPendingCameraPhoto()
                when {
                    source == null -> {
                        photoTasksCoordinator.mediaSelectionCancelled()
                        setStatus("Zdjęcie jest puste, za duże albo niedostępne.")
                    }
                    photoTasksCoordinator.submitUpload(currentSession.token, taskId, source) -> {
                        setStatus("Wysyłam pełne zdjęcie...")
                    }
                    else -> setStatus("Nie udało się przygotować zdjęcia do wysłania.")
                }
            }
        }
    }

    private fun createCameraPhotoFile(): File {
        val directory = File(cacheDir, "mobile-photo-captures").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        return File.createTempFile("dlaflow_${timestamp}_", ".jpg", directory)
    }

    private fun clearPendingCameraPhoto() {
        pendingCameraPhotoUri = null
        pendingCameraPhotoFile?.delete()
        pendingCameraPhotoFile = null
        pendingPhotoTaskId = null
        pendingPhotoResultRequestCode = null
        pendingPhotoResultSourceId = null
        photoCaptureStateHolder.reset()
    }

    private fun beginPendingPhoto(taskId: String, kind: PhotoCaptureKind): Boolean {
        val sessionToken = session?.token ?: return false
        val capture = photoCaptureStateHolder.begin(sessionToken, taskId, kind, java.util.UUID.randomUUID().toString()) ?: return false
        pendingPhotoTaskId = taskId
        pendingPhotoResultRequestCode = allocatePhotoResultRequestCode()
        pendingPhotoResultSourceId = capture.sourceId
        return true
    }

    private fun allocatePhotoResultRequestCode(): Int {
        val requestCode = nextPhotoResultRequestCode
        nextPhotoResultRequestCode = if (requestCode >= photoResultRequestCodeMax) photoResultRequestCodeMin else requestCode + 1
        return requestCode
    }

    private fun requestCallerIdRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setStatus("Włącz Caller ID w ustawieniach domyślnych aplikacji telefonu.")
            return
        }

        val missingPermissions = missingCallerIdRuntimePermissions()
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions, phoneStatePermissionRequestCode)
            return
        }

        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            setStatus("Ten telefon nie udostępnia roli Caller ID.")
            return
        }

        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            setStatus("Caller ID jest już włączony.")
            render()
            return
        }

        startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), callerIdRoleRequestCode)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequestCode)
        }
    }

    private fun areNotificationsAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }

        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        openResolvedSystemSettings(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            getString(R.string.settings_host_overlay_settings_open_failed),
        )
    }

    private fun openAppSystemSettings() {
        openResolvedSystemSettings(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            getString(R.string.settings_host_app_settings_open_failed),
            allowApplicationFallback = false,
        )
    }

    private fun ensureProductsLoaded() {
        val currentSession = session ?: return
        val state = productsStateHolder.state
        if (state.activeListRequestId == null && state.listState == pl.dlaflow.mobile.core.state.DlaFlowUiState.Loading) {
            productsCoordinator.resetList(currentSession.token, state.query, showFeedback = true)
        }
    }

    private fun handleProductsAction(action: ProductsAction) {
        session?.token?.let { productsCoordinator.handleAction(it, action, showFeedback = true) }
        syncProductsUiState()
    }

    private fun handleProductsFeedback(feedback: ProductsFeedback) {
        syncProductsUiState()
        setStatus(
            when (feedback) {
                ProductsFeedback.LIST_LOADING -> "Odświeżam produkty..."
                ProductsFeedback.LIST_READY -> "Produkty gotowe."
                ProductsFeedback.LIST_EMPTY -> "Brak produktów dla wybranego filtra."
                ProductsFeedback.VARIANTS_LOADING -> "Pobieram warianty..."
                ProductsFeedback.VARIANTS_READY -> "Warianty gotowe."
                ProductsFeedback.QUICK_EDIT_SAVING -> "Zapisuję zmianę..."
                ProductsFeedback.QUICK_EDIT_SAVED -> "Produkt zaktualizowany."
                ProductsFeedback.LOAD_FAILED -> "Nie udało się wykonać operacji na produktach."
            },
        )
        render()
    }

    private fun handleProductsUnauthorized(
        error: Throwable,
        @Suppress("UNUSED_PARAMETER") operation: ProductsOperation,
        allowRetry: Boolean,
        retryConfirmedRequest: () -> Unit,
        finishUnconfirmedRequest: () -> Unit,
    ) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się wykonać operacji na produktach.",
            showNonAuthStatus = false,
            onSessionValid = { if (allowRetry) retryConfirmedRequest() },
            onSessionUnconfirmed = finishUnconfirmedRequest,
        )
    }

    private fun handlePhotoTasksAction(action: PhotoTasksAction) {
        photoTasksCoordinator.handleAction(session?.token, action)
    }

    private fun handlePhotoTasksEffect(effect: PhotoTasksEffect) {
        when (effect) {
            is PhotoTasksEffect.LaunchCamera -> requestCamera(effect.taskId)
            is PhotoTasksEffect.LaunchGallery -> openGallery(effect.taskId)
            is PhotoTasksEffect.PresentDispatch -> presentPhotoTaskDispatch(effect.task)
            PhotoTasksEffect.UploadSucceeded -> {
                session?.token?.let { token -> dashboardCoordinator.refresh(token, showFeedback = false) }
                setStatus("Zdjęcie dodane do produktu.")
            }
            PhotoTasksEffect.CompletionSucceeded -> {
                session?.token?.let { token -> dashboardCoordinator.refresh(token, showFeedback = false) }
                setStatus("Zadanie zakończone.")
            }
            PhotoTasksEffect.OperationFailed -> setStatus("Nie udało się wykonać operacji na zadaniu zdjęciowym.")
        }
        render()
    }

    private fun handlePhotoTasksUnauthorized(
        error: Throwable,
        allowRetry: Boolean,
        retryConfirmedRequest: () -> Unit,
        finishUnconfirmedRequest: () -> Unit,
    ) {
        confirmRevokedSession(
            error = error,
            fallbackMessage = "Nie udało się wykonać operacji na zadaniu zdjęciowym.",
            showNonAuthStatus = false,
            onSessionValid = { if (allowRetry) retryConfirmedRequest() },
            onSessionUnconfirmed = finishUnconfirmedRequest,
        )
    }

    private fun syncProductsUiState() {
        val featureState = productsStateHolder.state
        val content = featureState.listState.let { state ->
            when (state) {
                is pl.dlaflow.mobile.core.state.DlaFlowUiState.Content -> state.data
                is pl.dlaflow.mobile.core.state.DlaFlowUiState.Offline -> state.lastContent
                else -> null
            }
        }
        mobileProducts = content?.items.orEmpty().map { item ->
            MobileProduct(
                id = item.id,
                name = item.name,
                sku = item.sku,
                ean = item.ean,
                image = item.thumbnailUrl,
                thumbnailUrl = item.thumbnailUrl,
                grossPrice = item.grossPrice,
                stock = item.stock,
                status = item.status.label,
                currency = item.currency,
                variantCount = item.variantCount,
                lowStock = item.lowStock,
                editableFields = MobileProductEditableFields(item.editableFields.grossPrice, item.editableFields.stock),
            )
        }
        mobileProductsNextCursor = content?.nextCursor
        mobileProductsTotal = content?.total ?: 0
        mobileProductsLoading = featureState.activeListRequestId != null
        mobileProductsSearch = featureState.query.search
        mobileProductsFilter = when (featureState.query.filter) {
            pl.dlaflow.mobile.feature.products.ProductsFilter.ALL -> MobileProductFilter.ALL
            pl.dlaflow.mobile.feature.products.ProductsFilter.LOW_STOCK -> MobileProductFilter.LOW_STOCK
            pl.dlaflow.mobile.feature.products.ProductsFilter.NO_IMAGE -> MobileProductFilter.NO_IMAGE
            pl.dlaflow.mobile.feature.products.ProductsFilter.HAS_VARIANTS -> MobileProductFilter.HAS_VARIANTS
        }
        mobileProductsReadOnly = content?.canEdit == false
        mobileProductsNoAccess = featureState.listState is pl.dlaflow.mobile.core.state.DlaFlowUiState.NoAccess
        mobileProductVariants = featureState.variants.mapValues { (_, value) ->
            when (value) {
                is pl.dlaflow.mobile.core.state.DlaFlowUiState.Content -> value.data.map { variant ->
                    MobileProductVariant(
                        id = variant.id,
                        productId = variant.productId,
                        name = variant.name,
                        sku = variant.sku,
                        ean = variant.ean,
                        image = variant.thumbnailUrl,
                        thumbnailUrl = variant.thumbnailUrl,
                        price = variant.price,
                        stock = variant.stock,
                        status = variant.status.label,
                        editableFields = MobileProductVariantEditableFields(variant.editableFields.price, variant.editableFields.stock),
                    )
                }
                else -> emptyList()
            }
        }
        mobileProductVariantsLoading = featureState.variants.filterValues { it is pl.dlaflow.mobile.core.state.DlaFlowUiState.Loading }.keys
    }

    private fun syncPhotoTasksUiState() {
        val featureTasks = photoTasksStateHolder.state.orderedTasks()
        photoTasks = featureTasks.map { task ->
            MobilePhotoTask(
                id = task.id,
                productName = task.productName,
                productSku = task.productSku,
                status = task.status.name.lowercase(Locale.ROOT),
                mediaCount = task.mediaCount,
                maxPhotos = task.maxPhotos,
                expiresAt = task.expiresAt,
            )
        }
        focusedPhotoTaskId = photoTasksStateHolder.state.focusedTaskId
    }

    private fun presentPhotoTaskDispatch(task: ProductPhotoTask) {
        syncPhotoTasksUiState()
        val mobileTask = MobilePhotoTask(
            id = task.id,
            productName = task.productName,
            productSku = task.productSku,
            status = task.status.name.lowercase(Locale.ROOT),
            mediaCount = task.mediaCount,
            maxPhotos = task.maxPhotos,
            expiresAt = task.expiresAt,
        )
        selectedTab = MobileAssistantTab.PRODUCTS
        showPhotoTaskNotification(mobileTask)
        openPhotoTaskIfAllowed(mobileTask)
        session?.token?.let { token -> dashboardCoordinator.refresh(token, showFeedback = false) }
        render()
    }

    private fun openResolvedSystemSettings(
        intent: Intent,
        failureMessage: String,
        allowApplicationFallback: Boolean = true,
    ): Boolean {
        val candidates = buildList {
            add(intent)
            if (allowApplicationFallback) add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        val result = launchFirstResolvedSettingsTarget(
            candidates = candidates,
            canResolve = { it.resolveActivity(packageManager) != null },
            launch = ::startActivity,
        )
        if (result.candidateIndex == 1) setStatus(getString(R.string.settings_host_system_settings_fallback_opened))
        if (!result.launched) setStatus(failureMessage)
        return result.launched
    }

    private fun canDrawOverOtherApps(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun autoOpenStatusText(): String {
        return if (canDrawOverOtherApps()) {
            "Telefon może pokazać nowe zadanie od razu. Gdy Android zablokuje ekran, użyj powiadomienia."
        } else {
            "Bez dodatkowej zgody Android pokaże powiadomienie z zadaniem."
        }
    }

    private fun testCallerIdLookup(requestedPhone: String? = null) {
        val currentSession = session ?: return
        val phone = (requestedPhone ?: callerIdTestPhoneValue).trim()
        if (phone.isBlank()) {
            setStatus("Wpisz numer do testu Caller ID.")
            return
        }

        val request = SettingsCallerIdLookupRequest(++nextCallerIdLookupRequestId, settingsSessionEpoch, phone)
        activeCallerIdLookupRequest = request
        callerIdPreview = null
        setStatus("Sprawdzam numer...")
        executor.execute {
            runCatching {
                mobileApiClientForSession(sessionStore).lookupCallerId(currentSession.token, phone)
            }.onSuccess { lookup ->
                runOnUiThread {
                    if (activeCallerIdLookupRequest != request || !isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    activeCallerIdLookupRequest = null
                    callerIdPreview = lookup
                    render()
                    setStatus(if (lookup.primaryOrder == null) "Brak zamówienia dla numeru." else "Znaleziono zamówienie.")
                }
            }.onFailure { error ->
                runOnUiThread {
                    if (activeCallerIdLookupRequest != request || !isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    activeCallerIdLookupRequest = null
                    handleMobileApiFailure(error, "Nie udało się sprawdzić numeru.")
                }
            }
        }
    }

    private fun handleMobileApiFailure(
        error: Throwable,
        fallbackMessage: String,
        showNonAuthStatus: Boolean = true,
        confirmUnauthorized: Boolean = true,
    ): Boolean {
        if (isMobileSessionRevoked(error)) {
            if (confirmUnauthorized) {
                confirmRevokedSession(error, fallbackMessage, showNonAuthStatus)
            } else {
                clearRevokedSession()
            }
            return true
        }

        if (showNonAuthStatus) {
            setStatus(mobileApiBusinessMessage(error, fallbackMessage))
        }

        return false
    }

    private fun confirmRevokedSession(
        error: Throwable,
        fallbackMessage: String,
        showNonAuthStatus: Boolean,
        onSessionValid: () -> Unit = {},
        onSessionUnconfirmed: () -> Unit = {},
    ) {
        val currentSession = session
        if (currentSession == null) {
            clearRevokedSession()
            return
        }

        if (showNonAuthStatus) {
            setStatus("Sprawdzam połączenie telefonu...")
        }

        executor.execute {
            var sessionConfirmedValid = false
            var sessionUnconfirmed = false
            val shouldClearSession = shouldClearMobileSessionAfterUnauthorized(
                error = error,
                verifyCurrentSession = {
                    mobileApiClientForSession(sessionStore).verifySession(currentSession.token)
                },
                onSessionValid = { sessionConfirmedValid = true },
                onSessionUnconfirmed = { sessionUnconfirmed = true },
            )

            runOnUiThread {
                if (!isCurrentSessionToken(currentSession.token)) {
                    return@runOnUiThread
                }

                if (shouldClearSession) {
                    clearRevokedSession()
                } else {
                    if (showNonAuthStatus) {
                        setStatus(mobileApiBusinessMessage(error, fallbackMessage))
                    }
                    if (sessionConfirmedValid) {
                        onSessionValid()
                    } else if (sessionUnconfirmed) {
                        onSessionUnconfirmed()
                    }
                }
            }
        }
    }

    private fun mobileApiBusinessMessage(error: Throwable, fallbackMessage: String): String {
        if (error !is MobileApiException) {
            return fallbackMessage
        }

        return when (error.statusCode) {
            400 -> "Sprawdź dane i spróbuj ponownie."
            403 -> "Brak uprawnień do tej akcji."
            404 -> "Nie znaleziono danych dla tej akcji."
            409 -> "Ta akcja nie jest dostępna dla tego elementu."
            410 -> "Ta akcja wygasła. Odśwież dane."
            else -> fallbackMessage
        }
    }

    private fun isMobileSessionRevoked(error: Throwable): Boolean {
        return error is MobileApiException && (error.statusCode == 401 || error.code == "AUTH_REQUIRED")
    }

    private fun isCurrentSessionToken(token: String): Boolean {
        return session?.token == token
    }

    private fun clearMobileProductsData(invalidateCallbacks: Boolean = false) {
        if (invalidateCallbacks) {
            mobileProductsStateVersion += 1
        }
        mobileProducts = emptyList()
        mobileProductsNextCursor = null
        mobileProductsTotal = 0
        mobileProductsLoading = false
        mobileProductVariants = emptyMap()
        mobileProductVariantsLoading = emptySet()
    }

    private fun clearMobileProductsState() {
        mobileProductsRequestVersion += 1
        clearMobileProductsData(invalidateCallbacks = true)
        mobileProductsSearch = ""
        mobileProductsFilter = MobileProductFilter.ALL
        mobileProductsReadOnly = false
        mobileProductsNoAccess = false
    }

    private fun clearMobileNotificationsState() {
        mobileOverlayScreen = MobileAssistantOverlayScreen.NONE
        mobileNotifications = emptyList()
        mobileNotificationsLoading = false
        mobileNotificationFilter = MobileNotificationFilter.ALL
        markNotificationsReadOnOpen = false
    }

    private fun clearAppUpdateState() {
        appUpdate = null
        appUpdateDialogVisible = false
        appUpdateChecking = false
        appUpdateDownloading = false
        appUpdateDownloadProgress = 0
        appUpdateError = ""
        pendingInstallApkFile = null
        pendingInstallUpdate = null
    }

    private fun clearRevokedSession() {
        clearDisconnectedSession("Telefon został odłączony w panelu. Sparuj go ponownie.")
    }

    private fun clearDisconnectedSession(message: String) {
        sessionStore.clearSession()
        DlaFlowBackgroundSyncService.stop(this)
        stopPhotoTaskDispatchPolling()
        clearPendingCameraPhoto()
        session = null
        dashboardCoordinator.reset()
        photoTasks = emptyList()
        callerIdPreview = null
        focusedPhotoTaskId = null
        focusedPhotoTaskView = null
        productsCoordinator.reset()
        photoTasksCoordinator.reset()
        notificationsCoordinator.reset()
        scannerCoordinator.reset()
        clearAppUpdateState()
        ordersCoordinator.reset()
        clearMobileProductsState()
        clearMobileNotificationsState()
        selectedTab = MobileAssistantTab.DASHBOARD
        pairingStateHolder.reset()
        contentReadyForDisplay = true
        render()
        setStatus(message)
    }

    private fun disconnectLocalPhone(request: SettingsDisconnectRequest? = null) {
        val currentSession = session

        if (currentSession == null) {
            if (request == null || settingsCoordinator.acceptsDisconnectSuccess(request)) {
                clearDisconnectedSession("Telefon odłączony. Sparuj go ponownie w panelu.")
            }
            return
        }

        setStatus("Odłączam telefon w panelu...")
        executor.execute {
            runCatching {
                mobileApiClientForSession(sessionStore).revokeCurrentDevice(currentSession.token)
            }.onSuccess {
                runOnUiThread {
                    if ((request != null && !settingsCoordinator.acceptsDisconnectSuccess(request)) || !isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    clearDisconnectedSession("Telefon odłączony. Panel nie będzie go już pokazywać.")
                }
            }.onFailure { error ->
                runOnUiThread {
                    if ((request != null && !settingsCoordinator.acceptDisconnectFailure(request)) || !isCurrentSessionToken(currentSession.token)) {
                        return@runOnUiThread
                    }
                    if (!handleMobileApiFailure(error, "Nie udało się odłączyć telefonu w panelu. Sprawdź połączenie i spróbuj ponownie.")) {
                        render()
                    }
                }
            }
        }
    }

    private fun isCallerIdRoleAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }

        return getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun isCallerIdRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }

        return getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun hasPhoneStatePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasContactsPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallerIdRuntimePermissions(): Boolean {
        return hasPhoneStatePermission() && hasContactsPermission()
    }

    private fun missingCallerIdRuntimePermissions(): Array<String> {
        return buildList {
            if (!hasPhoneStatePermission()) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (!hasContactsPermission()) {
                add(Manifest.permission.READ_CONTACTS)
            }
        }.toTypedArray()
    }

    private fun isCallerIdOperational(): Boolean {
        return isCallerIdRoleHeld() && hasCallerIdRuntimePermissions()
    }

    private fun setStatus(value: String) {
        statusMessage = value
        if (::statusView.isInitialized) {
            statusView.text = value
        }
    }

    private fun showSessionTransition(activeStepIndex: Int, progress: Int, animateIn: Boolean = true) {
        sessionTransitionStartedAt = System.currentTimeMillis()
        sessionTransitionOverlay?.show(
            title = "Logowanie",
            description = "Przygotowujemy bezpieczną sesję i wczytujemy zadania.",
            activeStepIndex = activeStepIndex,
            progress = progress,
            steps = sessionTransitionSteps,
            animateIn = animateIn,
        )
    }

    private fun releaseSystemSplash() {
        keepSystemSplashVisible = false
    }

    private fun updateSessionTransition(activeStepIndex: Int, progress: Int) {
        sessionTransitionOverlay?.update(activeStepIndex = activeStepIndex, progress = progress, steps = sessionTransitionSteps)
    }

    private fun completeSessionTransition(onHidden: (() -> Unit)? = null) {
        sessionTransitionOverlay?.update(activeStepIndex = 3, progress = 100, steps = sessionTransitionSteps)
        val elapsedMs = System.currentTimeMillis() - sessionTransitionStartedAt
        val delayMs = (sessionTransitionMinimumVisibleMs - elapsedMs).coerceAtLeast(360L)
        dispatchHandler.postDelayed({ revealContent() }, delayMs)
        sessionTransitionOverlay?.finishAndHide(delayMs = delayMs)
        if (onHidden != null) {
            dispatchHandler.postDelayed(onHidden, delayMs + 320L)
        }
    }

    private fun hideSessionTransition() {
        revealContent()
        sessionTransitionOverlay?.finishAndHide(delayMs = 0)
    }

    private fun revealContent() {
        contentReadyForDisplay = true
        if (::contentView.isInitialized) {
            contentView.animate().cancel()
            contentView.animate().alpha(1f).setDuration(220L).start()
        }
    }

    private fun card(prominent: Boolean = false): LinearLayout {
        val theme = mobileTheme()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(theme.surface, dp(8), if (prominent) theme.primarySoftBorder else theme.borderSubtle, dp(1))
            root.addViewSpacer(12)
        }
    }

    private fun innerCard(focused: Boolean = false): LinearLayout {
        val theme = mobileTheme()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(theme.surfaceSubtle, dp(8), if (focused) theme.primarySoftBorder else theme.borderSubtle, dp(1))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(10)
        }
    }

    private fun input(hint: String, value: String): EditText {
        val theme = mobileTheme()
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 13f
            setTextColor(theme.strong)
            setHintTextColor(theme.muted)
            typeface = appTypeface(Typeface.BOLD)
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(theme.surfaceSubtle, dp(8), theme.border, dp(1))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).withTop(12)
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): Button {
        val theme = mobileTheme()
        return Button(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            background = rounded(theme.primary, dp(8))
            typeface = appTypeface(Typeface.BOLD)
            includeFontPadding = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).withTop(12)
        }
    }

    private fun secondaryButton(text: String, onClick: () -> Unit): Button {
        val theme = mobileTheme()
        return Button(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(theme.primaryDark)
            background = rounded(theme.primarySoft, dp(8), theme.primarySoftBorder, dp(1))
            typeface = appTypeface(Typeface.BOLD)
            includeFontPadding = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).withTop(10)
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false, top: Int = 0): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) {
                typeface = appTypeface(Typeface.BOLD)
            } else {
                typeface = appTypeface(Typeface.NORMAL)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(top)
        }
    }

    private fun sectionLabel(text: String): TextView {
        val theme = mobileTheme()
        return label(text, size = 10f, color = theme.muted, bold = true).apply {
            letterSpacing = 0.04f
        }
    }

    private fun pill(text: String, textColor: Int, fillColor: Int): TextView {
        val theme = mobileTheme()
        return label(text, size = 10f, color = textColor, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            background = rounded(fillColor, dp(14), theme.borderSubtle, dp(1))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(30))
        }
    }

    private fun statusTile(title: String, value: String, theme: DlaFlowMobileTheme): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = rounded(theme.surface, dp(8), theme.borderSubtle, dp(1))
            addView(label(title, size = 13f, color = theme.strong, bold = true))
            addView(label(value, size = 11f, color = theme.muted, bold = true, top = 6).apply {
                maxLines = 1
            })
        }
    }

    private fun twoColumnText(leftValue: String, rightValue: String, theme: DlaFlowMobileTheme, top: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).withTop(top)
            addView(label(leftValue, size = 12f, color = theme.muted, bold = true).apply {
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(label(rightValue, size = 12f, color = theme.muted, bold = true).apply {
                gravity = Gravity.RIGHT
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun progressTrack(current: Int, max: Int, theme: DlaFlowMobileTheme): View {
        val safeMax = max.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeMax)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(theme.borderSubtle, dp(3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)).withTop(12)
            addView(View(context).apply {
                background = rounded(theme.primary, dp(3))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, safeCurrent.toFloat())
            })
            addView(View(context), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (safeMax - safeCurrent).toFloat()))
        }
    }

    private fun rounded(fill: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeColor != null && strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun mobileTheme(): DlaFlowMobileTheme {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (dark) {
            DlaFlowMobileTheme(
                dark = true,
                appBg = color(0x0F131D),
                surface = color(0x171C27),
                surfaceSubtle = color(0x151A24),
                border = color(0x2A3342),
                borderSubtle = color(0x202735),
                strong = color(0xF8FAFC),
                text = color(0xD7DEEA),
                muted = color(0x9AA7BA),
                primary = color(0x9B83FF),
                primaryDark = color(0xC8B8FF),
                primarySoft = colorWithAlpha(0x7B5CF6, 0x29),
                primarySoftBorder = colorWithAlpha(0x9B83FF, 0x3D),
                successText = color(0x5EEAD4),
                warningText = color(0xFBBF24),
                warningBg = colorWithAlpha(0xF59E0B, 0x24),
            )
        } else {
            DlaFlowMobileTheme(
                dark = false,
                appBg = color(0xF8F9FC),
                surface = color(0xFFFFFF),
                surfaceSubtle = color(0xFBFCFE),
                border = color(0xDFE4EC),
                borderSubtle = color(0xEDF0F5),
                strong = color(0x0F172A),
                text = color(0x334155),
                muted = color(0x64748B),
                primary = color(0x7B5CF6),
                primaryDark = color(0x4F1BD8),
                primarySoft = color(0xF1ECFF),
                primarySoftBorder = color(0xE4DCFF),
                successText = color(0x0B8F78),
                warningText = color(0xC2410C),
                warningBg = color(0xFFF7ED),
            )
        }
    }

    private fun appTypeface(style: Int): Typeface {
        val base = ResourcesCompat.getFont(this, resources.getIdentifier("inter_variable", "font", packageName))
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        return Typeface.create(base, style)
    }

    private fun LinearLayout.addViewSpacer(height: Int) {
        if (childCount > 0) {
            addView(View(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height)))
        }
    }

    private fun LinearLayout.LayoutParams.withTop(top: Int): LinearLayout.LayoutParams {
        topMargin = dp(top)
        return this
    }

    private fun LinearLayout.LayoutParams.withLeft(left: Int): LinearLayout.LayoutParams {
        leftMargin = dp(left)
        return this
    }

    private fun LinearLayout.LayoutParams.withRight(right: Int): LinearLayout.LayoutParams {
        rightMargin = dp(right)
        return this
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun formatBytes(bytes: Int): String {
        return if (bytes >= 1024 * 1024) {
            String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1024.0 / 1024.0)
        } else {
            "${bytes / 1024} KB"
        }
    }

    private fun color(value: Long): Int {
        return (0xFF000000L or value).toInt()
    }

    private fun colorWithAlpha(rgb: Long, alpha: Int): Int {
        return ((alpha.toLong().coerceIn(0, 255) shl 24) or rgb).toInt()
    }

    data class DlaFlowMobileTheme(
        val dark: Boolean,
        val appBg: Int,
        val surface: Int,
        val surfaceSubtle: Int,
        val border: Int,
        val borderSubtle: Int,
        val strong: Int,
        val text: Int,
        val muted: Int,
        val primary: Int,
        val primaryDark: Int,
        val primarySoft: Int,
        val primarySoftBorder: Int,
        val successText: Int,
        val warningText: Int,
        val warningBg: Int,
    )

    private enum class QrScanMode {
        PAIRING,
        PACKAGE,
    }

    companion object {
        private const val dispatchPollIntervalMs = 5_000L
        private const val extraSmokeApiUrl = "pl.dlaflow.mobile.SMOKE_API_URL"
        private const val extraSmokePairingCode = "pl.dlaflow.mobile.SMOKE_PAIRING_CODE"
        private const val extraSmokePairingDeviceName = "pl.dlaflow.mobile.SMOKE_PAIRING_DEVICE_NAME"
        private const val systemSplashFallbackDelayMs = 1_200L
        private const val sessionTransitionMinimumVisibleMs = 950L
        private const val photoResultRequestCodeMin = 4_200
        private const val photoResultRequestCodeMax = 65_000
        private val sessionTransitionSteps = listOf("Telefon", "Sesja", "Zadania", "Start")
    }
}

internal fun callerIdMissingPermissionMessage(needsPhoneState: Boolean, needsContacts: Boolean): String {
    return when {
        needsPhoneState && needsContacts -> "Caller ID wymaga zgody na telefon i kontakty, żeby pokazywać kartę także dla zapisanych klientów."
        needsContacts -> "Caller ID wymaga zgody na kontakty, żeby działać dla numerów zapisanych w telefonie."
        needsPhoneState -> "Caller ID wymaga zgody na stan telefonu, żeby karta pojawiała się przy połączeniu."
        else -> "Caller ID wymaga jeszcze zgody systemowej."
    }
}

private fun MobileProductFilter.toFeatureProductsFilter(): pl.dlaflow.mobile.feature.products.ProductsFilter = when (this) {
    MobileProductFilter.ALL -> pl.dlaflow.mobile.feature.products.ProductsFilter.ALL
    MobileProductFilter.LOW_STOCK -> pl.dlaflow.mobile.feature.products.ProductsFilter.LOW_STOCK
    MobileProductFilter.NO_IMAGE -> pl.dlaflow.mobile.feature.products.ProductsFilter.NO_IMAGE
    MobileProductFilter.HAS_VARIANTS -> pl.dlaflow.mobile.feature.products.ProductsFilter.HAS_VARIANTS
}

private fun MobileProductQuickEditField.toFeatureField(): pl.dlaflow.mobile.feature.products.ProductQuickEditField = when (this) {
    MobileProductQuickEditField.GROSS_PRICE -> pl.dlaflow.mobile.feature.products.ProductQuickEditField.GROSS_PRICE
    MobileProductQuickEditField.STOCK -> pl.dlaflow.mobile.feature.products.ProductQuickEditField.STOCK
}

private fun MobileVariantQuickEditField.toFeatureField(): pl.dlaflow.mobile.feature.products.VariantQuickEditField = when (this) {
    MobileVariantQuickEditField.PRICE -> pl.dlaflow.mobile.feature.products.VariantQuickEditField.PRICE
    MobileVariantQuickEditField.STOCK -> pl.dlaflow.mobile.feature.products.VariantQuickEditField.STOCK
}

private fun MobileNotificationFilter.toFeatureNotificationFilter(): NotificationFilter = when (this) {
    MobileNotificationFilter.ALL -> NotificationFilter.ALL
    MobileNotificationFilter.ATTENTION -> NotificationFilter.ATTENTION
    MobileNotificationFilter.UNREAD -> NotificationFilter.UNREAD
}

private fun NotificationItem.toMobileAssistantNotification() = MobileAssistantNotification(
    id = id,
    title = title,
    description = description,
    tone = when (tone) {
        NotificationTone.Neutral -> "neutral"
        NotificationTone.Info -> "info"
        NotificationTone.Success -> "success"
        NotificationTone.Attention -> "warning"
    },
    source = source,
    account = account,
    occurredAt = occurredAt,
    readAt = readAt,
    mobileAction = MobileNotificationAction(
        type = when (destination) {
            NotificationDestination.Orders -> "OPEN_ORDERS"
            NotificationDestination.Products -> "OPEN_PRODUCTS"
            NotificationDestination.Messages -> "OPEN_MESSAGES"
            NotificationDestination.PhotoTasks -> "OPEN_PHOTO_TASKS"
            NotificationDestination.LogsSummary -> "OPEN_LOGS_SUMMARY"
            NotificationDestination.ContactAdmin -> "CONTACT_ADMIN"
            NotificationDestination.Unsupported -> ""
        },
        label = actionLabel.orEmpty(),
    ),
)
