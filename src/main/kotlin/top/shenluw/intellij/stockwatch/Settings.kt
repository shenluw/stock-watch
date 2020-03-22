package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.*
import com.intellij.ui.ColorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import java.awt.Color
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
    var symbols = TreeSet<String>()

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
    var fallColor: String = ColorUtil.toHex(Color.GREEN)

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

interface DataSourceSetting

@Tag("tigerDataSource")
data class TigerDataSourceSetting(val tigerId: String, val privateKey: String) : DataSourceSetting