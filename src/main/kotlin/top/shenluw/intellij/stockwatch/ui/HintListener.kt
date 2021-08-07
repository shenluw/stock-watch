package top.shenluw.intellij.stockwatch.ui

import com.intellij.util.castSafelyTo
import java.awt.Color
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField

/**
 * @author Shenluw
 * created: 2021/8/7 15:02
 */
class HintListener(private val hint: String) : FocusListener {
    var origin: Color? = null
    override fun focusGained(e: FocusEvent?) {
        e?.source?.castSafelyTo<JTextField>()?.apply {
            if (hint == text) {
                text = ""
                if (origin != null) {
                    foreground = origin
                }
            }
        }
    }

    override fun focusLost(e: FocusEvent?) {
        e?.source.castSafelyTo<JTextField>()?.apply {
            if (text.isNullOrBlank()) {
                if (origin == null) {
                    origin = foreground
                }
                text = hint
                foreground = Color.GRAY
            }
        }

    }
}