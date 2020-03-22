package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.ServiceManager

/**
 * @author Shenluw
 * created: 2020/3/21 21:17
 */
interface QuotesService {
    companion object {
        val instance: QuotesService
            get() = ServiceManager.getService(QuotesService::class.java)
    }

    fun init()


    /**
     * 开启行情订阅
     */
    fun start()

    /**
     * 停止行情订阅
     */
    fun stop()

    /**
     * 更新订阅列表
     */
    fun updateSubscribe()

    fun register(listener: QuotesListener)

    fun unregister(listener: QuotesListener)

    interface QuotesListener {
        fun quoteChange(stockInfo: StockInfo)
    }

}

