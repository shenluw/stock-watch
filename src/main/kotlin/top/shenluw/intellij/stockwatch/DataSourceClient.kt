package top.shenluw.intellij.stockwatch

import org.jetbrains.concurrency.Promise
import java.net.URL

/**
 * @author Shenluw
 * created: 2020/3/29 16:14
 */
interface DataSourceClient<T : DataSourceSetting> {

    fun create(dataSourceSetting: T)

    fun start(symbols: MutableSet<String>)

    fun update(symbols: MutableSet<String>)

    fun close()

    /**
     * 测试配置是否正确
     */
    fun testConfig(dataSourceSetting: T, symbols: Set<String>): Promise<ClientResponse>

    fun getStockInfo(symbol: String): StockInfo?

    /**
     * 根据关键字搜索股票信息
     */
    fun searchStockSummary(keyword: String): List<StockSummary> {
        return emptyList()
    }

    /**
     * 获取趋势图 图像地址
     * @return 如果返回值为null， 表示不支持获取
     */
    fun getTrendChart(symbol: String, type: QuotesService.TrendType): URL? {
        return null
    }
}

data class ClientResponse(val code: Int, val msg: String? = null) {
    fun isSuccess(): Boolean {
        return code == ResultCode.SUCCESS
    }
}

object ResultCode {
    const val SUCCESS = 0
    const val UNKNOWN = -1
    const val NOT_SUPPORT_DATASOURCE = 1
    const val CLIENT_ERROR = 2
    const val SETTING_ERROR = 3
    const val SCRIPT_EMPTY = 4
    const val SCRIPT_FAIL = 5
    const val SCRIPT_NOT_SUPPORT = 6
    const val SYMBOL_EMPTY = 7
}


class ClientException : Exception {
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}
