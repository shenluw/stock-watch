package top.shenluw.intellij.stockwatch

/**
 * @author Shenluw
 * created: 2020/3/22 21:07
 * @param name 股票名称
 * @param symbol 股票代码： AAPL
 * @param openPrice 开盘价
 * @param price 当前价格
 * @param percentage 涨跌幅
 * @param timestamp 更新时间戳,单位毫秒
 */
data class StockInfo(
        val name: String,
        val symbol: String,
        val openPrice: Double?,
        var price: Double?,
        var percentage: Double?,
        var timestamp: Long?
)