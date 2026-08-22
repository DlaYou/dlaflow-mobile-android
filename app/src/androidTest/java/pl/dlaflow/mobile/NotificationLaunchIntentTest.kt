package pl.dlaflow.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationLaunchIntentTest {
    @Test
    fun ordersNotificationIntentTargetsOrdersTab() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = DlaFlowDeepLinks.ordersIntent(context)

        assertTrue(intent.getBooleanExtra(DlaFlowDeepLinks.extraOpenOrders, false))
    }

    @Test
    fun customerMessageNotificationIntentTargetsMessagesTab() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = DlaFlowDeepLinks.messagesIntent(context)

        assertTrue(intent.getBooleanExtra(DlaFlowDeepLinks.extraOpenMessages, false))
    }
}
