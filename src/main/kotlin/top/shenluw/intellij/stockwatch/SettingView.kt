package top.shenluw.intellij.stockwatch

import com.intellij.openapi.options.ConfigurableUi
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.ClickListener
import com.intellij.ui.ColorPicker
import com.intellij.ui.ColorUtil
import com.intellij.ui.picker.ColorListener
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.ui.UIUtil
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.jetbrains.concurrency.runAsync
import top.shenluw.intellij.Application
import top.shenluw.intellij.stockwatch.client.TIGER_HELP_LINK
import top.shenluw.intellij.stockwatch.ui.SettingUI
import top.shenluw.intellij.stockwatch.utils.ColorUtil.getColor
import top.shenluw.intellij.stockwatch.utils.TradingUtil
import top.shenluw.intellij.stockwatch.utils.UiUtil.getItems
import java.awt.Color
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

/**
 * @author Shenluw
 * created: 2020/3/29 17:03
 */
class SettingView : SettingUI(), ConfigurableUi<Settings>, KLogger {
    /**
     *  枚举和ui映射关系
     */
    private val trendGroupMap = hashMapOf<QuotesService.TrendType, JRadioButton>()

    init {
        helpLinkLabel.setHyperlinkTarget(TIGER_HELP_LINK)
        helpLinkLabel.setHyperlinkText("帮助")

        val settings = Settings.instance
        val riseColor = settings.riseColor
        val fallColor = settings.fallColor
        setColorButton(fallColorBtn, fallColor)
        setColorButton(riseColorBtn, riseColor)
        ColorPickerHandler(fallColorBtn)
        ColorPickerHandler(riseColorBtn)


        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                handler()
                return true
            }

