package top.shenluw.intellij.stockwatch.dialog

import com.intellij.openapi.project.Project
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import top.shenluw.intellij.notifyMsg
import top.shenluw.intellij.stockwatch.DataSourceClient
import top.shenluw.intellij.stockwatch.DataSourceSetting
import top.shenluw.intellij.stockwatch.QuotesService
import top.shenluw.intellij.stockwatch.StockSummary
import top.shenluw.intellij.stockwatch.ui.HintListener
import top.shenluw.intellij.stockwatch.ui.SearchStockDialogUI
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
class SearchStockDialog(project: Project) : SearchStockDialogUI(project) {

    private val trendGroupMap = hashMapOf<JRadioButton, QuotesService.TrendType>()

    private var dataSourceClient: DataSourceClient<DataSourceSetting>? = null

    init {
        initTrendUI()

        isModal = false
        resultList.cellRenderer = SimpleListCellRenderer.create { label: JBLabel, value: StockSummary?, _: Int ->
            value?.let {
                label.text = "${it.name} (${it.symbol})"
            }
        }
        resultList.addListSelectionListener {
            showPreview()
        }
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                addSettingSymbol()
                return true
            }

        }.installOn(resultList)

        initSearchUI()
    }

    private fun initSearchUI() {
        searchTextField.addFocusListener(HintListener("输入关键字搜索"))
        searchTextField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    if (searchTextField.text.isNotBlank()) {
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
        trendGroupMap.keys.forEach { group.add(it) }
    }

    /**
     * 展示趋势图
     */
    private fun showPreview() {

    }

    /**
     * 添加到配置中
     */
    private fun addSettingSymbol() {
    }


    private fun search(keyword: String) {
        val service = QuotesService.instance
        var client = service.getDataSourceClient()
        if (client == null) {
            val setting = service.getActiveDataSourceSetting()
            if (setting == null) {
                notifyMsg("stock-watch", "数据源未配置")
                return
            }
            client = service.createDataSourceClient(setting)
            if (client == null) {
                notifyMsg("stock-watch", "数据源客户端创建失败")
                return
            }
            dataSourceClient = client
        }
        val result = client.searchStockSummary(keyword)
        if (result.isEmpty()) {
            notifyMsg("stock-watch", "搜索结果为空")
            return
        }
        resultList.setListData(Vector(result))
    }


    override fun dispose() {
        super.dispose()
        dataSourceClient?.close()
    }
}