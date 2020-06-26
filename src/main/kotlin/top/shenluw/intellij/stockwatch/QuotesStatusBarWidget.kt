package top.shenluw.intellij.stockwatch

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.text.DateFormatUtil
import org.apache.commons.lang.text.StrLookup
import org.apache.commons.lang.text.StrSubstitutor
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.Application
import top.shenluw.intellij.CurrentProject
import top.shenluw.intellij.stockwatch.utils.ColorUtil
import top.shenluw.intellij.stockwatch.utils.TradingUtil
import java.text.DecimalFormat
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
    private var symbols = emptySet<String>()

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

        symbols = Settings.instance.getRealSymbols()

        nameStrategy = createNameStrategy()

        QuotesService.instance.init()

        msgConn = Application?.messageBus?.connect()

        msgConn?.subscribe(QuotesTopic, this)

        container?.isVisible = Settings.instance.enabled

        if (Settings.instance.enabled) {
            runAsync {
                QuotesService.instance.start()
            }
        }
    }

    override fun quoteChange(stockInfo: StockInfo) {
        val symbol = stockInfo.symbol
        if (symbol !in symbols) {
            return
        }

        var label = stocks[symbol]

        val text = formatStatusBarItemText(stockInfo)
        if (label == null) {
            label = JLabel(text)
            container?.add(label)
            stocks[symbol] = label
        } else {
            label.text = text
        }
        label.toolTipText = toolTipText(stockInfo)

        var color: String? = null
        var percentage: Double? = stockInfo.percentage

        val timestamp = stockInfo.timestamp
        if (timestamp != null && Settings.instance.preAndAfterTrading) {
            if (TradingUtil.isPreTimeRange(timestamp)) {
                percentage = stockInfo.prePercentage
            }
            if (TradingUtil.isAfterTimeRange(timestamp)) {
                percentage = stockInfo.afterPercentage
            }
        }

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

    override fun symbolChange(symbols: MutableSet<String>) {
        this.symbols = symbols
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

        container?.removeAll()
        stocks.clear()
    }

    override fun settingChange() {
        val settings = Settings.instance
        nameStrategy = createNameStrategy()

        symbolChange(settings.getRealSymbols())

        val client = QuotesService.instance.getDataSourceClient()
        stocks.forEach { (symbol, _) ->
            client?.getStockInfo(symbol)?.let { info ->
                quoteChange(info)
            }
        }
    }

    private fun createNameStrategy(): NameStrategy {
        val patternSetting = Settings.instance.patternSetting
        return PrefixNameStrategy(patternSetting.namePrefix)
    }

    private val formatCache = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat {
            return DecimalFormat("0.00")
        }
    }

    private fun toolTipText(info: StockInfo): String {
        return """
            今开: ${info.openPrice} 昨收: ${info.preClose}          
            最高: ${info.high} 最低: ${info.low}
            现价: ${info.price} 成交量: ${formatVolume(info.volume)}            
            ${info.timestamp?.let { DateFormatUtil.formatTimeWithSeconds(it) }}            
        """.trimIndent()
    }

    private fun formatStatusBarItemText(stockInfo: StockInfo): String {
        val setting = Settings.instance

        val name = nameStrategy.transform(stockInfo)
        var price: Double? = null
        var percentage = stockInfo.percentage
        val timestamp = stockInfo.timestamp
        if (timestamp != null && Settings.instance.preAndAfterTrading) {
            if (TradingUtil.isPreTimeRange(timestamp)) {
                price = stockInfo.prePrice
                percentage = stockInfo.prePercentage
            }
            if (TradingUtil.isAfterTimeRange(timestamp)) {
                price = stockInfo.afterPrice
                percentage = stockInfo.afterPercentage
            }
        }
        if (price == null) {
            price = stockInfo.price
        }

        val pattern = setting.patternSetting.pattern

        return StrSubstitutor(MyStrLookup(mapOf(
            "name" to name,
            "symbol" to stockInfo.symbol,
            "openPrice" to stockInfo.openPrice,
            "preClose" to stockInfo.preClose,
            "price" to price,
            "high" to stockInfo.high,
            "low" to stockInfo.low,
            "volume" to formatVolume(stockInfo.volume),
            "prePrice" to stockInfo.prePrice,
            "afterPrice" to stockInfo.afterPrice,
            "percentage" to formatCache.get().format(percentage?.times(100)),
            "prePercentage" to formatCache.get().format(stockInfo.prePercentage?.times(100)),
            "afterPercentage" to formatCache.get().format(stockInfo.afterPercentage?.times(100)),
            "timestamp" to stockInfo.timestamp?.let { DateFormatUtil.formatTimeWithSeconds(it) }
        )))
            .replace(pattern)
    }

    private class MyStrLookup(private val map: Map<*, *>?) : StrLookup() {

        override fun lookup(key: String?): String? {
            return if (map == null) {
                "--"
            } else {
                val obj = map[key] ?: return "--"
                return obj.toString()
            }
        }
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