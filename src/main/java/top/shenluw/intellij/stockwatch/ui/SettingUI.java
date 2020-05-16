package top.shenluw.intellij.stockwatch.ui;

import com.intellij.ui.HyperlinkLabel;

import javax.swing.*;

/**
 * @author shenlw
 * @date 2020/3/29 15:31
 */
public class SettingUI {

    protected JPanel root;
    protected JCheckBox toggleCheckBox;
    protected JTextField tigerIdTextField;
    protected JTextArea privateKeyTextArea;
    protected JTextArea symbolTextArea;
    protected HyperlinkLabel helpLinkLabel;
    protected JButton riseColorBtn;
    protected JButton fallColorBtn;
    protected JButton testConnectBtn;
    protected JSpinner prefixCountSpinner;
    protected JCheckBox onlyCloseUICheckBox;
    protected JCheckBox pollCheckBox;
    protected JFormattedTextField pollIntervalTextField;
    protected JRadioButton scriptRadioButton;
    protected JRadioButton tigerRadioButton;
    protected JCheckBox preAndAfterTradingCheckBox;
    protected JButton addScriptButton;
    protected JButton removeScriptButton;
    protected JList<String> scriptList;
    protected JCheckBox scriptLogCheckBox;
    protected JTextField displayFormatTextField;

}
