package top.shenluw.intellij.stockwatch

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

/**
 * @param symbol 股票代码
 * @param name 股票名称
 * @author Shenluw
 * created: 2021/8/1 14:46
 */
@Tag("stockSummary")
class StockSummary {
    @Attribute("symbol")
    lateinit var symbol: String

    @Attribute("name")
    lateinit var name: String

    constructor()
    constructor(symbol: String, name: String) {
        this.symbol = symbol
        this.name = name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StockSummary

        if (symbol != other.symbol) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = symbol.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

}