package top.shenluw.intellij.stockwatch

import com.intellij.openapi.options.ConfigurableBase

/**
 * @author shenlw
 * @date 2020/3/29 15:30
 */
class OptionsConfigurable : ConfigurableBase<SettingView, Settings>(PLUGIN_ID, PLUGIN_NAME, null) {
    override fun getSettings(): Settings {
        return Settings.instance
    }

    override fun createUi(): SettingView {
        return SettingView()
    }
}