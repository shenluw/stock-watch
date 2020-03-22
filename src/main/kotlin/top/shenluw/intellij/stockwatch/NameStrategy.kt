package top.shenluw.intellij.stockwatch

/**
 * @author Shenluw
 * created: 2020/3/22 21:34
 */
interface NameStrategy {
    fun transform(name: String?): String?
}

class FullNameStrategy : NameStrategy {

    override fun transform(name: String?): String? {
        return name
    }

    companion object {
        val instance: NameStrategy
            get() = FullNameStrategy()
    }
}

class PrefixNameStrategy(val prefix: Int) : NameStrategy {

    override fun transform(name: String?): String? {
        if (prefix > 0 && !name.isNullOrEmpty()) {
            if (name.length >= prefix) {
                return name.substring(0, prefix)
            }
        }
        return name
    }

}