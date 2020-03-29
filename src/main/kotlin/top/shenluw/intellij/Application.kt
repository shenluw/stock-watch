package top.shenluw.intellij

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.bouncycastle.jce.provider.BouncyCastleProvider
import top.shenluw.intellij.stockwatch.PLUGIN_ID
import java.security.Security

/**
 * @author Shenluw
 * created: 2020/3/21 17:58
 */
inline val Application get() = ApplicationManager.getApplication()

var CurrentProject: Project? = null

inline fun notifyMsg(
    title: String, msg: String,
    type: NotificationType = NotificationType.INFORMATION,
    listener: NotificationListener? = null
) {
    Notifications.Bus.notify(
        Notification(PLUGIN_ID, title, msg, type, listener)
    )
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


class MyApplicationInitializedListener : ApplicationInitializedListener {
    override fun componentsInitialized() {
        Security.addProvider(BouncyCastleProvider())
    }
}