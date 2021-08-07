package top.shenluw.intellij.stockwatch.utils

/**
 * 过滤短时间内重复事件
 * @author Shenluw
 * created: 2021/8/7 20:06
 */
class EventFilter(private val interval: Long) {
    var last = 0L

    fun canRun(): Boolean {
        val now = System.currentTimeMillis()
        if (now - last > interval) {
            last = now
            return true
        }
        return false
    }
}