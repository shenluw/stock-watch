package top.shenluw.intellij.stockwatch.client

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.jetbrains.rd.util.concurrentMapOf
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.SymbolNameItem
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteMarketRequest
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteSymbolNameRequest
import com.tigerbrokers.stock.openapi.client.socket.ApiAuthentication
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol
import com.tigerbrokers.stock.openapi.client.struct.enums.Language
import com.tigerbrokers.stock.openapi.client.struct.enums.Market
import com.tigerbrokers.stock.openapi.client.util.ApiLogger
import org.apache.commons.collections.CollectionUtils
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.utils.compress
import top.shenluw.intellij.stockwatch.utils.uncompress
import top.shenluw.plugin.dubbo.utils.KLogger
import java.util.*


/**
 * @author shenlw
 * @date 2020/3/23 18:02
 */
class TigerClientClient : DataSourceClient, ApiComposeCallback, KLogger {

    private companion object {
        @Volatile
        private var symbolNameItems: List<SymbolNameItem>? = null

        @Volatile
        private var lastUpdateTimestamp: Long = 0L

        init {
            ApiLogger.setDebugEnabled(false)
            ApiLogger.setInfoEnabled(false)
            ApiLogger.setErrorEnabled(true)
        }
    }

    @Volatile
    private var socketClient: WebSocketClient? = null

    private var symbols: SortedSet<String>? = null

    private var dataSourceSetting: TigerDataSourceSetting? = null

    private var cache = concurrentMapOf<String, StockInfo>()

    @Synchronized
    override fun start(dataSourceSetting: DataSourceSetting, symbols: SortedSet<String>) {
        if (dataSourceSetting !is TigerDataSourceSetting) {
            throw ClientException("not support data source")
        }
        if (!dataSourceSetting.isValid()) {
            throw ClientException("setting error")
        }

        val origin = this.symbols
        this.symbols = symbols

        this.dataSourceSetting = dataSourceSetting

        updateSymbolNames()

        if (socketClient == null) {
            val auth = ApiAuthentication.build(dataSourceSetting.tigerId, dataSourceSetting.privateKey)
            socketClient = WebSocketClient(WSS_API_ADDRESS, auth, this)
            socketClient?.connect()
            if (!this.symbols.isNullOrEmpty()) {
                subscribeQuote(symbols)
            }
        } else {
            if (socketClient?.isConnected == true) {
                if (!CollectionUtils.isEqualCollection(symbols, origin)) {
                    socketClient?.cancelSubscribeQuote(origin)
                    subscribeQuote(symbols)
                }
            }
        }
    }

    @Synchronized
    override fun close() {
        log.debug("close client")
        socketClient?.disconnect()
        socketClient = null
        cache.clear()
    }

    override fun update(symbols: SortedSet<String>) {
        if (CollectionUtils.isEqualCollection(this.symbols, symbols)) {
            return
        }
        if (!this.symbols.isNullOrEmpty() && socketClient?.isConnected == true) {
            socketClient?.cancelSubscribeQuote(this.symbols)
        }
        this.symbols = symbols
        subscribeQuote(symbols)
    }

