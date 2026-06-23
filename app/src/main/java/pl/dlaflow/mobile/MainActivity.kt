package pl.dlaflow.mobile

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Intent
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val cameraRequestCode = 4101
    private val galleryRequestCode = 4102
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
    private lateinit var root: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var statusView: TextView
    private lateinit var baseUrlInput: EditText
    private lateinit var pairingCodeInput: EditText
    private lateinit var callerIdTestPhoneInput: EditText
    private var sessionTransitionOverlay: DlaFlowSessionTransitionOverlay? = null
    private var sessionTransitionStartedAt: Long = 0L
    private var callerIdPreview: MobileCallerIdLookup? = null
    private var focusedPhotoTaskId: String? = null
    private var focusedPhotoTaskView: View? = null
    private var lastDispatchedPhotoTaskId: String? = null
    private var photoTasks: List<MobilePhotoTask> = emptyList()
    private var pendingCameraPhotoFile: File? = null
    private var pendingCameraPhotoUri: Uri? = null
    private var pendingPhotoTaskId: String? = null
    private var pendingSmokeApiUrl: String? = null
    private var pendingSmokePairingCode: String? = null
    private var contentReadyForDisplay = false
    private var keepSystemSplashVisible = true
    private var session: MobileSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplashVisible }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        super.onCreate(savedInstanceState)
        sessionStore = MobileSessionStore(this)
        createNotificationChannel()
        handleLaunchIntent(intent)
        val hasSavedSession = sessionStore.readToken().isNotBlank()
        render()
        showSessionTransition(activeStepIndex = 0, progress = 18, animateIn = false)
        sessionTransitionOverlay?.post {
            keepSystemSplashVisible = false
            if (consumeSmokePairingIntent()) {
                return@post
            } else if (hasSavedSession) {
                verifySavedSession(showInitialTransition = false)
            } else {
                setStatus("Przygotowujemy aplikację...")
                completeSessionTransition {
                    setStatus("Sparuj telefon z panelem.")
                }
            }
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
        refreshPhotoTasks(showLoading = false)
    }

    override fun onDestroy() {
        stopPhotoTaskDispatchPolling()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val pairingScan = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (pairingScan != null) {
            handlePairingQrResult(pairingScan.contents)
            return
        }

        if (requestCode == callerIdRoleRequestCode) {
            render()
            setStatus(
                when {
                    isCallerIdOperational() -> "Caller ID włączony."
                    isCallerIdRoleHeld() -> "Caller ID wymaga jeszcze zgody na stan telefonu."
                    else -> "Caller ID nie jest jeszcze włączony w systemie."
                },
            )
            return
        }

        if (requestCode != cameraRequestCode && requestCode != galleryRequestCode) {
            return
        }

        if (resultCode != RESULT_OK) {
            setStatus("Nie wybrano zdjęcia.")
            clearPendingCameraPhoto()
            return
        }

        val taskId = pendingPhotoTaskId ?: return
        when (requestCode) {
            cameraRequestCode -> uploadCameraResult(taskId)
            galleryRequestCode -> uploadGalleryResult(taskId, data?.data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraPermissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingPhotoTaskId?.let { openCamera(it) }
        } else if (requestCode == cameraPermissionRequestCode) {
            setStatus("Aparat nie ma zgody. Możesz wybrać zdjęcie z telefonu.")
        } else if (requestCode == notificationPermissionRequestCode) {
            setStatus(if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) "Powiadomienia zadań włączone." else "Bez powiadomień otwórz aplikację, żeby zobaczyć zadania.")
        } else if (requestCode == phoneStatePermissionRequestCode) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                requestCallerIdRole()
            } else {
                setStatus("Caller ID wymaga zgody na stan telefonu, żeby karta znikała po rozmowie.")
                render()
            }
        }
    }

    private fun render() {
        val theme = mobileTheme()
        focusedPhotoTaskView = null
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(22))
            setBackgroundColor(theme.appBg)
        }

        scrollView = ScrollView(this).apply {
            alpha = if (contentReadyForDisplay) 1f else 0f
            fitsSystemWindows = true
            setBackgroundColor(theme.appBg)
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
            addView(root)
        }

        window.statusBarColor = theme.appBg
        window.navigationBarColor = theme.appBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (theme.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val screen = FrameLayout(this).apply {
            addView(scrollView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            sessionTransitionOverlay = DlaFlowSessionTransitionOverlay(this@MainActivity).also { overlay ->
                addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }

        setContentView(screen)
        renderHeader()
        statusView = label("", size = 12f, color = theme.muted, top = 12)
        root.addView(statusView)
        if (session == null) {
            renderPairingCard()
            renderCallerIdCard()
        } else {
            renderPhotoTaskHero()
            renderPhotoTasksCard()
            renderQuickStatusGrid()
            renderCallerIdCard()
            renderConnectedCard()
        }
        scrollFocusedPhotoTaskIntoView()
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

    private fun renderPairingCard() {
        val theme = mobileTheme()
        val card = card(prominent = true)
        card.addView(sectionLabel("POŁĄCZENIE Z PANELEM"))
        card.addView(label("Sparuj telefon z DlaFlow", size = 20f, color = theme.strong, bold = true, top = 8))
        card.addView(label("W panelu otwórz Integracje, wybierz Wtyczki i zeskanuj kod QR z Mobile Assistant.", size = 13f, color = theme.muted, top = 8))

        baseUrlInput = input("Adres API", sessionStore.readBaseUrl())
        pairingCodeInput = input("Kod parowania", "")

        card.addView(baseUrlInput)
        card.addView(pairingCodeInput)
        card.addView(secondaryButton("Skanuj kod QR") { scanPairingQr() })
        card.addView(primaryButton("Sparuj telefon") { pairDevice() })
        root.addView(card)
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
            card.addView(secondaryButton("Odłącz ten telefon") {
                sessionStore.clear()
                session = null
                photoTasks = emptyList()
                focusedPhotoTaskId = null
                stopPhotoTaskDispatchPolling()
                render()
            })
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
            card.addView(secondaryButton("Odśwież zadania") { refreshPhotoTasks() })
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
                taskCard.addView(primaryButton("Zrób zdjęcie") { requestCamera(task.id) })
                taskCard.addView(secondaryButton("Wybierz zdjęcie") { openGallery(task.id) })
                taskCard.addView(secondaryButton("Zakończ zadanie") { completePhotoTask(task.id) })
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
            isCallerIdRoleHeld() -> "Caller ID wymaga jeszcze zgody na stan telefonu, żeby karta znikała po rozmowie."
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
                MobileApiClient(baseUrl).verifySession(token)
            }.onSuccess { verifiedSession ->
                runOnUiThread {
                    session = verifiedSession
                    render()
                    showSessionTransition(activeStepIndex = 2, progress = 78)
                    setStatus("Telefon jest połączony.")
                    completeSessionTransition {
                        requestNotificationPermissionIfNeeded()
                        startPhotoTaskDispatchPolling()
                        refreshPhotoTasks(showLoading = false)
                    }
                }
            }.onFailure {
                runOnUiThread {
                    if (!handleMobileApiFailure(it, "Zapisane połączenie wygasło. Sparuj telefon ponownie.")) {
                        hideSessionTransition()
                    }
                }
            }
        }
    }

    private fun pairDevice() {
        val baseUrl = baseUrlInput.text.toString().trim()
        val code = pairingCodeInput.text.toString().trim()

        if (baseUrl.isBlank() || code.isBlank()) {
            setStatus("Wpisz adres API i kod parowania.")
            return
        }

        setStatus("Paruję telefon...")
        showSessionTransition(activeStepIndex = 0, progress = 18)
        executor.execute {
            runCatching {
                val client = MobileApiClient(baseUrl)
                client.completePairing(code, "Telefon DlaFlow")
            }.onSuccess { nextSession ->
                sessionStore.saveSession(baseUrl, nextSession)
                runOnUiThread {
                    updateSessionTransition(activeStepIndex = 1, progress = 46)
                    session = nextSession
                    render()
                    showSessionTransition(activeStepIndex = 2, progress = 78)
                    setStatus("Telefon sparowany.")
                    completeSessionTransition {
                        requestNotificationPermissionIfNeeded()
                        startPhotoTaskDispatchPolling()
                        refreshPhotoTasks(showLoading = false)
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    hideSessionTransition()
                    setStatus(error.message ?: "Nie udało się sparować telefonu.")
                }
            }
        }
    }

    private fun scanPairingQr() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Zeskanuj kod QR z panelu DlaFlow")
            setBeepEnabled(false)
            setOrientationLocked(false)
            initiateScan()
        }
    }

    private fun handlePairingQrResult(rawValue: String?) {
        val code = extractPairingCodeFromQr(rawValue)
        if (code.isBlank()) {
            setStatus("Nie odczytano kodu QR.")
            return
        }

        if (!code.matches(Regex("^[A-Z0-9]{3}-?[A-Z0-9]{3}$"))) {
            setStatus("To nie jest kod parowania DlaFlow.")
            return
        }

        pairingCodeInput.setText(code)
        if (baseUrlInput.text.toString().trim().isBlank()) {
            setStatus("Kod QR odczytany. Wpisz adres API i sparuj telefon.")
            return
        }

        setStatus("Kod QR odczytany.")
        pairDevice()
    }

    private fun extractPairingCodeFromQr(rawValue: String?): String {
        val value = rawValue?.trim().orEmpty()
        val prefix = "dlaflow-pair:v1:"
        val code = if (value.startsWith(prefix, ignoreCase = true)) {
            value.substring(prefix.length)
        } else {
            value
        }

        return code.trim().uppercase(Locale.ROOT).replace(" ", "")
    }

    private fun refreshPhotoTasks(showLoading: Boolean = true) {
        val currentSession = session ?: return
        if (showLoading) {
            setStatus("Odświeżam zadania zdjęciowe...")
        }
        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).listActivePhotoTasks(currentSession.token)
            }.onSuccess { tasks ->
                runOnUiThread {
                    photoTasks = tasks
                    render()
                    setStatus(if (tasks.isEmpty()) "Brak aktywnych zadań." else "Zadania gotowe.")
                }
            }.onFailure { error ->
                runOnUiThread {
                    handleMobileApiFailure(error, "Nie udało się pobrać zadań.")
                }
            }
        }
    }

    private fun startPhotoTaskDispatchPolling() {
        dispatchHandler.removeCallbacks(dispatchRunnable)
        dispatchHandler.post(dispatchRunnable)
    }

    private fun stopPhotoTaskDispatchPolling() {
        dispatchHandler.removeCallbacks(dispatchRunnable)
    }

    private fun checkPhotoTaskDispatch() {
        val currentSession = session ?: return
        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).getPhotoTaskDispatch(currentSession.token)
            }.onSuccess { dispatch ->
                val task = dispatch.pendingOpenTask ?: return@onSuccess
                if (task.id == lastDispatchedPhotoTaskId) {
                    return@onSuccess
                }

                lastDispatchedPhotoTaskId = task.id
                runOnUiThread {
                    focusedPhotoTaskId = task.id
                    showPhotoTaskNotification(task)
                    openPhotoTaskIfAllowed(task)
                    refreshPhotoTasks(showLoading = false)
                }
            }.onFailure { error ->
                runOnUiThread {
                    handleMobileApiFailure(error, "Nie udało się sprawdzić zadań z panelu.", showNonAuthStatus = false)
                }
            }
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notification = NotificationCompat.Builder(this, photoTaskChannelId)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("Zrób zdjęcia produktu")
            .setContentText(task.productName)
            .setContentIntent(PendingIntent.getActivity(this, task.id.hashCode(), createPhotoTaskIntent(task.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(photoTaskNotificationId, notification)
    }

    private fun createPhotoTaskIntent(taskId: String): Intent {
        return Intent(this, MainActivity::class.java)
            .putExtra(extraFocusPhotoTaskId, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val taskId = intent?.getStringExtra(extraFocusPhotoTaskId).orEmpty()
        if (taskId.isNotBlank()) {
            focusedPhotoTaskId = taskId
            lastDispatchedPhotoTaskId = taskId
        }
        val smokeApiUrl = intent?.getStringExtra(extraSmokeApiUrl).orEmpty()
        val smokePairingCode = intent?.getStringExtra(extraSmokePairingCode).orEmpty()
        if (smokeApiUrl.isNotBlank() && smokePairingCode.isNotBlank()) {
            pendingSmokeApiUrl = smokeApiUrl
            pendingSmokePairingCode = smokePairingCode
        }
    }

    private fun consumeSmokePairingIntent(): Boolean {
        val apiUrl = pendingSmokeApiUrl?.trim().orEmpty()
        val pairingCode = pendingSmokePairingCode?.trim().orEmpty()
        if (apiUrl.isBlank() || pairingCode.isBlank() || !::baseUrlInput.isInitialized || !::pairingCodeInput.isInitialized) {
            return false
        }

        pendingSmokeApiUrl = null
        pendingSmokePairingCode = null
        baseUrlInput.setText(apiUrl)
        pairingCodeInput.setText(pairingCode)
        pairDevice()

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
        pendingPhotoTaskId = taskId
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(taskId)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
        }
    }

    private fun openCamera(taskId: String) {
        pendingPhotoTaskId = taskId
        val photoFile = runCatching { createCameraPhotoFile() }.getOrNull()
        if (photoFile == null) {
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
            setStatus("Nie znaleziono aplikacji aparatu.")
            return
        }
        startActivityForResult(intent, cameraRequestCode)
    }

    private fun openGallery(taskId: String) {
        pendingPhotoTaskId = taskId
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, galleryRequestCode)
    }

    private fun uploadCameraResult(taskId: String) {
        val uri = pendingCameraPhotoUri
        val file = pendingCameraPhotoFile
        val bytes = when {
            uri != null -> contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
            file != null && file.exists() -> file.readBytes()
            else -> null
        }

        if (bytes == null || bytes.isEmpty()) {
            clearPendingCameraPhoto()
            setStatus("Aparat nie zapisał pełnego zdjęcia.")
            return
        }

        uploadPhoto(taskId, bytes, file?.name ?: "zdjecie-z-telefonu.jpg", "image/jpeg")
    }

    private fun uploadGalleryResult(taskId: String, uri: Uri?) {
        if (uri == null) {
            setStatus("Nie wybrano zdjęcia.")
            return
        }

        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        }
        if (bytes == null) {
            setStatus("Nie udało się odczytać zdjęcia.")
            return
        }

        uploadPhoto(taskId, bytes, "zdjecie-z-telefonu", contentResolver.getType(uri) ?: "image/jpeg")
    }

    private fun uploadPhoto(taskId: String, bytes: ByteArray, fileName: String, mimeType: String) {
        val currentSession = session ?: return
        setStatus("Wysyłam pełne zdjęcie (${formatBytes(bytes.size)})...")
        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).uploadPhotoTaskMedia(
                    token = currentSession.token,
                    taskId = taskId,
                    imageBytes = bytes,
                    fileName = fileName,
                    mimeType = mimeType,
                )
            }.onSuccess {
                runOnUiThread {
                    clearPendingCameraPhoto()
                    setStatus("Zdjęcie dodane do produktu.")
                    refreshPhotoTasks()
                }
            }.onFailure { error ->
                runOnUiThread {
                    clearPendingCameraPhoto()
                    handleMobileApiFailure(error, "Nie udało się wysłać zdjęcia.")
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
        pendingCameraPhotoFile = null
    }

    private fun completePhotoTask(taskId: String) {
        val currentSession = session ?: return
        setStatus("Kończę zadanie...")
        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).completePhotoTask(currentSession.token, taskId)
            }.onSuccess {
                runOnUiThread {
                    photoTasks = photoTasks.filterNot { it.id == taskId }
                    render()
                    setStatus("Zadanie zakończone.")
                }
            }.onFailure { error ->
                runOnUiThread {
                    handleMobileApiFailure(error, "Nie udało się zakończyć zadania.")
                }
            }
        }
    }

    private fun requestCallerIdRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setStatus("Włącz Caller ID w ustawieniach domyślnych aplikacji telefonu.")
            return
        }

        if (!hasPhoneStatePermission()) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), phoneStatePermissionRequestCode)
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

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(photoTaskChannelId, "Zdjęcia produktów", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Zadania zdjęciowe wysłane z panelu DlaFlow."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun testCallerIdLookup() {
        val currentSession = session ?: return
        val phone = callerIdTestPhoneInput.text.toString().trim()
        if (phone.isBlank()) {
            setStatus("Wpisz numer do testu Caller ID.")
            return
        }

        setStatus("Sprawdzam numer...")
        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).lookupCallerId(currentSession.token, phone)
            }.onSuccess { lookup ->
                runOnUiThread {
                    callerIdPreview = lookup
                    render()
                    setStatus(if (lookup.primaryOrder == null) "Brak zamówienia dla numeru." else "Znaleziono zamówienie.")
                }
            }.onFailure { error ->
                runOnUiThread {
                    handleMobileApiFailure(error, "Nie udało się sprawdzić numeru.")
                }
            }
        }
    }

    private fun handleMobileApiFailure(error: Throwable, fallbackMessage: String, showNonAuthStatus: Boolean = true): Boolean {
        if (isMobileSessionRevoked(error)) {
            clearRevokedSession()
            return true
        }

        if (showNonAuthStatus) {
            setStatus(error.message ?: fallbackMessage)
        }

        return false
    }

    private fun isMobileSessionRevoked(error: Throwable): Boolean {
        return error is MobileApiException && (error.statusCode == 401 || error.code == "AUTH_REQUIRED")
    }

    private fun clearRevokedSession() {
        sessionStore.clearSession()
        stopPhotoTaskDispatchPolling()
        clearPendingCameraPhoto()
        session = null
        photoTasks = emptyList()
        callerIdPreview = null
        focusedPhotoTaskId = null
        focusedPhotoTaskView = null
        lastDispatchedPhotoTaskId = null
        contentReadyForDisplay = true
        render()
        setStatus("Telefon został odłączony w panelu. Sparuj go ponownie.")
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

    private fun isCallerIdOperational(): Boolean {
        return isCallerIdRoleHeld() && hasPhoneStatePermission()
    }

    private fun setStatus(value: String) {
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
        if (::scrollView.isInitialized) {
            scrollView.animate().cancel()
            scrollView.animate().alpha(1f).setDuration(220L).start()
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

    companion object {
        private const val dispatchPollIntervalMs = 5_000L
        private const val extraFocusPhotoTaskId = "pl.dlaflow.mobile.FOCUS_PHOTO_TASK_ID"
        private const val extraSmokeApiUrl = "pl.dlaflow.mobile.SMOKE_API_URL"
        private const val extraSmokePairingCode = "pl.dlaflow.mobile.SMOKE_PAIRING_CODE"
        private const val photoTaskChannelId = "product-photo-tasks"
        private const val photoTaskNotificationId = 2701
        private const val sessionTransitionMinimumVisibleMs = 950L
        private val sessionTransitionSteps = listOf("Telefon", "Sesja", "Zadania", "Start")
    }
}
