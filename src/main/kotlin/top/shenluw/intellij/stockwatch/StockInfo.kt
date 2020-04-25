package top.shenluw.intellij.stockwatch

/**
 * @author Shenluw
 * created: 2020/3/22 21:07
 * @param name 股票名称
 * @param symbol 股票代码： AAPL
 * @param openPrice 开盘价
 * @param preClose 上一日收盘价
 * @param price 当前价格
 * @param high 最高价
 * @param low 最低价
 * @param volume 成交量
 * @param prePrice 盘前价
 * @param afterPrice 盘后价
 * @param timestamp 更新时间戳,单位毫秒
 */
data class StockInfo(
    val name: String,
    val symbol: String,
    var openPrice: Double?,
    var preClose: Double?,
    var price: Double?,
    var high: Double?,
    var low: Double?,
    var volume: Long?,
    var prePrice: Double? = null,
    var afterPrice: Double? = null,
    var timestamp: Long?
) {

    /* 涨跌幅 */
    val percentage: Double?
        get() {
            val base = preClose
            val p = price
            if (p != null && base != null) {
                return (p - base) / base
            }
            return null
        }

    /* 盘前涨跌幅 */
    val prePercentage: Double?
        get() {
            val base = price
            val price = prePrice
            if (price != null && base != null) {
                return (price - base) / base
            }
            return percentage
        }

    /* 盘后涨跌幅 */
    val afterPercentage: Double?
        get() {
            val base = price
            val price = afterPrice
            if (price != null && base != null) {
                return (price - base) / base
            }
            return percentage
        }

}