package top.shenluw.intellij.stockwatch.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import top.shenluw.intellij.stockwatch.dialog.SearchStockDialog

/**
 * @author Shenluw
 * created: 2021/8/1 16:48
 */
class SearchSymbolBarAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.apply {
            SearchStockDialog(this).show()
        }

    }
}