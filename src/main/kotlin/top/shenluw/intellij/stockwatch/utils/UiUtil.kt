package top.shenluw.intellij.stockwatch.utils

import com.intellij.util.castSafelyTo
import javax.swing.JList

/**
 * @author Shenluw
 * created: 2020/5/3 22:35
 */
object UiUtil {

    inline fun <reified E> JList<E>.getItems(): MutableList<E> {
        val items = arrayListOf<E>()
        for (i in 0 until model.size) {
            model.getElementAt(i).castSafelyTo<E>()?.run {
                items.add(this)
            }
        }
        return items
    }
}
