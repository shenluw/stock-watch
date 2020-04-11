package top.shenluw.intellij.stockwatch.client

import org.apache.commons.lang.math.NumberUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import top.shenluw.intellij.stockwatch.*
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Shenluw
 * created: 2020/4/11 15:54
 */
class SinaClient : AbstractPollClient<SinaPollDataSourceSetting>() {
    private val api_url = "http://hq.sinajs.cn/list="

    private var httpClient: CloseableHttpClient? = null

    private var processedUrl: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    private val dateFormatHK = SimpleDateFormat("hh:mm")
    private val dateFormatA = SimpleDateFormat("hh:mm:ss")

    private fun processUrl(symbols: SortedSet<String>): String {
        if (processedUrl != null) {
            return processedUrl!!
        }
        val p = symbols.map {
            return@map processAStock(it)
        }.joinToString(",")

        processedUrl = api_url + p
        return processedUrl!!
    }

    override fun fetch(): List<StockInfo> {
        return if (this.symbols.isNullOrEmpty()) {
            emptyList()
        } else {
            val symbols = this.symbols
            val response = httpClient?.execute(HttpGet(processUrl(symbols!!)))

            if (response == null) {
                emptyList()
            } else {
                if (response.statusLine.statusCode / 100 == 2) {
                    val resp = EntityUtils.toString(response.entity)
                    parse(resp)
                } else {
                    emptyList()
                }
            }
        }
    }

    override fun start(dataSourceSetting: SinaPollDataSourceSetting, symbols: SortedSet<String>) {
        if (!dataSourceSetting.isValid()) {
            throw ClientException("setting error")
        }

        interval = dataSourceSetting.interval

        this.symbols = symbols
        httpClient = HttpClients.createSystem()
        startPoll()
        HttpClients.createDefault()
    }

    override fun testConfig(dataSourceSetting: SinaPollDataSourceSetting): Promise<ClientResponse> {
        return resolvedPromise(ClientResponse(ResultCode.SUCCESS))
    }

    override fun update(symbols: SortedSet<String>) {
        this.processedUrl = null
        super.update(symbols)
    }

    private fun parse(text: String?): List<StockInfo> {
        if (text.isNullOrBlank()) return emptyList()

        val list = arrayListOf<StockInfo>()

        try {
            BufferedReader(StringReader(text)).use { reader ->
                var line: String
                while (reader.readLine().also { line = it?.trim() ?: "" } != null) {
                    if (!line.isBlank()) {
                        var stock: StockInfo? = null
                        if (line.startsWith("var hq_str_gb")) {
                            stock = parseUs(line)
                        }
                        if (line.startsWith("var hq_str_hk")) {
                            stock = parseHK(line)
                        }
                        if (line.startsWith("var hq_str_sh") || line.startsWith("var hq_str_sz")) {
                            stock = parseA(line)
                        }
                        if (stock != null) {
                            list.add(stock)
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return list
    }

    private fun parseUs(text: String): StockInfo? {
        val sp = text.split("=")
        val symbolInfo = sp[0]
        val t = sp[1]
        if (!t.isNullOrBlank()) {
            val symbol = symbols?.findLast { symbolInfo.endsWith(it) } ?: "--"
            val detail = t.split(',')
            val name = detail[0]

            val preClose = NumberUtils.toDouble(detail[26])

            val openPrice = NumberUtils.toDouble(detail[5])
            val price = NumberUtils.toDouble(detail[1])
            val high = NumberUtils.toDouble(detail[6])
            val low = NumberUtils.toDouble(detail[7])
            val volume = NumberUtils.toLong(detail[10])

            val timestamp = dateFormat.parse(detail[3]).time

            return StockInfo(name, symbol, openPrice, preClose, price, high, low, volume, timestamp)
        }
        return null
    }

    private fun parseHK(text: String): StockInfo? {
        val sp = text.split("=")
        val symbolInfo = sp[0]
        val t = sp[1]
        if (!t.isNullOrBlank()) {
            val symbol = symbols?.findLast { symbolInfo.endsWith(it) } ?: "--"
            val detail = t.split(',')
            val name = detail[1]

            val openPrice = NumberUtils.toDouble(detail[2])
            val preClose = NumberUtils.toDouble(detail[3])
            val high = NumberUtils.toDouble(detail[4])
            val low = NumberUtils.toDouble(detail[5])
            val price = NumberUtils.toDouble(detail[6])
            val volume = NumberUtils.toLong(detail[12])

            val timestamp = dateFormatHK.parse(detail[18]).time

            return StockInfo(name, symbol, openPrice, preClose, price, high, low, volume, timestamp)
        }
        return null
    }

    private fun parseA(text: String): StockInfo? {
        val sp = text.split("=")
        val symbolInfo = sp[0]
        val t = sp[1]
        if (!t.isNullOrBlank()) {
            val symbol = symbols?.findLast { symbolInfo.endsWith(it) } ?: "--"


            val detail = t.split(',')
            val name = detail[0]

            val openPrice = NumberUtils.toDouble(detail[1])
            val preClose = NumberUtils.toDouble(detail[2])
            val price = NumberUtils.toDouble(detail[3])
            val high = NumberUtils.toDouble(detail[4])
            val low = NumberUtils.toDouble(detail[5])
            val volume = NumberUtils.toLong(detail[8])
            val timestamp = dateFormatA.parse(detail[31]).time

            return StockInfo(name, symbol, openPrice, preClose, price, high, low, volume, timestamp)
        }
        return null
    }

    private val shPrefix = arrayOf("60", "51", "68")
    private val szPrefix = arrayOf("30", "00", "15", "399")

    private fun processAStock(symbol: String): String {
        if (symbol.startsWith("hz")) {
            return symbol
        }
        if (symbol.startsWith("gb_")) {
            return symbol
        }
        if (symbol.length == 6) {
            for (s in shPrefix) {
                if (symbol.startsWith(s))
                    return "sh$symbol"
            }
            for (s in szPrefix) {
                if (symbol.startsWith(s))
                    return "sz$symbol"
            }

        }
        return symbol
    }

    override fun close() {
        super.close()
        HttpClientUtils.closeQuietly(httpClient)
        httpClient = null
    }
}