package top.shenluw.intellij.stockwatch.dialog

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.util.xmlb.XmlSerializerUtil
import top.shenluw.intellij.Application
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.*
import top.shenluw.intellij.stockwatch.ui.HintListener
import top.shenluw.intellij.stockwatch.ui.SearchStockDialogUI
import top.shenluw.intellij.stockwatch.utils.EventFilter
import top.shenluw.intellij.stockwatch.utils.TrendImages
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.ButtonGroup
import javax.swing.JRadioButton

/**
 * @author Shenluw
 * created: 2021/8/1 17:00
 */
class SearchStockDialog(private val project: Project) : SearchStockDialogUI(project), KLogger {
    private val NOTIFICATION_TITLE = "stock-watch"

    private val trendGroupMap = hashMapOf<JRadioButton, QuotesService.TrendType>()

    private var dataSourceClient: DataSourceClient<DataSourceSetting>? = null

    private var uiSetting = SearchUISetting.instance

    init {
        initTrendUI()

        isModal = false
        resultList.cellRenderer = SimpleListCellRenderer.create { label: JBLabel, value: StockSummary?, _: Int ->
            value?.let {
                label.text = "${it.name} (${it.symbol})"
            }
        }
        resultList.addListSelectionListener { e ->
            log.debug("search select", resultList.selectedValue)
            if (!e.valueIsAdjusting) {
                showPreview()
            }
        }
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                addSettingSymbol()
                return true
            }

        }.installOn(resultList)

        initSearchUI()

        val x = uiSetting.x
        val y = uiSetting.y
        if (x != null && y != null) {
            setLocation(x, y)
        }
        val w = uiSetting.w
        val h = size?.height
        if (w != null && h != null) {
            setSize(w, h)
        }
    }

    private fun initSearchUI() {
        searchTextField.addFocusListener(HintListener("输入关键字搜索"))
        searchTextField.addKeyListener(object : KeyAdapter() {
            val filter = EventFilter(1000)
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    if (searchTextField.text.isNotBlank() && filter.canRun()) {
                        search(searchTextField.text)
                    }
                }
            }
        })
    }

    private fun initTrendUI() {
        trendGroupMap[trendMinuteRadioButton] = QuotesService.TrendType.MINUTE
        trendGroupMap[trendDailyRadioButton] = QuotesService.TrendType.DAILY
        trendGroupMap[trendWeeklyRadioButton] = QuotesService.TrendType.WEEKLY
        trendGroupMap[trendMonthlyRadioButton] = QuotesService.TrendType.MONTHLY

        val group = ButtonGroup()

        trendGroupMap.forEach { (u, t) ->
            group.add(u)
            u.addItemListener { v ->
                if (v.stateChange == ItemEvent.SELECTED) {
                    showPreview()
                    uiSetting.trendType = t
                }
            }
        }
        val trendType = uiSetting.trendType
        trendGroupMap.forEach { (u, t) ->
            if (t == trendType) {
                u.isSelected = true
            }
        }

    }

    /**
     * 展示趋势图
     */
    private fun showPreview() {
        val value = resultList.selectedValue ?: return
        var type: QuotesService.TrendType = QuotesService.TrendType.MINUTE
        trendGroupMap.forEach { (u, t) -> if (u.isSelected) type = t }
        // NOTE: 2021/8/7 解决框每次变更都变大问题 -24
        TrendImages.createTrendImage(value.symbol, type, contentPanel.width - 24, 0, project, getClient()) {
            previewLabel.icon = it
        }
    }

    /**
     * 添加到配置中
     */
    private fun addSettingSymbol() {
        val summary = resultList.selectedValue ?: return
        Settings.instance.symbols.add(summary)
        QuotesService.instance.updateSubscribe()
    }


    private fun search(keyword: String) {
        val client = getClient() ?: return
        val result = client.searchStockSummary(keyword)
        if (result.isEmpty()) {
            notifyMsg(NOTIFICATION_TITLE, "搜索结果为空")
            return
        }
        resultList.setListData(Vector(result))
    }

    private fun getClient(): DataSourceClient<DataSourceSetting>? {
        if (dataSourceClient != null) {
            return dataSourceClient
        }
        val service = QuotesService.instance
        dataSourceClient = service.getDataSourceClient()
        if (dataSourceClient == null) {
            val setting = service.getActiveDataSourceSetting()
            if (setting == null) {
                notifyMsg(NOTIFICATION_TITLE, "数据源未配置")
                return null
            }
            dataSourceClient = service.createDataSourceClient(setting)
            if (dataSourceClient == null) {
                notifyMsg(NOTIFICATION_TITLE, "数据源客户端创建失败")
                return null
            }
        }
        return dataSourceClient
    }

    override fun dispose() {
        super.dispose()
        val other = QuotesService.instance.getDataSourceClient()
        if (other != dataSourceClient) {
            dataSourceClient?.close()
            dataSourceClient = null
        }
        val uiSetting = SearchUISetting.instance
        uiSetting.x = location.x
        uiSetting.y = location.y
        uiSetting.w = size.width
    }

    @State(name = "StockWatchSearchUISetting", storages = [Storage(StoragePathMacros.CACHE_FILE)])
    class SearchUISetting : PersistentStateComponent<SearchUISetting> {
        var x: Int? = null
        var y: Int? = null
        var w: Int? = null
        var trendType: QuotesService.TrendType = QuotesService.TrendType.MINUTE

        override fun getState(): SearchUISetting = this

        override fun loadState(state: SearchUISetting) {
            XmlSerializerUtil.copyBean(state, this)
        }

        companion object {
            val instance: SearchUISetting
                get() = Application.getService(SearchUISetting::class.java)
        }


    }
}