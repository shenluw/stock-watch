package top.shenluw.intellij.stockwatch.impl

import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.castSafelyTo
import top.shenluw.intellij.Application
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.client.ScriptPollClient
import top.shenluw.intellij.stockwatch.client.TigerClient
import top.shenluw.intellij.stockwatch.client.TigerPollClient
import top.shenluw.intellij.stockwatch.utils.TradingUtil
import java.net.URL

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
class QuotesServiceImpl : QuotesService, Disposable {
    private var initialized = false
    private var disposed = false

    private var dataSourceClient: DataSourceClient<DataSourceSetting>? = null

    @Synchronized
    override fun init() {
        if (initialized) return
        Disposer.register(Application, this)
        initialized = true
    }

    @Synchronized
    override fun start() {
        val src = getActiveDataSourceSetting() ?: return
        if (!src.isValid()) {
            return
        }
        val symbols = TradingUtil.filterSymbols(Settings.instance.symbols)

        try {
            if (dataSourceClient != null) {
                dataSourceClient?.close()
            }
            dataSourceClient = createDataSourceClient(src)
            dataSourceClient?.start(src, symbols)
        } catch (e: Exception) {
            notifyMsg("start error", e.message ?: "", NotificationType.ERROR)
        }
        Application.messageBus.syncPublisher(QuotesTopic).toggle(true)

    }

    @Synchronized
    override fun stop() {
        dataSourceClient?.close()
        dataSourceClient = null
    }

    @Synchronized
    override fun updateSubscribe() {
        val symbols = TradingUtil.filterSymbols(Settings.instance.symbols)

        dataSourceClient?.update(symbols)
        Application.messageBus.syncPublisher(QuotesTopic).symbolChange(symbols)

    }

    @Synchronized
    override fun close() {
        stop()
        Application.messageBus.syncPublisher(QuotesTopic).toggle(false)
    }

    override fun getActiveDataSourceSetting(): DataSourceSetting? {
        val sourceId = Settings.instance.useDataSourceId

        if (sourceId == ScriptPollDataSourceSetting::class.simpleName) {
            return Settings.instance.scriptPollDataSourceSetting
        }

        if (sourceId == TigerDataSourceSetting::class.simpleName) {
            return Settings.instance.tigerDataSourceSetting
        }
        if (sourceId == TigerPollDataSourceSetting::class.simpleName) {
            return Settings.instance.tigerPollDataSourceSetting
        }
        return null
    }

    override fun createDataSourceClient(dataSourceSetting: DataSourceSetting): DataSourceClient<DataSourceSetting>? {
        if (!dataSourceSetting.isValid()) {
            return null
        }
        if (dataSourceSetting is TigerDataSourceSetting) {
            return TigerClient().castSafelyTo()
        }
        if (dataSourceSetting is TigerPollDataSourceSetting) {
            return TigerPollClient().castSafelyTo()
        }
        if (dataSourceSetting is ScriptPollDataSourceSetting) {
            return ScriptPollClient().castSafelyTo()
        }
        return null
    }

    override fun getDataSourceClient(): DataSourceClient<DataSourceSetting>? {
        return dataSourceClient
    }

    override fun getTrendChart(symbol: String, type: QuotesService.TrendType): URL? {
        return dataSourceClient?.getTrendChart(symbol, type)
    }

    override fun dispose() {
        stop()
        dataSourceClient = null
        disposed = true
    }
}