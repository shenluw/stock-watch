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
    var timestamp: Long?
) {

    /* 涨跌幅 */
    val percentage: Double?
        get() {
            if (price != null && preClose != null) {
                return (price!! - preClose!!) / preClose!!
            }
            return null
        }

}