            private fun handler() {
                val setting = createDataSourceSetting()
                val builder = DialogBuilder(root)
                builder.addOkAction()
                if (!setting.isValid()) {
                    builder.title("error")
                        .centerPanel(JLabel("Setting error"))
                    builder.show()
                    return
                }

                val client = QuotesService.instance.createDataSourceClient(setting)
                if (client == null) {
                    builder.title("error")
                        .centerPanel(JLabel("not support client setting"))
                    builder.show()
                    return
                }

                testConnectBtn.isEnabled = false

                client.testConfig(setting, TradingUtil.filterSymbols(transform(symbolTextArea.text)))
                    .onSuccess {
                        UIUtil.invokeAndWaitIfNeeded(Runnable {
                            testConnectBtn.isEnabled = true
                            if (it.isSuccess()) {
                                builder.setTitle("OK")
                            } else {
                                builder
                                    .title("error")
                                    .centerPanel(JLabel(it.msg))
                            }
                            builder.show()
                        })
                    }.onError {
                        UIUtil.invokeAndWaitIfNeeded(Runnable {
                            testConnectBtn.isEnabled = true
                            builder.title("error")
                                .centerPanel(JLabel(it.message))
                            builder.show()
                        })
                    }
            }
        }.installOn(testConnectBtn)

        initPoll()

        val group = ButtonGroup()
        group.add(tigerRadioButton)
        group.add(scriptRadioButton)

        initScriptPane()

        initTrendChart()
    }

    private val scriptFileScript =
        FileNameExtensionFilter("script", "js", "ts", "groovy", "kt", "go", "py")

    private fun initScriptPane() {
        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                val chooser = JFileChooser(Settings.instance.lastScriptDir)

                chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                chooser.isMultiSelectionEnabled = true
                chooser.fileFilter = scriptFileScript
                chooser.showOpenDialog(root)

                val selectedFile = chooser.selectedFile
                if (selectedFile != null) {
                    Settings.instance.lastScriptDir = selectedFile.parentFile.absolutePath
                    val files = chooser.selectedFiles
                    val items = scriptList.getItems()
                    items.addAll(files.map { it.absolutePath })
                    scriptList.setListData(items.distinct().toTypedArray())
                }
                return true
            }
        }.installOn(addScriptButton)

        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                val selects = scriptList.selectedValuesList

                val model = scriptList.model
                val update = Vector<String>()
                for (i in model.size - 1 downTo 0) {
                    val item = model.getElementAt(i)
                    if (item !in selects) {
                        update.add(item.toString())
                    }
                }
                scriptList.setListData(update)
                return true
            }
        }.installOn(removeScriptButton)
    }

    private fun initPoll() {
        pollCheckBox.addItemListener {
            pollIntervalTextField.isEnabled = it.stateChange == ItemEvent.SELECTED
        }
        pollIntervalTextField.formatterFactory = DefaultFormatterFactory(NumberFormatter())
    }

    private fun initTrendChart() {
        trendGroupMap[QuotesService.TrendType.NONE] = trendNoneRadioButton
        trendGroupMap[QuotesService.TrendType.MINUTE] = trendMinuteRadioButton
        trendGroupMap[QuotesService.TrendType.DAILY] = trendDailyRadioButton
        trendGroupMap[QuotesService.TrendType.WEEKLY] = trendWeeklyRadioButton
        trendGroupMap[QuotesService.TrendType.MONTHLY] = trendMonthlyRadioButton

        val group = ButtonGroup()
        trendGroupMap.values.forEach { group.add(it) }
        setColorButton(trendPopupBackground, Settings.instance.trendPopupBackground)
        ColorPickerHandler(trendPopupBackground)
    }

    override fun reset(settings: Settings) {
        log.debug("reset")

        toggleCheckBox.isSelected = settings.enabled
        val sourceSetting = settings.tigerDataSourceSetting
        tigerIdTextField.text = sourceSetting?.tigerId
        privateKeyTextArea.text = sourceSetting?.privateKey
        val sb = StringBuilder()
        val symbols: Set<StockSummary> = settings.symbols
        if (!symbols.isNullOrEmpty()) {
            val iterator: Iterator<StockSummary> = symbols.iterator()
            while (iterator.hasNext()) {
                val summary = iterator.next()
                sb.append(summary.symbol)
                if (summary.name.isNotBlank()) {
                    sb.append('@').append(summary.name)
                }
                if (iterator.hasNext()) {
                    sb.append("\n")
                }
            }
        }
        symbolTextArea.text = sb.toString()

        scriptLogCheckBox.isSelected = settings.enableScriptLog

        displayFormatTextField.text = settings.pattern

        setColorButton(fallColorBtn, settings.fallColor)
        setColorButton(riseColorBtn, settings.riseColor)

        val useDataSourceId = settings.useDataSourceId
        pollCheckBox.isSelected = isPollDataSourceId(useDataSourceId)

        pollIntervalTextField.value = settings.interval

        if (useDataSourceId == ScriptPollDataSourceSetting::class.simpleName) {
            scriptRadioButton.isSelected = true
        } else {
            tigerRadioButton.isSelected = true
        }

        preAndAfterTradingCheckBox.isSelected = settings.preAndAfterTrading

        settings.scriptPollDataSourceSetting?.paths?.apply {
            scriptList.setListData(this.toTypedArray())
        }

        /* 趋势图 */
        trendGroupMap[settings.trendType]?.isSelected = true

        trendWidth.text = settings.trendPopupWidth.toString()
        trendHeight.text = settings.trendPopupHeight.toString()
        // 默认透明
        trendPopupBackground.text = settings.trendPopupBackground
    }

    override fun isModified(settings: Settings): Boolean {
        if (settings.enabled != toggleCheckBox.isSelected) {
            return true
        }

        if (settings.preAndAfterTrading != preAndAfterTradingCheckBox.isSelected) {
            return true
        }

        if (settings.interval != pollIntervalTextField.value as Long) {
            return true
        }

        if (settings.enableScriptLog != scriptLogCheckBox.isSelected) {
            return true
        }

        val useDataSourceId = settings.useDataSourceId
        if (tigerRadioButton.isSelected) {
            if (useDataSourceId == ScriptPollDataSourceSetting::class.simpleName) {
                return true
            }
        } else {
            if (useDataSourceId != ScriptPollDataSourceSetting::class.simpleName) {
                return true
            }
        }
        if (pollCheckBox.isSelected) {
            if (useDataSourceId == TigerDataSourceSetting::class.simpleName) {
                return true
            }
        } else {
            if (useDataSourceId != TigerDataSourceSetting::class.simpleName) {
                return true
            }
        }

        if (createTigerDataSourceSetting() != settings.tigerDataSourceSetting ?: TigerDataSourceSetting()) {
            return true
        }
        if (createTigerPollDataSourceSetting() != settings.tigerPollDataSourceSetting ?: TigerPollDataSourceSetting()) {
            return true
        }
        if (createScriptPollDataSourceSetting() != settings.scriptPollDataSourceSetting ?: ScriptPollDataSourceSetting()) {
            return true
        }
        if (fallColorBtn.text != settings.fallColor) {
            return true
        }
        if (riseColorBtn.text != settings.riseColor) {
            return true
        }
        if (settings.pattern != displayFormatTextField.text) {
            return true
        }

        if (!CollectionUtils.isEqualCollection(settings.symbols, transform(symbolTextArea.text))) {
            return true
        }

        /* 趋势图设置 */
        trendGroupMap.forEach { (type, ui) ->
            if (ui.isSelected && type != settings.trendType) {
                return true
            }
        }

        if (settings.trendPopupWidth != trendWidth.text.toIntOrNull()) {
            return true
        }
        if (settings.trendPopupHeight != trendHeight.text.toIntOrNull()) {
            return true
        }

        if (settings.trendPopupBackground != trendPopupBackground.text) {
            return true
        }

        /* 脚本设置 */

        val saved = settings.scriptPollDataSourceSetting?.paths ?: emptyList()
        val scriptModel = scriptList.model
        if (saved.size != scriptModel.size) {
            return true
        }
        val scripts = scriptList.getItems()
        if (!CollectionUtils.isEqualCollection(saved, scripts)) {
            return true
        }
        return false
    }

    override fun apply(settings: Settings) {
        log.debug("apply")

        settings.enabled = toggleCheckBox.isSelected
        val text = symbolTextArea.text
        settings.symbols = transform(text)
        settings.tigerDataSourceSetting = createTigerDataSourceSetting()
        settings.tigerPollDataSourceSetting = createTigerPollDataSourceSetting()
        settings.scriptPollDataSourceSetting = createScriptPollDataSourceSetting()
        settings.fallColor = fallColorBtn.text
        settings.riseColor = riseColorBtn.text

        settings.pattern = displayFormatTextField.text

        settings.enableScriptLog = scriptLogCheckBox.isSelected

        /* 趋势图设置 */
        trendGroupMap.forEach {
            if (it.value.isSelected) {
                settings.trendType = it.key
            }
        }

        settings.trendPopupWidth = trendWidth.text.toInt()
        settings.trendPopupHeight = trendHeight.text.toInt()

        settings.trendPopupBackground = trendPopupBackground.text

        // 数据源
        settings.interval = pollIntervalTextField.value as Long
        if (pollCheckBox.isSelected) {
            if (scriptRadioButton.isSelected) {
                settings.useDataSourceId = ScriptPollDataSourceSetting::class.simpleName
            } else {
                settings.useDataSourceId = TigerPollDataSourceSetting::class.simpleName
            }
        } else {
            settings.useDataSourceId = TigerDataSourceSetting::class.simpleName
        }

        settings.preAndAfterTrading = preAndAfterTradingCheckBox.isSelected

        /* 启动/更新 */
        val quotesService = QuotesService.instance
        runAsync {
            if (settings.enabled) {
                quotesService.start()
                quotesService.updateSubscribe()
            } else {
                quotesService.close()
            }
        }
        Application.messageBus.syncPublisher(QuotesTopic).settingChange()
    }


    override fun getComponent(): JComponent {
        return root
    }

    private fun isPollDataSourceId(sourceId: String?): Boolean {
        if (sourceId == TigerDataSourceSetting::class.simpleName) {
            return false
        }
        return true
    }

    private fun createTigerDataSourceSetting(): TigerDataSourceSetting {
        return TigerDataSourceSetting(
            StringUtils.trimToNull(tigerIdTextField.text),
            StringUtils.trimToNull(privateKeyTextArea.text)
        )
    }

    private fun createTigerPollDataSourceSetting(): TigerPollDataSourceSetting {
        return TigerPollDataSourceSetting(
            StringUtils.trimToNull(tigerIdTextField.text),
            StringUtils.trimToNull(privateKeyTextArea.text),
            pollIntervalTextField.value as Long
        )
    }

    private fun createScriptPollDataSourceSetting(): ScriptPollDataSourceSetting {

        val scriptList = scriptList.getItems()
        return ScriptPollDataSourceSetting(pollIntervalTextField.value as Long, scriptList, scriptList)
    }

    private fun createDataSourceSetting(): DataSourceSetting {
        if (tigerRadioButton.isSelected) {
            if (pollCheckBox.isSelected) {
                return createTigerPollDataSourceSetting()
            } else {
                return createTigerDataSourceSetting()
            }
        } else {
            return createScriptPollDataSourceSetting()
        }
    }

    private fun transform(text: String?): MutableSet<StockSummary> {
        val set = linkedSetOf<StockSummary>()
        if (text.isNullOrBlank()) {
            return set
        }
        try {
            BufferedReader(StringReader(text)).use { reader ->
                var line: String
                while (reader.readLine().also { line = it.trim() } != null) {
                    if (line.isNotBlank()) {
                        val data = line.split('@')
                        if (data.size == 2) {
                            set.add(StockSummary(data[0], data[1]))
                        } else {
                            set.add(StockSummary(data[0], ""))
                        }
                    }
                }
            }
        } catch (ignore: Exception) {
        }
        return set
    }

    private fun setColorButton(button: JButton, color: String) {
        button.text = color
        button.putClientProperty("JButton.backgroundColor", getColor(color))
    }

    private class ColorPickerHandler(private val colorButton: JButton) :
        ClickListener(), ColorListener {
        override fun colorChanged(color: Color, source: Any) {
            colorButton.text = ColorUtil.toHex(color)
            colorButton.putClientProperty("JButton.backgroundColor", color)
        }

        override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
            ColorPicker.showColorPickerPopup(null, getColor(colorButton.text), this)
            return true
        }

        init {
            installOn(colorButton)
        }
    }
}