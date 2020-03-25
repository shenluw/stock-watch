package top.shenluw.intellij.stockwatch

import com.alibaba.fastjson.JSONObject
import com.tigerbrokers.stock.openapi.client.socket.ApiAuthentication
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol
import org.apache.commons.collections.CollectionUtils
import top.shenluw.intellij.Application
import top.shenluw.plugin.dubbo.utils.KLogger
import java.util.*

/**
 * @author shenlw
 * @date 2020/3/23 18:02
 */
class TigerClientService : ApiComposeCallback, KLogger {

    @Volatile
    private var socketClient: WebSocketClient? = null

    private var symbols: SortedSet<String>? = null

    @Synchronized
    fun start(dataSourceSetting: TigerDataSourceSetting, symbols: SortedSet<String>) {
        val origin = this.symbols
        this.symbols = symbols

        if (socketClient == null) {
            val auth = ApiAuthentication.build(dataSourceSetting.tigerId, dataSourceSetting.privateKey)
            socketClient = WebSocketClient(API_ADDRESS, auth, this)
            socketClient?.connect()
            if (!this.symbols.isNullOrEmpty()) {
                this.socketClient?.subscribeQuote(symbols)
            }
        } else {
            if (socketClient?.isConnected == true) {
                if (!CollectionUtils.isEqualCollection(symbols, origin)) {
                    socketClient?.cancelSubscribeQuote(origin)
                    socketClient?.subscribeQuote(symbols)
                }
            }
        }
    }

    @Synchronized
    fun close() {
        socketClient?.disconnect()
        socketClient = null
    }

    fun update(symbols: SortedSet<String>) {
        if (!this.symbols.isNullOrEmpty() && socketClient?.isConnected == true) {
            socketClient?.cancelSubscribeQuote(this.symbols)
        }
        this.symbols = symbols
        if (socketClient?.isConnected == true) {
            socketClient?.subscribeQuote(symbols)
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

        val preClose = jsonObject.getDouble("preClose")
        val price = jsonObject.getDouble("latestPrice")
        val info = StockInfo(jsonObject.getString("symbol"),
                jsonObject.getString("symbol"),
                preClose, price,
                (price - preClose) / preClose,
                jsonObject.getLong("latestTimestamp"))

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