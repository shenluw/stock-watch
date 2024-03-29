package top.shenluw.intellij

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import top.shenluw.intellij.stockwatch.PLUGIN_ID

/**
 * @author Shenluw
 * created: 2020/3/21 17:58
 */
inline val Application get() = ApplicationManager.getApplication()

inline fun notifyMsg(
    title: String, msg: String,
    type: NotificationType = NotificationType.INFORMATION,
    listener: NotificationListener? = null
) {
    val notification = Notification(PLUGIN_ID, title, msg, type)
    listener?.apply { notification.setListener(this) }
    Notifications.Bus.notify(notification)
}

inline fun invokeLater(crossinline block: () -> Unit) {
    if (Application.isDispatchThread) {
        block.invoke()
    } else {
        Application.invokeLater { block.invoke() }
    }
}

inline fun invokeLater(runnable: Runnable) {
    if (Application.isDispatchThread) {
        runnable.run()
    } else {
        Application.invokeLater(runnable)
    }
}