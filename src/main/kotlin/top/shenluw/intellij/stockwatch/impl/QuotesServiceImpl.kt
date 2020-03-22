package top.shenluw.intellij.stockwatch.impl

import com.intellij.openapi.Disposable
import top.shenluw.intellij.stockwatch.QuotesService
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
class QuotesServiceImpl : QuotesService, Disposable {
    private var initialized = false
    private var disposed = false

    private val listeners = CopyOnWriteArrayList<QuotesService.QuotesListener>()

    override fun init() {
        if (initialized) return
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun updateSubscribe() {
    }

    override fun register(listener: QuotesService.QuotesListener) {
        listeners.add(listener)
    }

    override fun unregister(listener: QuotesService.QuotesListener) {
        listeners.remove(listener)
    }

    override fun dispose() {
        stop()
        disposed = true
    }
}