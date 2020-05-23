package top.shenluw.intellij.stockwatch.client

import com.alibaba.fastjson.JSON
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.SymbolNameItem
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteMarketRequest
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteSymbolNameRequest
import com.tigerbrokers.stock.openapi.client.struct.enums.Language
import com.tigerbrokers.stock.openapi.client.struct.enums.Market
import com.tigerbrokers.stock.openapi.client.util.ApiLogger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.utils.compress
import top.shenluw.intellij.stockwatch.utils.uncompress
import top.shenluw.plugin.dubbo.utils.KLogger
import java.security.Security
import java.util.*

/**
 * @author Shenluw
 * created: 2020/4/11 14:59
 */
interface ITigerClient<T : ITigerDataSourceSetting> : DataSourceClient<T>, KLogger {
    private companion object {
        @Volatile
        private var symbolNameItems: List<SymbolNameItem>? = null

        @Volatile
        private var lastUpdateTimestamp: Long = 0L

        init {
            Security.addProvider(BouncyCastleProvider())

            ApiLogger.setDebugEnabled(false)
            ApiLogger.setInfoEnabled(false)
            ApiLogger.setErrorEnabled(true)
        }
    }

    override fun testConfig(dataSourceSetting: T, symbols: SortedSet<String>): Promise<ClientResponse> {
        if (!dataSourceSetting.isValid()) {
            return resolvedPromise(ClientResponse(ResultCode.SETTING_ERROR, "setting error"))
        }

        return runAsync {
            val client = createApiClient(dataSourceSetting)
            return@runAsync try {
                val response = client.execute(QuoteMarketRequest.newRequest(Market.US))
                if (response.isSuccess) {
                    ClientResponse(ResultCode.SUCCESS)
                } else {
                    ClientResponse(ResultCode.CLIENT_ERROR, response.message)
                }
            } catch (e: Exception) {
                ClientResponse(ResultCode.UNKNOWN, e.message)
            }
        }
    }

    fun createApiClient(setting: ITigerDataSourceSetting): TigerHttpClient {
        return TigerHttpClient(HTTP_API_ADDRESS, setting.tigerId, setting.privateKey)
    }

    fun findSymbolName(symbol: String): SymbolNameItem? {
        if (symbolNameItems != null) {
            return symbolNameItems?.find { it.symbol == symbol }
        }
        return null
    }

    fun updateSymbolNames(setting: ITigerDataSourceSetting, force: Boolean = false) {
        // 超过5分钟允许重新获取接口数据
        if (force && System.currentTimeMillis() - lastUpdateTimestamp < 5 * 60 * 1000) {
            return
        }

        val runnable = {
            val items = fetchSymbolNames(setting)
            if (items != null) {
                Settings.instance.symbolNameCache = compress(JSON.toJSONBytes(items))
            }
            symbolNameItems = items
        }

        if (force) {
            runnable.invoke()
            return
        }

        if (symbolNameItems == null) {
            val cache = Settings.instance.symbolNameCache
            if (!cache.isNullOrBlank()) {
                try {
                    symbolNameItems = JSON.parseArray(uncompress(cache), SymbolNameItem::class.java)
                } catch (e: Exception) {
                }
            }
            if (symbolNameItems == null) {
                runnable.invoke()
            }
        }
    }

    private fun fetchSymbolNames(setting: ITigerDataSourceSetting): MutableList<SymbolNameItem>? {
        val client = createApiClient(setting)
        val response = client.execute(QuoteSymbolNameRequest.newRequest(Market.ALL, Language.zh_CN))
        return if (response.isSuccess) {
            response.symbolNameItems
        } else {
            log.error("fetchSymbolNames error:" + response.message)
            null
        }
    }


}