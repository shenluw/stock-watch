package top.shenluw.intellij.stockwatch.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import top.shenluw.intellij.stockwatch.KLogger
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 图片ui辅助类
 * 避免图像缓存
 *
 * @author Shenluw
 * created: 2020/7/19 17:49
 */
object Images : Disposable, KLogger {
    private val CACHE_FILE_EXPIRE = Duration.ofSeconds(30).toMillis()

    private val imgFiles = arrayListOf<String>()

    private var client: CloseableHttpClient? = HttpClients.createMinimal()

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    suspend fun downloadImage(url: URL): URL? {
        val client = this.client ?: return null

        val file = File.createTempFile("stock_", "img")
        val path = file.absolutePath
        imgFiles.add(path)

        // 清理
        scheduledExecutorService.schedule({
            imgFiles.remove(path)
            FileUtil.delete(File(path))
        }, CACHE_FILE_EXPIRE, TimeUnit.MILLISECONDS)

        val httpGet = HttpGet(url.toURI())
        val config = RequestConfig.custom()
            .setConnectTimeout(15_000)
            .setSocketTimeout(15_000)
            .build()

        httpGet.config = config
        val response = client.execute(httpGet)
        if (response.statusLine.statusCode / 100 == 2) {
            try {
                response.entity.writeTo(FileOutputStream(file))
                return file.toURI().toURL()
            } catch (e: Exception) {
                log.debug("download fail", e)
            }
        }
        return null
    }

    override fun dispose() {
        HttpClientUtils.closeQuietly(client)
        client = null

        imgFiles.forEach {
            FileUtil.delete(File(it))
        }
        imgFiles.clear()
        scheduledExecutorService.shutdownNow()
    }
}
