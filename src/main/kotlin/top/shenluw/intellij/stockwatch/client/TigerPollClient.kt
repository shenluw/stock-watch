package top.shenluw.intellij.stockwatch.client

import com.intellij.notification.NotificationType
import com.tigerbrokers.stock.openapi.client.https.client.TigerClient
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteRealTimeQuoteRequest
import com.tigerbrokers.stock.openapi.client.struct.enums.TimeZoneId
import com.tigerbrokers.stock.openapi.client.util.DateUtils
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.ClientException
import top.shenluw.intellij.stockwatch.StockInfo
import top.shenluw.intellij.stockwatch.TigerPollDataSourceSetting
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * @author Shenluw
 * created: 2020/4/11 14:52
 */
class TigerPollClient : AbstractPollClient<TigerPollDataSourceSetting>(), ITigerClient<TigerPollDataSourceSetting> {

    private var tigerApiClient: TigerClient? = null

    private var request: QuoteRealTimeQuoteRequest? = null

    override fun fetch(): List<StockInfo> {
        val client = tigerApiClient ?: return emptyList()

        val symbols = this.symbols ?: return emptyList()

        if (this.request == null) {
            this.request = QuoteRealTimeQuoteRequest.newRequest(symbols.toList())
        } else {
            val dtf = DateUtils.DATETIME_FORMAT
            this.request?.timestamp = dtf.format(LocalDateTime.now(ZoneId.of(TimeZoneId.Shanghai.zoneId)))
        }
        val response = client.execute(request)

        if (response.isSuccess) {
            return response.realTimeQuoteItems?.map {
                val symbolName = findSymbolName(it.symbol)
                val info = StockInfo(
                    symbolName?.name ?: it.symbol,
                    it.symbol,
                    it.open,
                    it.preClose,
                    it.latestPrice,
                    it.high,
                    it.low,
                    it.volume,
                    null, null,
                    it.latestTime
                )
                info
            }?.toList().orEmpty()
        } else {
            notifyMsg("tiger poll error", response.message, NotificationType.WARNING)
            return emptyList()
        }
    }

    override fun start(dataSourceSetting: TigerPollDataSourceSetting, symbols: MutableSet<String>) {
        if (!dataSourceSetting.isValid()) {
            throw ClientException("setting error")
        }

        interval = dataSourceSetting.interval

        this.symbols = symbols
        updateSymbolNames(dataSourceSetting)

        tigerApiClient = createApiClient(dataSourceSetting)

        startPoll()
    }

    override fun update(symbols: MutableSet<String>) {
        this.request = null
        super.update(symbols)
    }
}