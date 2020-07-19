package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.ServiceManager
import java.net.URL

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

    fun createDataSourceClient(dataSourceSetting: DataSourceSetting): DataSourceClient<DataSourceSetting>?

    /**
     * 获取当前使用的数据源client
     */
    fun getDataSourceClient(): DataSourceClient<DataSourceSetting>?

    /**
     * 获取趋势图 图像地址
     * @return 如果返回值为null， 表示不支持获取
     */
    fun getTrendChart(symbol: String, type: TrendType): URL?

    enum class TrendType {

        /* 分钟级别 相当于实时 */
        MINUTE,

        /* 日K */
        DAILY,

        /* 周K */
        WEEKLY,

        /* 月K */
        MONTHLY
    }

    interface QuotesListener {
        /**
         * 行情变化调用
         */
        fun quoteChange(stockInfo: StockInfo)

        /**
         * 监听列表变化
         * @param symbols 当前列表
         */
        fun symbolChange(symbols: MutableSet<String>)

        /**
         * 是否开启插件功能
         */
        fun toggle(enable: Boolean)

        /**
         * 设置变化
         */
        fun settingChange()
    }

}

