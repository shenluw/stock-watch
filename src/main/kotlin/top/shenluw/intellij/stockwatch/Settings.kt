package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.*
import com.intellij.ui.ColorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
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
    var dataSourceSetting: DataSourceSetting? = null

    /**
     * 是否启动行情监控
     */
    var enabled: Boolean = true

    /**
     * 上涨颜色
     */
    var riseColor: String = ColorUtil.toHex(Color.WHITE)

    /**
     * 下跌颜色
     */
    var fallColor: String = ColorUtil.toHex(Color.ORANGE)

    var patternSetting: PatternSetting? = PatternSetting(true, 0, false)

    var symbolNameCache: String? = null

    override fun getState(): Settings? {
        return this
    }

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: Settings
            get() = ServiceManager.getService(Settings::class.java)
    }
}

interface DataSourceSetting {
    fun isValid(): Boolean
}

@Tag("tigerDataSource")
data class TigerDataSourceSetting(val tigerId: String?, val privateKey: String?) : DataSourceSetting, Serializable {
    override fun isValid(): Boolean {
        return !tigerId.isNullOrBlank() && !privateKey.isNullOrBlank()
    }
}

/**
 * @param fullName 是否使用全称显示
 * @param namePrefix 取名称前N个字符显示
 * @param useSymbol 使用股票代码显示
 */
@Tag("patternSetting")
data class PatternSetting(val fullName: Boolean, var namePrefix: Int, var useSymbol: Boolean) : Serializable