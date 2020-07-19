package top.shenluw.intellij.stockwatch

import com.intellij.openapi.components.*
import com.intellij.ui.ColorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import org.apache.commons.collections.CollectionUtils
import top.shenluw.intellij.stockwatch.utils.TradingUtil
import java.awt.Color
import java.io.File

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
    var symbols: MutableSet<String> = linkedSetOf()

    /**
     * 老虎股票数据源
     */
    var tigerDataSourceSetting: TigerDataSourceSetting? = null

    /**
     * 老虎股票数据源
     */
    var tigerPollDataSourceSetting: TigerPollDataSourceSetting? = null

    /**
     * 脚本股票数据源
     */
    var scriptPollDataSourceSetting: ScriptPollDataSourceSetting? = null

    /**
     * 当前选择使用的数据源
     */
    var useDataSourceId: String? = ScriptPollDataSourceSetting::class.simpleName

    /**
     * 最后一次选择脚本的目录
     */
    var lastScriptDir = ""

    /**
     * 是否开启脚本的日志输出
     */
    var enableScriptLog = false

    /**
     * 轮询间隔 单位毫秒
     */
    var interval = 10_000L

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

    /**
     *  显示盘前盘后价格
     */
    var preAndAfterTrading: Boolean = true

    /**
     * statusBar显示样式
     */
    var pattern: String = "[\${name} \${price} | \${percentage}%]"

    var symbolNameCache: String? = null

    /**
     * 是否开始走势图
     */
    var enableTrendChart: Boolean = true

    /**
     * 点击statusBar获取趋势周期
     */
    var trendType: QuotesService.TrendType = QuotesService.TrendType.MINUTE

    /**
     * 点击statusBar弹窗宽度
     */
    var trendPopupWidth = 320

    /**
     * 点击statusBar弹窗高度
     */
    var trendPopupHeight = 150


    @Transient
    fun getRealSymbols(): MutableSet<String> {
        return symbols.filter { !TradingUtil.isIgnoreSymbol(it) }
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .filter { !TradingUtil.isIgnoreSymbol(it) }
            .toHashSet()
    }

    override fun getState(): Settings? = this

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

    val id: String
        get() = this.javaClass.simpleName
}

interface ITigerDataSourceSetting : DataSourceSetting {
    val tigerId: String?
    val privateKey: String?
}

@Tag("tiger-data-source-setting")
data class TigerDataSourceSetting(
    @Property
    override val tigerId: String? = null,
    @Property
    override val privateKey: String? = null
) : ITigerDataSourceSetting {

    @Transient
    override fun isValid(): Boolean {
        return !tigerId.isNullOrBlank() && !privateKey.isNullOrBlank()
    }
}


@Tag("tiger-poll-data-source-setting")
data class TigerPollDataSourceSetting(
    @Property
    override val tigerId: String? = null,
    @Property
    override val privateKey: String? = null,
    @Property
    val interval: Long = 10_000L
) : ITigerDataSourceSetting {

    @Transient
    override fun isValid(): Boolean {
        return !tigerId.isNullOrBlank() && !privateKey.isNullOrBlank()
    }
}

@Tag("scrip-data-source-setting")
data class ScriptPollDataSourceSetting(
    @Property
    val interval: Long = 10_000L,
    @XCollection
    val paths: List<String>? = null,
    @XCollection
    val actives: List<String>? = null
) : DataSourceSetting {

    override fun isValid(): Boolean {
        if (!paths.isNullOrEmpty()) {
            for (path in paths) {
                val f = File(path)
                if (!f.exists() || !f.isFile) {
                    return false
                }
            }
        }
        return CollectionUtils.isSubCollection(actives ?: emptyList<String>(), paths)
    }
}