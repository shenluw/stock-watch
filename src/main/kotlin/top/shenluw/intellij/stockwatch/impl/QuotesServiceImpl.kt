package top.shenluw.intellij.stockwatch.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.*

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
class QuotesServiceImpl : QuotesService, Disposable {
    private var initialized = false
    private var disposed = false

    private var tigerClientService: TigerClientService? = null

    @Synchronized
    override fun init() {
        if (initialized) return
        Disposer.register(Application, this)
        initialized = true
    }

    @Synchronized
    override fun start() {
        if (tigerClientService == null) {
            tigerClientService = TigerClientService()
        }
        val symbols = Settings.instance.symbols
        val src = Settings.instance.dataSourceSetting
        if (src is TigerDataSourceSetting) {
            tigerClientService?.start(src, symbols)
        }
        Application.messageBus.syncPublisher(QuotesTopic).toggle(true)
    }

    @Synchronized
    override fun stop() {
        tigerClientService?.close()
    }

    @Synchronized
    override fun updateSubscribe() {
        val symbols = Settings.instance.symbols
        tigerClientService?.update(symbols)
        Application.messageBus.syncPublisher(QuotesTopic).symbolChange(symbols)

    }

    @Synchronized
    override fun close() {
        stop()
        Application.messageBus.syncPublisher(QuotesTopic).toggle(false)
    }

    override fun dispose() {
        stop()
        tigerClientService = null
        disposed = true
    }
}