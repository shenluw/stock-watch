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
import top.shenluw.plugin.dubbo.utils.KLogger
import java.awt.Color
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import javax.swing.*
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
                        .filter { !TradingUtil.isIgnoreSymbol(it) }.toSortedSet()
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

        initPatternSetting()

        initPoll()

        val group = ButtonGroup()
        group.add(tigerRadioButton)
        group.add(sinaRadioButton)
    }

    private fun initPatternSetting() {
        prefixCountSpinner.model = SpinnerNumberModel(2, 0, 10, 1)

        prefixCountSpinner.isEnabled = !fullNameCheckBox.isSelected
        fullNameCheckBox.addItemListener {
            prefixCountSpinner.isEnabled = it.stateChange != ItemEvent.SELECTED
        }
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
        val symbols: SortedSet<String> = settings.symbols
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

        val patternSetting = settings.patternSetting
        fullNameCheckBox.isSelected = patternSetting.fullName
        useSymbolCheckBox.isSelected = patternSetting.useSymbol
        prefixCountSpinner.value = patternSetting.namePrefix

        onlyCloseUICheckBox.isSelected = settings.onlyCloseUI

        setColorButton(fallColorBtn, settings.fallColor)
        setColorButton(riseColorBtn, settings.riseColor)

        val useDataSourceId = settings.useDataSourceId
        pollCheckBox.isSelected = isPollDataSourceId(useDataSourceId)

        pollIntervalTextField.value = settings.interval

        if (useDataSourceId == SinaPollDataSourceSetting::class.simpleName) {
            sinaRadioButton.isSelected = true
        } else {
            tigerRadioButton.isSelected = true
        }

        preAndAfterTradingCheckBox.isSelected = settings.preAndAfterTrading
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
        val useDataSourceId = settings.useDataSourceId
        if (tigerRadioButton.isSelected) {
            if (useDataSourceId == SinaPollDataSourceSetting::class.simpleName) {
                return true
            }
        } else {
            if (useDataSourceId != SinaPollDataSourceSetting::class.simpleName) {
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
        if (createSinaPollDataSourceSetting() != settings.sinaPollDataSourceSetting ?: SinaPollDataSourceSetting()) {
            return true
        }
        if (fallColorBtn.text != settings.fallColor) {
            return true
        }
        if (riseColorBtn.text != settings.riseColor) {
            return true
        }
        val patternSetting = createPatternSetting()

        if (settings.patternSetting != patternSetting) {
            return true
        }

        if (!CollectionUtils.isEqualCollection(settings.symbols, transform(symbolTextArea.text))) {
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
        settings.sinaPollDataSourceSetting = createSinaPollDataSourceSetting()
        settings.fallColor = fallColorBtn.text
        settings.riseColor = riseColorBtn.text

        settings.patternSetting = createPatternSetting()

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
            if (sinaRadioButton.isSelected) {
                settings.useDataSourceId = SinaPollDataSourceSetting::class.simpleName
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

    private fun createSinaPollDataSourceSetting(): SinaPollDataSourceSetting {
        return SinaPollDataSourceSetting(pollIntervalTextField.value as Long)
    }

    private fun createDataSourceSetting(): DataSourceSetting {
        if (tigerRadioButton.isSelected) {
            if (pollCheckBox.isSelected) {
                return createTigerPollDataSourceSetting()
            } else {
                return createTigerDataSourceSetting()
            }
        } else {
            return createSinaPollDataSourceSetting()
        }
    }

    private fun createPatternSetting(): PatternSetting {
        return PatternSetting(
            fullNameCheckBox.isSelected,
            prefixCountSpinner.value as Int,
            useSymbolCheckBox.isSelected
        )
    }

    private fun transform(text: String?): SortedSet<String> {
        val set: SortedSet<String> = TreeSet()
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
        } catch (e: Exception) {
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
            ColorPicker.showColorPickerPopup(CurrentProject, colorButton.background, this)
            return true
        }

        init {
            installOn(colorButton)
        }
    }
}