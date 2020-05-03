package top.shenluw.intellij.stockwatch.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * @author Shenluw
 * created: 2020/4/19 19:22
 */
object TradingUtil {
    private val zoneId = ZoneId.of("America/New_York")

    fun isPreTimeRange(timestamp: Long): Boolean {
        if (!isTradingDay()) {
            return false
        }

        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = instant.atZone(zoneId)

        val hour = dateTime.hour
        if (hour in 4..9) {
            if (hour == 9) {
                return dateTime.minute < 30
            }
            return true
        }
        return false

    }

    fun isAfterTimeRange(timestamp: Long): Boolean {
        if (!isTradingDay()) {
            return false
        }
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = instant.atZone(zoneId)

        val hour = dateTime.hour
        return hour in 16..20
    }

    /**
     * 认为周六、周日不是交易日
     */
    fun isTradingDay(): Boolean {
        val localDate = LocalDate.now(zoneId)
        val week = localDate.dayOfWeek
        if (week == DayOfWeek.SUNDAY || week == DayOfWeek.SATURDAY) {
            return false
        }
        return true
    }

    /**
     * 判断是否是夏令时
     * 夏令时为每年3月的第二个星期日至11月的第一个星期日
     */
    fun isDaylightSavings(): Boolean {
        return zoneId.rules.isDaylightSavings(Instant.now())
    }

    /**
     * 以# 开头标识忽略这一行
     */
    fun isIgnoreSymbol(symbol: String): Boolean {
        return symbol.isBlank() || symbol[0] == '#'
    }

}

