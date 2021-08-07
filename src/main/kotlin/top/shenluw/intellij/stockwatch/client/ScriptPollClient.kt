package top.shenluw.intellij.stockwatch.client

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.intellij.util.castSafelyTo
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.utils.FileLogger
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * @author Shenluw
 * created: 2020/4/19 20:52
 */
class ScriptPollClient : AbstractPollClient<ScriptPollDataSourceSetting>(), KLogger {
    /*********** 脚本中需要定义的方法  ************/
    private val URLFunction = "processUrl"
    private val ParseFunction = "parse"
    private val ResetFunction = "reset"
    private val TrendChartFunction = "trendChart"
    private val SearchUrlFunction = "searchUrl"
    private val SearchParseFunction = "searchParse"

    /********************************************/

    private val HttpMethod = "httpMethod"

    private var httpClient: CloseableHttpClient? = null

    private val scriptEngineManager = ScriptEngineManager()

    private var scriptEngines = hashMapOf<String, ScriptEngine>()

    private fun processUrl(engine: ScriptEngine, symbols: List<String>): String? {
        val any = engine.castSafelyTo<Invocable>()
            ?.invokeFunction(URLFunction, symbols)
        return any?.toString()
    }

    private fun parseStockInfo(text: String, engine: ScriptEngine): List<StockInfo> {
        if (engine is Invocable) {
            val ret = engine.invokeFunction(ParseFunction, text, symbols)
            if (ret is String) {
                return JSON.parseArray(ret)
                    .map {
                        return@map it.castSafelyTo<JSONObject>()
                            ?.let { obj ->
                                StockInfo(
                                    obj.getString("name"),
                                    obj.getString("symbol"),
                                    obj.getDouble("openPrice"),
                                    obj.getDouble("preClose"),
                                    obj.getDouble("price"),
                                    obj.getDouble("high"),
                                    obj.getDouble("low"),
                                    obj.getLong("volume"),
                                    obj.getDouble("prePrice"),
                                    obj.getDouble("afterPrice"),
                                    obj.getLong("timestamp")
                                )
                            }
                    }.filterNotNull()
            }
        }
        return emptyList()
    }

    override fun fetch(): List<StockInfo> {
        return if (this.symbols.isNullOrEmpty()) {
            emptyList()
        } else {
            val symbols = this.symbols!!
            val ret = arrayListOf<StockInfo>()

            scriptEngines.forEach { (path, engine) ->
                try {
                    ret.addAll(fetchOne(engine, httpClient!!, symbols))
                } catch (e: Exception) {
                    log.warn("fetch error $path", e)
                }
            }
            ret
        }
    }

    private fun fetchOne(
        engine: ScriptEngine,
        httpClient: CloseableHttpClient,
        symbols: List<String>
    ): List<StockInfo> {
        val processUrl = processUrl(engine, symbols) ?: throw ClientException("Script fail")
        val tmp = engine.get(HttpMethod)
        var method = HttpGet.METHOD_NAME
        if (tmp != null && tmp is String) {
            method = tmp.toString()
        }
        val request: HttpRequestBase = when (method) {
            HttpGet.METHOD_NAME -> {
                HttpGet(processUrl)
            }
            HttpPost.METHOD_NAME -> {
                HttpPost(processUrl)
            }
            HttpPut.METHOD_NAME -> {
                HttpPut(processUrl)
            }
            else -> {
                throw ClientException("HttpMethod not support $method")
            }
        }

        val response = httpClient.execute(request)
        if (response != null && response.statusLine.statusCode / 100 == 2) {
            val resp = EntityUtils.toString(response.entity)
            return parseStockInfo(resp, engine)
        }
        return emptyList()
    }

    @Synchronized
    override fun create(dataSourceSetting: ScriptPollDataSourceSetting) {
        if (!dataSourceSetting.isValid()) {
            throw ClientException("setting error")
        }
        if (httpClient != null) {
            return
        }

        interval = dataSourceSetting.interval

        scriptEngines.clear()

        val actives = dataSourceSetting.actives
        if (actives.isNullOrEmpty()) {
            return
        }

        dataSourceSetting.actives.forEach {
            val engine = createScriptEngine(File(it))
            if (engine != null) {
                scriptEngines[it] = engine
            }
        }

        httpClient = HttpClients.createSystem()
    }

    override fun start(symbols: MutableSet<String>) {
        if (httpClient == null) {
            throw ClientException("not created")
        }
        this.symbols = symbols.toList()
        startPoll()
    }

