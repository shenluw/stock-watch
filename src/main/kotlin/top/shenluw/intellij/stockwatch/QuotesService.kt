package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.ServiceManager
import java.util.*

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
     * 关闭插件功能
     */
    fun close();

    /**
     * 更新订阅列表
     */
    fun updateSubscribe()

    interface QuotesListener {
        /**
         * 行情变化调用
         */
        fun quoteChange(stockInfo: StockInfo)

        /**
         * 监听列表变化
         * @param symbols 当前列表
         */
        fun symbolChange(symbols: SortedSet<String>)

        /**
         * 是否开启插件功能
         */
        fun toggle(enable: Boolean)

    }

}

