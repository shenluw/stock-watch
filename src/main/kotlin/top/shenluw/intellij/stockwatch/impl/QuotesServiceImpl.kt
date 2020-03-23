package top.shenluw.intellij.stockwatch.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.QuotesService

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
class QuotesServiceImpl : QuotesService, Disposable {
    private var initialized = false
    private var disposed = false

    override fun init() {
        if (initialized) return
        Disposer.register(Application, this)
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun updateSubscribe() {
    }

    override fun dispose() {
        stop()
        disposed = true
    }
}