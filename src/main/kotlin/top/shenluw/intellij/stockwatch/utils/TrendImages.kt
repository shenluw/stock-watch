package top.shenluw.intellij.stockwatch.utils

import com.intellij.openapi.project.Project
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBImageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import top.shenluw.intellij.stockwatch.DataSourceClient
import top.shenluw.intellij.stockwatch.DataSourceSetting
import top.shenluw.intellij.stockwatch.QuotesService
import java.net.URL
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * 趋势图创建
 *
 * @author Shenluw
 * created: 2021/8/7 16:29
 */
object TrendImages {


    suspend fun downloadImage(
        symbol: String,
        type: QuotesService.TrendType,
        client: DataSourceClient<DataSourceSetting>
    ): URL? {
        if (type == QuotesService.TrendType.NONE) {
            return null
        }
        val url: URL = client.getTrendChart(symbol, type) ?: return null
        return Images.downloadImage(url)
    }

    fun createImageView(icon: Icon): JComponent {
        val label = JLabel("", icon, JLabel.CENTER)
        label.setSize(icon.iconWidth, icon.iconHeight)
        return label
    }

    fun createIcon(url: URL, w: Int, h: Int): Icon {
        val imageIcon = ImageIcon(url)
        val sw = w.toDouble() / imageIcon.iconWidth
        val sh = h.toDouble() / imageIcon.iconHeight

        val image = ImageUtil.scaleImage(imageIcon.image, maxOf(sw, sh))
        return JBImageIcon(image)
    }

    inline fun createTrendImage(
        symbol: String,
        type: QuotesService.TrendType,
        w: Int, h: Int,
        project: Project?,
        client: DataSourceClient<DataSourceSetting>? = QuotesService.instance.getDataSourceClient(),
        crossinline block: (Icon) -> Unit
    ) {
        GlobalScope.async {
            if (project == null || project.isDisposed) {
                return@async
            }
            if (client == null) {
                return@async
            }
            val url = downloadImage(symbol, type, client) ?: return@async

            withContext(Dispatchers.Swing) {
                if (project.isDisposed) {
                    return@withContext
                }
                block.invoke(createIcon(url, w, h))
            }
        }
    }


}