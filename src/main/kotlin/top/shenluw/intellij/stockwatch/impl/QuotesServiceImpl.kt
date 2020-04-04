package top.shenluw.intellij.stockwatch.impl

import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import top.shenluw.intellij.Application
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.client.TigerClientClient

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
class QuotesServiceImpl : QuotesService, Disposable {
    private var initialized = false
    private var disposed = false

    private var dataSourceClient: DataSourceClient? = null

    @Synchronized
    override fun init() {
        if (initialized) return
        Disposer.register(Application, this)
        initialized = true
    }

    @Synchronized
    override fun start() {
        val src = Settings.instance.tigerDataSourceSetting ?: return
        if (!src.isValid()) {
            return
        }
        val symbols = Settings.instance.symbols

        try {
            getDataSourceClient(src)?.start(src, symbols)
        } catch (e: Exception) {
            notifyMsg("start error", e.message ?: "", NotificationType.ERROR)
        }
        Application.messageBus.syncPublisher(QuotesTopic).toggle(true)

    }

    @Synchronized
    override fun stop() {
        dataSourceClient?.close()
    }

    @Synchronized
    override fun updateSubscribe() {
        val symbols = Settings.instance.symbols

        dataSourceClient?.update(symbols)
        Application.messageBus.syncPublisher(QuotesTopic).symbolChange(symbols)

    }

    @Synchronized
    override fun close() {
        if (!Settings.instance.onlyCloseUI) {
            stop()
        }
        Application.messageBus.syncPublisher(QuotesTopic).toggle(false)
    }

    @Synchronized
    override fun getDataSourceClient(dataSourceSetting: DataSourceSetting): DataSourceClient? {
        if (!dataSourceSetting.isValid()) {
            return null
        }
        if (dataSourceSetting is TigerDataSourceSetting) {
            if (dataSourceClient == null || dataSourceClient !is TigerClientClient) {
                dataSourceClient = TigerClientClient()
            }
            return dataSourceClient
        }
        return null
    }

    override fun dispose() {
        stop()
        dataSourceClient = null
        disposed = true
    }
}