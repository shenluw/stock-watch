package top.shenluw.intellij.stockwatch.client

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.intellij.notification.NotificationType
import com.jetbrains.rd.util.concurrentMapOf
import com.tigerbrokers.stock.openapi.client.socket.ApiAuthentication
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol
import org.apache.commons.collections.CollectionUtils
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.Application
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.*


/**
 * @author shenlw
 * @date 2020/3/23 18:02
 */
class TigerClient : ITigerClient<TigerDataSourceSetting>, ApiComposeCallback, KLogger {

    @Volatile
    private var socketClient: WebSocketClient? = null

    private var symbols: MutableSet<String>? = null

    private var dataSourceSetting: TigerDataSourceSetting? = null

    private var cache = concurrentMapOf<String, StockInfo>()

    @Synchronized
    override fun create(dataSourceSetting: TigerDataSourceSetting) {
        if (!dataSourceSetting.isValid()) {
            throw ClientException("setting error")
        }
        this.dataSourceSetting = dataSourceSetting
    }

    @Synchronized
    override fun start(symbols: MutableSet<String>) {
        val dataSourceSetting = this.dataSourceSetting ?: throw ClientException("not created")
        val origin = this.symbols
        this.symbols = symbols

        updateSymbolNames(dataSourceSetting)

        if (socketClient == null) {
            val auth = ApiAuthentication.build(dataSourceSetting.tigerId, dataSourceSetting.privateKey)
            socketClient = WebSocketClient(TIGER_WSS_API_ADDRESS, auth, this)
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
        socketClient?.cancelSubscribeQuote(this.symbols)
        socketClient?.disconnect()
        socketClient = null
        this.symbols = null
        cache.clear()
    }

    override fun update(symbols: MutableSet<String>) {
        if (CollectionUtils.isEqualCollection(this.symbols, symbols)) {
            return
        }
        if (!this.symbols.isNullOrEmpty() && socketClient?.isConnected == true) {
            socketClient?.cancelSubscribeQuote(this.symbols)
        }
        this.symbols = symbols
        subscribeQuote(symbols)
    }

    override fun getStockInfo(symbol: String): StockInfo? {
        return cache.getOrDefault(symbol, null)
    }

    private fun subscribeQuote(symbols: MutableSet<String>) {
        if (socketClient?.isConnected == true) {
            val focusKeys = arrayListOf(
                "marketStatus", "preClose", "latestPrice", "symbol",
                "open", "price", "high", "low", "volume", "latestTimestamp"
            )
            socketClient?.subscribeQuote(symbols, focusKeys)
        }
    }

    override fun orderStatusChange(jsonObject: JSONObject?) {
        log.debug("orderStatusChange: ", jsonObject)
    }

    override fun optionChange(jsonObject: JSONObject?) {
        log.debug("optionChange: ", jsonObject)
    }

    override fun subscribeEnd(jsonObject: JSONObject?) {
        log.debug("subscribeEnd: ", jsonObject)

        val msg = jsonObject?.getString("message")
        if (!msg.isNullOrBlank()) {
            notifyMsg("subscribe result", msg, NotificationType.WARNING)
        }
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
                dataSourceSetting?.let { updateSymbolNames(it, true) }
            }
        }

        val preClose = jsonObject.getDouble("preClose")
        val price = jsonObject.getDouble("latestPrice")
        val info = StockInfo(
            name, symbol,
            jsonObject.getDouble("open"), preClose, price,
            jsonObject.getDouble("high"),
            jsonObject.getDouble("low"),
            jsonObject.getLong("volume"),
            null, null,
            jsonObject.getLong("latestTimestamp")
        )

        cache[symbol] = info
        val application = Application
        if (!application.isDisposed) {
            application.messageBus.syncPublisher(QuotesTopic).quoteChange(info)
        }
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
        if (log.isDebugEnabled) {
            log.debug("getSubscribedSymbolEnd: ", JSON.toJSON(subscribedSymbol))
        }
    }

    override fun depthQuoteChange(jsonObject: JSONObject?) {
        if (log.isDebugEnabled) {
            log.debug("getSubscribedSymbolEnd: ", JSON.toJSON(jsonObject))
        }
    }
}