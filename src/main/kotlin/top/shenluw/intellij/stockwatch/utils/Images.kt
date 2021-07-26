package top.shenluw.intellij.stockwatch.utils

import com.intellij.openapi.Disposable
import org.apache.commons.io.FileUtils
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

    private var client: CloseableHttpClient? = HttpClients
        .custom()
        .disableAutomaticRetries()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setSocketTimeout(15_000)
                .build()
        )
        .build()

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun downloadImage(url: URL): URL? {
        val client = this.client ?: return null

        val file = try {
            File.createTempFile("stock_", "img")
        } catch (e: Exception) {
            return null
        }
        val path = file.absolutePath
        imgFiles.add(path)

        // 清理
        scheduledExecutorService.schedule({
            imgFiles.remove(path)
            FileUtils.deleteQuietly(File(path))
        }, CACHE_FILE_EXPIRE, TimeUnit.MILLISECONDS)

        val response = try {
            client.execute(HttpGet(url.toURI()))
        } catch (e: Exception) {
            return null
        }
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
            FileUtils.deleteQuietly(File(it))
        }
        imgFiles.clear()
        scheduledExecutorService.shutdownNow()
    }
}
