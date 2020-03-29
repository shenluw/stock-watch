package top.shenluw.intellij.stockwatch.utils

import com.intellij.ui.ColorUtil
import java.awt.Color

/**
 * @author Shenluw
 * created: 2020/3/22 21:50
 */
object ColorUtil {

    private val cacheColor = hashMapOf<String, Color>()

    fun getColor(hex: String): Color? {
        var color = cacheColor[hex]
        if (color != null) {
            return color
        }
        color = try {
            ColorUtil.fromHex(hex)
        } catch (e: Exception) {
            null
        }
        if (color != null) {
            cacheColor[hex] = color
            if (cacheColor.size > 120) {
                val iterator = cacheColor.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key != hex) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
        return color
    }
}