    override fun testConfig(dataSourceSetting: DataSourceSetting): Promise<ClientResponse> {
        if (!dataSourceSetting.isValid()) {
            return resolvedPromise(ClientResponse(ResultCode.SETTING_ERROR, "setting error"))
        }
        if (dataSourceSetting !is TigerDataSourceSetting) {
            return resolvedPromise(ClientResponse(ResultCode.NOT_SUPPORT_DATASOURCE, "not support data source"))
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

    private fun subscribeQuote(symbols: SortedSet<String>) {
        if (socketClient?.isConnected == true) {
            val focusKeys = arrayListOf(
                "marketStatus", "preClose", "latestPrice", "symbol",
                "open", "price", "high", "low", "volume", "latestTimestamp"
            )
            socketClient?.subscribeQuote(symbols, focusKeys)
        }
    }

    private fun findSymbolName(symbol: String): SymbolNameItem? {
        if (symbolNameItems != null) {
            return symbolNameItems?.find { it.symbol == symbol }
        }
        return null
    }

    private fun updateSymbolNames(force: Boolean = false) {
        // 超过5分钟允许重新获取接口数据
        if (force && System.currentTimeMillis() - lastUpdateTimestamp < 5 * 60 * 1000) {
            return
        }

        val runnable = {
            val items = fetchSymbolNames()
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

    private fun fetchSymbolNames(): MutableList<SymbolNameItem>? {
        val setting = dataSourceSetting ?: return null
        val client = createApiClient(setting)
        val response = client.execute(QuoteSymbolNameRequest.newRequest(Market.ALL, Language.zh_CN))
        if (response.isSuccess) {
            return response.symbolNameItems
        } else {
            log.error("fetchSymbolNames error:" + response.message)
            return null
        }
    }

    private fun createApiClient(setting: TigerDataSourceSetting): TigerHttpClient {
        return TigerHttpClient(HTTP_API_ADDRESS, setting.tigerId, setting.privateKey)
    }

    override fun orderStatusChange(jsonObject: JSONObject?) {
        log.debug("orderStatusChange: ", jsonObject)
    }

    override fun optionChange(jsonObject: JSONObject?) {
        log.debug("optionChange: ", jsonObject)
    }

    override fun subscribeEnd(jsonObject: JSONObject?) {
        log.debug("subscribeEnd: ", jsonObject)
    }

    override fun connectionClosed() {
        log.debug("connectionClosed")
    }

    override fun hearBeat(heartBeatContent: String?) {
        log.debug("hearBeat: ", heartBeatContent)
    }

    override fun futureChange(jsonObject: JSONObject?) {
        log.debug("futureChange: ", jsonObject)
    }

    override fun cancelSubscribeEnd(jsonObject: JSONObject?) {
        log.debug("cancelSubscribeEnd: ", jsonObject)
    }

    override fun error(errorMsg: String?) {
        log.debug("error: ", errorMsg)
    }

    override fun error(id: Int, errorCode: Int, errorMsg: String?) {
        log.debug("error: ", id, errorCode, errorMsg)
    }

    override fun askBidChange(jsonObject: JSONObject?) {
        log.debug("askBidChange: ", jsonObject)
    }

    override fun quoteChange(jsonObject: JSONObject?) {
        log.debug("quoteChange: ", jsonObject)

        if (jsonObject == null) {
            return
        }

        if (!jsonObject.containsKey("marketStatus")) {
            return
        }
        val symbol = jsonObject.getString("symbol")
        val item = findSymbolName(symbol)

        var name = symbol
        if (item != null) {
            name = item.name
        } else {
            runAsync {
                updateSymbolNames(true)
            }
        }

        val preClose = jsonObject.getDouble("preClose")
        val price = jsonObject.getDouble("latestPrice")
        val info = StockInfo(
            name, symbol,
            jsonObject.getDouble("open")
            , preClose, price,
            jsonObject.getDouble("high"),
            jsonObject.getDouble("low"),
            jsonObject.getLong("volume"),
            jsonObject.getLong("latestTimestamp")
        )

        cache[symbol] = info

        Application.messageBus.syncPublisher(QuotesTopic).quoteChange(info)
    }

    override fun positionChange(jsonObject: JSONObject?) {
        log.debug("positionChange: ", jsonObject)
    }

    override fun serverHeartBeatTimeOut(channelIdAsLongText: String?) {
        log.debug("serverHeartBeatTimeOut: ", channelIdAsLongText)
    }

    override fun assetChange(jsonObject: JSONObject?) {
        log.debug("assetChange: ", jsonObject)
    }

    override fun connectionAck() {
        log.debug("connectionAck")
    }

    override fun connectionAck(serverSendInterval: Int, serverReceiveInterval: Int) {
        log.debug("connectionAck: ", serverSendInterval, serverReceiveInterval)
    }

    override fun getSubscribedSymbolEnd(subscribedSymbol: SubscribedSymbol?) {
        log.debug("getSubscribedSymbolEnd: ", subscribedSymbol)
    }
}