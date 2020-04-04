package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.*
import com.intellij.ui.ColorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import java.awt.Color
import java.io.Serializable
import java.util.*

/**
 * @author Shenluw
 * created: 2020/3/21 21:24
 */
@State(name = "StockWatchSetting", storages = [Storage(StoragePathMacros.CACHE_FILE)])
class Settings : PersistentStateComponent<Settings> {

    /**
     * 股票代码
     */
    @XCollection
    var symbols: SortedSet<String> = sortedSetOf()

    /**
     * 股票数据源
     */
    var tigerDataSourceSetting: TigerDataSourceSetting? = null

    /**
     * 是否启动行情监控
     */
    var enabled: Boolean = true

    /**
     * 使用快捷键关闭时是否只隐藏UI显示
     */
    var onlyCloseUI: Boolean = true

    /**
     * 上涨颜色
     */
    var riseColor: String = ColorUtil.toHex(Color.WHITE)

    /**
     * 下跌颜色
     */
    var fallColor: String = ColorUtil.toHex(Color.ORANGE)

    var patternSetting: PatternSetting = PatternSetting()

    var symbolNameCache: String? = null

    @Transient
    fun getRealSymbols(): SortedSet<String> {
        return symbols.filter { !isIgnoreSymbol(it) }.toSortedSet()
    }

    override fun getState(): Settings? = this

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /**
     * 以# 开头标识忽略这一行
     */
    private fun isIgnoreSymbol(symbol: String): Boolean {
        return symbol.isBlank() || symbol[0] == '#'
    }

    companion object {
        val instance: Settings
            get() = ServiceManager.getService(Settings::class.java)
    }
}

interface DataSourceSetting {
    fun isValid(): Boolean
}

@Tag("tiger-data-source-setting")
data class TigerDataSourceSetting(
    @Property
    val tigerId: String? = null,
    @Property
    val privateKey: String? = null
) : DataSourceSetting,
    Serializable {

    @Transient
    override fun isValid(): Boolean {
        return !tigerId.isNullOrBlank() && !privateKey.isNullOrBlank()
    }
}

/**
 * @param fullName 是否使用全称显示
 * @param namePrefix 取名称前N个字符显示
 * @param useSymbol 使用股票代码显示
 */
@Tag("pattern-setting")
data class PatternSetting(
    @Property
    val fullName: Boolean = true,
    @Property
    var namePrefix: Int = 2,
    @Property
    var useSymbol: Boolean = false
) :
    Serializable