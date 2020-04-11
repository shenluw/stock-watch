package top.shenluw.intellij.stockwatch

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurableUi
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * @author shenlw
 * @date 2020/3/29 15:30
 */
class OptionsConfigurable : MyConfigurableBase<SettingView, Settings>(PLUGIN_ID, PLUGIN_NAME, null) {
    override fun getSettings(): Settings {
        return Settings.instance
    }

    override fun createUi(): SettingView {
        return SettingView()
    }
}

abstract class MyConfigurableBase<UI : ConfigurableUi<S>, S> protected constructor(
    private val id: String,
    private val displayName: String,
    private val helpTopic: String?
) :
    SearchableConfigurable {
    private var ui: UI? = null

    override fun getId(): String {
        return id
    }

    @Nls
    override fun getDisplayName(): String {
        return displayName
    }

    override fun getHelpTopic(): String? {
        return helpTopic
    }

    protected abstract fun getSettings(): S

    override fun reset() {
        ui?.reset(getSettings())
    }

    override fun createComponent(): JComponent {
        if (ui == null) {
            ui = createUi()
        }
        return ui!!.component
    }

    protected abstract fun createUi(): UI

    override fun isModified(): Boolean {
        return ui?.isModified(getSettings()) == true
    }

    override fun apply() {
        ui?.apply(getSettings())
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return ui?.preferredFocusedComponent
    }

    override fun disposeUIResources() {
        val ui = ui
        if (ui != null) {
            this.ui = null
            if (ui is Disposable) {
                Disposer.dispose((ui as Disposable?)!!)
            }
        }
    }

}