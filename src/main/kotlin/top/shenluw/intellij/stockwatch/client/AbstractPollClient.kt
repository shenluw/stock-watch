package top.shenluw.intellij.stockwatch.client

import org.apache.commons.collections.CollectionUtils
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.DataSourceClient
import top.shenluw.intellij.stockwatch.DataSourceSetting
import top.shenluw.intellij.stockwatch.QuotesTopic
import top.shenluw.intellij.stockwatch.StockInfo
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * 通过轮询获取股票数据
 *
 * @author Shenluw
 * created: 2020/4/11 14:43
 */
abstract class AbstractPollClient<T : DataSourceSetting> : DataSourceClient<T> {

    /* 轮询间隔 */
    var interval: Long = 10_000

    protected var symbols: List<String>? = null

    private var scheduledService: ScheduledExecutorService? = null

    private val cache = hashMapOf<String, StockInfo>()

    protected abstract fun fetch(): List<StockInfo>

    @Synchronized
    protected fun startPoll() {
        stopScheduledService()

        val interval = max(100, this.interval)

        val scheduledService = Executors.newSingleThreadScheduledExecutor()
        this.scheduledService = scheduledService

        scheduledService.scheduleAtFixedRate({
            val application = Application
            if (!application.isDisposed) {
                val publisher = application.messageBus.syncPublisher(QuotesTopic)
                fetch().forEach {
                    cache[it.symbol] = it
                    publisher.quoteChange(it)
                }
            }
        }, interval, interval, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    override fun update(symbols: MutableSet<String>) {
        if (CollectionUtils.isEqualCollection(this.symbols, symbols)) {
            return
        }
        this.symbols = symbols.toList()

        startPoll()
    }

    override fun close() {
        stopScheduledService()
        cache.clear()
    }

    private fun stopScheduledService() {
        val service = scheduledService
        if (service != null && !service.isShutdown) {
            service.shutdownNow()
        }
        scheduledService = null
    }

    override fun getStockInfo(symbol: String): StockInfo? {
        return cache.getOrDefault(symbol, null)
    }
}