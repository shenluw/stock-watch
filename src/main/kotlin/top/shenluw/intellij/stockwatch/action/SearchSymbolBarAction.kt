package top.shenluw.intellij.stockwatch.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import top.shenluw.intellij.stockwatch.dialog.SearchStockDialog
import java.lang.ref.WeakReference

/**
 * @author Shenluw
 * created: 2021/8/1 16:48
 */
class SearchSymbolBarAction : AnAction() {

    var ref: WeakReference<SearchStockDialog>? = null

    override fun actionPerformed(e: AnActionEvent) {
        var dialog = ref?.get()
        if (dialog != null) {
            if (!dialog.isDisposed) {
                return
            }
        }
        ref?.clear()
        e.project?.apply {
            dialog = SearchStockDialog(this)
            ref = WeakReference(dialog)
            dialog?.show()
        }

    }
}