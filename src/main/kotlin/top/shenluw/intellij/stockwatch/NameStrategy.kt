package top.shenluw.intellij.stockwatch

/**
 * @author Shenluw
 * created: 2020/3/22 21:34
 */
interface NameStrategy {
    fun transform(info: StockInfo?): String?
}

class FullNameStrategy(private val useSymbol: Boolean) : NameStrategy {

    override fun transform(info: StockInfo?): String? {
        if (useSymbol) {
            return info?.symbol
        }
        return info?.name
    }

}

class PrefixNameStrategy(private val prefix: Int) : NameStrategy {

    override fun transform(info: StockInfo?): String? {
        if (prefix == 0) {
            return ""
        }
        val name = info?.name
        if (prefix > 0 && !name.isNullOrEmpty()) {
            if (name.length >= prefix) {
                return name.substring(0, prefix)
            }
        }
        return name
    }

}