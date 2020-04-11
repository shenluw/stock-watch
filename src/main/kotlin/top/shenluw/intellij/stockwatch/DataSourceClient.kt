package top.shenluw.intellij.stockwatch

import org.jetbrains.concurrency.Promise
import java.util.*

/**
 * @author Shenluw
 * created: 2020/3/29 16:14
 */
interface DataSourceClient<T : DataSourceSetting> {

    fun start(dataSourceSetting: T, symbols: SortedSet<String>)

    fun update(symbols: SortedSet<String>)

    fun close()

    fun testConfig(dataSourceSetting: T): Promise<ClientResponse>

    fun getStockInfo(symbol: String): StockInfo?
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
