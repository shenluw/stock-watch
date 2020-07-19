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
import top.shenluw.intellij.CurrentProject
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
                val builder = DialogBuilder(CurrentProject)
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

                client.testConfig(
                    setting,
                    transform(symbolTextArea.text)
                        .filter { !TradingUtil.isIgnoreSymbol(it) }.toHashSet()
                )
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

    override fun reset(settings: Settings) {
        log.debug("reset")

        toggleCheckBox.isSelected = settings.enabled
        val sourceSetting = settings.tigerDataSourceSetting
        tigerIdTextField.text = sourceSetting?.tigerId
        privateKeyTextArea.text = sourceSetting?.privateKey
        val sb = StringBuilder()
        val symbols: Set<String> = settings.symbols
        if (!symbols.isNullOrEmpty()) {
            val iterator: Iterator<String?> = symbols.iterator()
            while (iterator.hasNext()) {
                sb.append(iterator.next())
                if (iterator.hasNext()) {
                    sb.append("\n")
                }
            }
        }
        symbolTextArea.text = sb.toString()

        scriptLogCheckBox.isSelected = settings.enableScriptLog

        displayFormatTextField.text = settings.pattern

        onlyCloseUICheckBox.isSelected = settings.onlyCloseUI

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
    }

    override fun isModified(settings: Settings): Boolean {
        if (settings.enabled != toggleCheckBox.isSelected) {
            return true
        }
        if (settings.onlyCloseUI != onlyCloseUICheckBox.isSelected) {
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
        settings.onlyCloseUI = onlyCloseUICheckBox.isSelected
        val text = symbolTextArea.text
        settings.symbols = transform(text)
        settings.tigerDataSourceSetting = createTigerDataSourceSetting()
        settings.tigerPollDataSourceSetting = createTigerPollDataSourceSetting()
        settings.scriptPollDataSourceSetting = createScriptPollDataSourceSetting()
        settings.fallColor = fallColorBtn.text
        settings.riseColor = riseColorBtn.text

        settings.pattern = displayFormatTextField.text

        settings.enableScriptLog = scriptLogCheckBox.isSelected

        val quotesService = QuotesService.instance
        runAsync {
            if (settings.enabled) {
                quotesService.start()
                quotesService.updateSubscribe()
            } else {
                quotesService.close()
            }
        }

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
            transformPrivateKey(privateKeyTextArea.text)
        )
    }

    private fun createTigerPollDataSourceSetting(): TigerPollDataSourceSetting {
        return TigerPollDataSourceSetting(
            StringUtils.trimToNull(tigerIdTextField.text),
            transformPrivateKey(privateKeyTextArea.text),
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

    private fun transform(text: String?): MutableSet<String> {
        val set = linkedSetOf<String>()
        if (text.isNullOrBlank()) {
            return set
        }
        try {
            BufferedReader(StringReader(text)).use { reader ->
                var line: String
                while (reader.readLine().also { line = it.trim() } != null) {
                    if (!line.isBlank()) {
                        set.add(line)
                    }
                }
            }
        } catch (ignore: Exception) {
        }
        return set
    }

    private fun transformPrivateKey(text: String?): String? {
        if (text.isNullOrBlank()) {
            return null
        }
        var txt = text

        val key = "KEY-----"
        var index = txt.indexOf(key)
        if (index >= 0) {
            txt = txt.substring(index + key.length)
        }
        index = txt.indexOf("-----END")
        if (index >= 0) {
            txt = txt.substring(0, index)
        }
        return txt
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
            ColorPicker.showColorPickerPopup(CurrentProject, getColor(colorButton.text), this)
            return true
        }

        init {
            installOn(colorButton)
        }
    }
}