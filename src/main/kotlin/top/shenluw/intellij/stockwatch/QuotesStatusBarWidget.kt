package top.shenluw.intellij.stockwatch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.util.messages.MessageBusConnection
import top.shenluw.intellij.Application
import top.shenluw.intellij.CurrentProject
import top.shenluw.intellij.stockwatch.utils.ColorUtil
import java.text.DecimalFormat
import java.util.*
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

    private var nameStrategy: NameStrategy = FullNameStrategy(false)
    private var msgConn: MessageBusConnection? = null

    override fun ID(): String {
        return "QuotesStatusBarWidget"
    }

    override fun getComponent(): JComponent {
        container = JPanel()
        return container!!
    }

    override fun install(statusBar: StatusBar) {
        CurrentProject = statusBar.project

        QuotesService.instance.init()

        msgConn = Application?.messageBus?.connect()

        msgConn?.subscribe(QuotesTopic, this)

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
            stocks[stockInfo.symbol] = label
        } else {
            label.text = text
        }
        label.toolTipText = toolTipText(stockInfo)

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

    override fun symbolChange(symbols: SortedSet<String>) {
        val iterator = stocks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in symbols) {
                container?.remove(entry.value)
                iterator.remove()
            }
        }
    }

    override fun toggle(enable: Boolean) {
        container?.isVisible = enable
    }

    override fun settingChange() {
        val patternSetting = Settings.instance.patternSetting
            ?: PatternSetting(true, 0, false)

        if (patternSetting.fullName) {
            nameStrategy = FullNameStrategy(patternSetting.useSymbol)
        } else {
            nameStrategy = PrefixNameStrategy(patternSetting.namePrefix)
        }
    }

    private val formatCache = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat {
            return DecimalFormat("0.00")
        }
    }

    private fun toString(stockInfo: StockInfo): String {
        val name = nameStrategy.transform(stockInfo)
        return "[$name ${stockInfo.price}|${formatCache.get().format(stockInfo.percentage?.times(100))}%]"
    }

    private fun toolTipText(info: StockInfo): String {
        return """
            今开: ${info.openPrice} 昨收: ${info.preClose}          
            最高: ${info.high} 最低: ${info.low}
            现价: ${info.price} 成交量: ${formatVolume(info.volume)}            
        """.trimIndent()
    }

    private fun formatVolume(value: Long?): String {
        if (value == null) {
            return "-"
        }
        if (value > 1000000) {
            val v = value / 1000000
            return formatCache.get().format(v) + "MM"
        }
        if (value > 1000) {
            val v = value / 1000
            return formatCache.get().format(v) + "K"
        }
        return value.toString()
    }

    override fun dispose() {
        stocks.clear()
        msgConn?.disconnect()
        msgConn = null
        container = null
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