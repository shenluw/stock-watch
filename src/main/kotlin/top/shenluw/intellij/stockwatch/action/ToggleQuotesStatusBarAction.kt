package top.shenluw.intellij.stockwatch.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import top.shenluw.intellij.stockwatch.QuotesService
import top.shenluw.intellij.stockwatch.Settings

/**
 * @author shenlw
 * @date 2020/3/29 15:54
 */
class ToggleQuotesStatusBarAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val state = !Settings.instance.enabled

        Settings.instance.enabled = state

        if (state) {
            QuotesService.instance.start()
        } else {
            QuotesService.instance.close()
        }

    }
}