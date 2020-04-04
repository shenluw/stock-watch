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
import top.shenluw.plugin.dubbo.utils.KLogger
import java.awt.Color
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SpinnerNumberModel

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

                val client = QuotesService.instance.getDataSourceClient(setting)
                if (client == null) {
                    builder.title("error")
                        .centerPanel(JLabel("not support client setting"))
                    builder.show()
                    return
                }

                testConnectBtn.isEnabled = false

                client.testConfig(setting)
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
    }

    private fun initPatternSetting() {
        prefixCountSpinner.model = SpinnerNumberModel(2, 0, 10, 1)

        prefixCountSpinner.isEnabled = !fullNameCheckBox.isSelected
        fullNameCheckBox.addItemListener {
            prefixCountSpinner.isEnabled = it.stateChange != ItemEvent.SELECTED
        }

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
    }

    override fun isModified(settings: Settings): Boolean {
        if (settings.enabled != toggleCheckBox.isSelected) {
            return true
        }
        if (settings.onlyCloseUI != onlyCloseUICheckBox.isSelected) {
            return true
        }

        val dataSourceSetting = settings.tigerDataSourceSetting ?: TigerDataSourceSetting()
        val setting = createDataSourceSetting()
        if (setting != dataSourceSetting) {
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
        settings.tigerDataSourceSetting = createDataSourceSetting()
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

        Application.messageBus.syncPublisher(QuotesTopic).settingChange()
    }


    override fun getComponent(): JComponent {
        return root
    }

    private fun createDataSourceSetting(): TigerDataSourceSetting {
        return TigerDataSourceSetting(
            StringUtils.trimToNull(tigerIdTextField.text),
            StringUtils.trimToNull(privateKeyTextArea.text)
        )
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
                while (reader.readLine().also { line = it } != null) {
                    set.add(line)
                }
            }
        } catch (e: Exception) {
        }
        return set
    }

    private fun setColorButton(button: JButton, color: String) {
        button.text = color
        button.background = getColor(color)
    }

    private class ColorPickerHandler(private val colorButton: JButton) :
        ClickListener(), ColorListener {
        override fun colorChanged(color: Color, source: Any) {
            colorButton.text = ColorUtil.toHex(color)
            colorButton.background = color
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