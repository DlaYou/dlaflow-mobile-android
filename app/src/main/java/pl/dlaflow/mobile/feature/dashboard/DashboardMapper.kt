package pl.dlaflow.mobile.feature.dashboard

import pl.dlaflow.mobile.MobileAssistantDashboard

internal fun MobileAssistantDashboard.toDashboardContent() = DashboardContent(
    userName = userName,
    tenantName = tenantName,
    todayRevenue = todayRevenue,
    revenueChangePercent = revenueChangePercent,
    kpis = DashboardKpis(
        newOrders = kpis.newOrders,
        toShip = kpis.toShip,
        overdueOrProblems = kpis.overdueOrProblems,
        messages = kpis.messages,
    ),
    notificationSummary = DashboardNotificationSummary(
        unreadCount = notificationSummary.unreadCount,
        unreadAttentionCount = notificationSummary.unreadAttentionCount,
    ),
    notifications = notifications.map { notification ->
        DashboardNotification(
            id = notification.id,
            title = notification.title,
            description = notification.description,
            tone = notification.tone,
            source = notification.source,
            account = notification.account,
            occurredAt = notification.occurredAt,
            readAt = notification.readAt,
            actionType = notification.mobileAction.type,
            actionLabel = notification.mobileAction.label,
        )
    },
    activePhotoTask = activePhotoTask?.let { task ->
        DashboardPhotoTask(
            id = task.id,
            productName = task.productName,
            productSku = task.productSku,
            productImage = task.productImage,
            status = task.status,
            mediaCount = task.mediaCount,
            maxPhotos = task.maxPhotos,
            expiresAt = task.expiresAt,
        )
    },
    callerIdStatus = DashboardCallerIdStatus(
        enabled = callerIdStatus.enabled,
        label = callerIdStatus.label,
    ),
    trend = trend.map { point ->
        DashboardTrendPoint(
            date = point.date,
            orders = point.orders,
            revenue = point.revenue,
        )
    },
    generatedAt = generatedAt,
)