    override fun testConfig(
        dataSourceSetting: ScriptPollDataSourceSetting, symbols: Set<String>
    ): Promise<ClientResponse> {
        val actives = dataSourceSetting.actives
        if (actives.isNullOrEmpty()) {
            return resolvedPromise(ClientResponse(ResultCode.SCRIPT_EMPTY, "active script is empty"))
        }

        dataSourceSetting.actives.forEach {
            if (symbols.isEmpty()) {
                return resolvedPromise(ClientResponse(ResultCode.SYMBOL_EMPTY, "symbol is empty"))
            }

            val engine = createScriptEngine(File(it))
                ?: return resolvedPromise(ClientResponse(ResultCode.SCRIPT_NOT_SUPPORT, "script not support $it"))

            val httpClient = HttpClients.createSystem()

            try {
                if (fetchOne(engine, httpClient, symbols.toList()).isEmpty()) {
                    return resolvedPromise(ClientResponse(ResultCode.SCRIPT_FAIL, "script return empty $it. "))
                }
            } catch (e: Exception) {
                log.error("script fail", e)
                return resolvedPromise(ClientResponse(ResultCode.SCRIPT_FAIL, "script $it. ${e.message}"))
            } finally {
                HttpClientUtils.closeQuietly(httpClient)
                engine.get("console").castSafelyTo<FileLogger>()
                    ?.close()
            }
        }

        return resolvedPromise(ClientResponse(ResultCode.SUCCESS))
    }

    private fun createScriptEngine(script: File): ScriptEngine? {
        val logger =
            FileLogger(script.parentFile.absolutePath + File.separator + script.nameWithoutExtension + ".log", log)
        logger.writeable = Settings.instance.enableScriptLog
        val engine = scriptEngineManager.getEngineByExtension(script.extension)
        engine?.put("console", logger)

        val text = script.readText(StandardCharsets.UTF_8)
        engine.eval(text)
        return engine
    }

    override fun update(symbols: MutableSet<String>) {
        scriptEngines.forEach { (_, u) ->
            u.castSafelyTo<Invocable>()?.invokeFunction(ResetFunction)
            u.get("console").castSafelyTo<FileLogger>()
                ?.writeable = Settings.instance.enableScriptLog
        }
        super.update(symbols)
    }

    override fun searchStockSummary(keyword: String): List<StockSummary> {
        val total = mutableListOf<StockSummary>()
        scriptEngines.forEach {
            val engine = it.value

            try {
                val url = engine.castSafelyTo<Invocable>()?.invokeFunction(SearchUrlFunction, keyword)?.toString()
                    ?: throw ClientException("Script fail")
                httpClient?.let { client ->
                    val response = client.execute(HttpGet(url))
                    if (response != null && response.statusLine.statusCode / 100 == 2) {
                        val resp = EntityUtils.toString(response.entity)
                        val summaries = parseSummary(resp, engine)
                        if (summaries.isNotEmpty()) {
                            total.addAll(summaries)
                        }
                    }
                }
            } catch (e: Exception) {
                log.warn("search stock error. ${it.key}", e)
            }
        }
        return total
    }

    private fun parseSummary(resp: String, engine: ScriptEngine): List<StockSummary> {
        val ret = engine.castSafelyTo<Invocable>()
            ?.invokeFunction(SearchParseFunction, resp)
        if (ret is String) {
            return JSON.parseArray(ret)
                .map {
                    return@map it.castSafelyTo<JSONObject>()
                        ?.let { obj ->
                            StockSummary(
                                obj.getString("symbol"),
                                obj.getString("name")
                            )
                        }
                }.filterNotNull()
        }
        return emptyList()
    }

    override fun getTrendChart(symbol: String, type: QuotesService.TrendType): URL? {
        for (entry in scriptEngines) {
            val engine = entry.value
            try {
                val any = engine.castSafelyTo<Invocable>()
                    ?.invokeFunction(TrendChartFunction, symbol, type)

                if (any != null) {
                    return URL(any.toString())
                }

            } catch (e: Exception) {
                log.warn("get trend chart error $entry.key", e)
            }
        }
        return null
    }

    override fun close() {
        super.close()
        HttpClientUtils.closeQuietly(httpClient)
        httpClient = null
        try {
            scriptEngines.forEach { (_, u) ->
                u.get("console").castSafelyTo<FileLogger>()
                    ?.close()
            }
        } finally {
            scriptEngines.clear()
        }
    }
}