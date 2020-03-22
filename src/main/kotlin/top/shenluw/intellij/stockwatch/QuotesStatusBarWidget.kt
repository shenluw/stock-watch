package top.shenluw.intellij.stockwatch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Shenluw
 * created: 2020/3/22 20:59
 */
class QuotesStatusBarWidget : CustomStatusBarWidget, QuotesService.QuotesListener {

    private var container: JPanel? = null
    private val stocks = hashMapOf<String, JLabel>()

    private var nameStrategy: NameStrategy = FullNameStrategy.instance

    override fun ID(): String {
        return "QuotesStatusBarWidget"
    }

    override fun getComponent(): JComponent {
        container = JPanel()
        return container!!
    }

    override fun install(statusBar: StatusBar) {
        QuotesService.instance.init()
        QuotesService.instance.register(this)
        if (Settings.instance.enabled) {
            QuotesService.instance.start()
        }
    }

    override fun quoteChange(stockInfo: StockInfo) {
        var label = stocks[stockInfo.symbol]

        val text = toString(stockInfo)
        if (label == null) {
            label = JLabel(text)
            container?.add(label)
        } else {
            label.text = text
        }
        var color: String? = null
        val percentage = stockInfo.percentage
        if (percentage != null) {
            if (percentage > 0) {
                color = Settings.instance.riseColor
            } else if (percentage < 0) {
                color = Settings.instance.fallColor
            }
        }
        if (color != null) {
            val c = ColorUtil.getColor(color)
            if (c != null) {
                label.foreground = c
            }
        }
    }

    private fun toString(stockInfo: StockInfo): String {
        val name = nameStrategy.transform(stockInfo.name)
        return "$name ${stockInfo.price}|$${stockInfo.percentage}"
    }

    override fun dispose() {
        stocks.clear()
        QuotesService.instance.unregister(this)
    }

}


class QuotesStatusBarWidgetProvider : StatusBarWidgetProvider {
    override fun getWidget(project: Project): StatusBarWidget? {
        return QuotesStatusBarWidget()
    }

    override fun getAnchor(): String {
        return StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)
    }
}