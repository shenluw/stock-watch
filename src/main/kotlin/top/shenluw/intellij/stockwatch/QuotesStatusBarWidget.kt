package top.shenluw.intellij.stockwatch

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.text.DateFormatUtil
import org.apache.commons.lang.text.StrLookup
import org.apache.commons.lang.text.StrSubstitutor
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.utils.ColorUtil
import top.shenluw.intellij.stockwatch.utils.Images
import top.shenluw.intellij.stockwatch.utils.TradingUtil
import top.shenluw.intellij.stockwatch.utils.TrendImages
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseEvent
import java.text.DecimalFormat
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Shenluw
 * created: 2020/3/22 20:59
 */
class QuotesStatusBarWidget(var project: Project?) : CustomStatusBarWidget, QuotesService.QuotesListener {

    private var container: JPanel? = null
    private val stocks = hashMapOf<String, JLabel>()
    private var symbols = emptySet<String>()

    private var msgConn: MessageBusConnection? = null

    override fun ID(): String {
        return "QuotesStatusBarWidget"
    }

    override fun getComponent(): JComponent {
        container = JPanel()
        return container!!
    }

    override fun install(statusBar: StatusBar) {
        Disposer.register(Application!!, Images)

        symbols = TradingUtil.filterSymbols(Settings.instance.symbols)

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
            registerItemClickListener(symbol, label)
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

        symbolChange(TradingUtil.filterSymbols(settings.symbols))

        val client = QuotesService.instance.getDataSourceClient()
        stocks.forEach { (symbol, _) ->
            client?.getStockInfo(symbol)?.let { info ->
                quoteChange(info)
            }
        }
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

        val pattern = setting.pattern

        return StrSubstitutor(MyStrLookup(mapOf(
            "name" to stockInfo.name,
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

        fun substring(text: String, start: Int, end: Int): String {
            val length = text.length
            if (start > length) {
                return text
            }
            if (end > length) {
                return text.substring(start)
            }
            return text.substring(start, end)
        }

        override fun lookup(key: String?): String? {
            if (key.isNullOrBlank()) {
                return key
            }

            return if (map == null) {
                "--"
            } else {
                var obj = map[key]
                if (obj != null) {
                    return obj.toString()
                }
                var index = key.indexOf(":")
                if (index > 0) {
                    val newKey = key.substring(0, index)
                    obj = map[newKey]
                    if (obj == null) {
                        return "--"
                    }

                    val sub = key.substring(index + 1)
                    index = sub.indexOf(',')
                    return try {
                        if (index > 0) {
                            val start = sub.substring(0, index).toInt()
                            val end = sub.substring(index + 1).toInt()
                            substring(obj.toString(), start, end)
                        } else {
                            val len = sub.toInt()
                            substring(obj.toString(), 0, len)
                        }
                    } catch (ignore: Exception) {
                        obj.toString()
                    }
                }

                return "--"
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
        this.project = null
    }

    private fun registerItemClickListener(symbol: String, component: JComponent) {

        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                val settings = Settings.instance

                TrendImages.createTrendImage(
                    symbol,
                    settings.trendType,
                    settings.trendPopupWidth,
                    settings.trendPopupHeight,
                    project
                ) {
                    showPopup(TrendImages.createImageView(it), event.component)
                }
                return true
            }
        }.installOn(component)
    }

    private fun showPopup(image: JComponent, target: Component) {
        val builder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(image, null)
        val popup = builder.createPopup()

        val settings = Settings.instance

        ColorUtil.getColor(settings.trendPopupBackground)?.apply {
            popup.content.background = this
        }
        val dimension: Dimension = popup.content.preferredSize
        val at = Point(0, -dimension.height)
        popup.show(RelativePoint(target, at))
    }
}

class QuotesStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "QuotesStatusBarWidget"
    }

    override fun getDisplayName(): String {
        return PLUGIN_NAME
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return QuotesStatusBarWidget(project)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}