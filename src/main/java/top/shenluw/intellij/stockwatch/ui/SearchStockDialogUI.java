package top.shenluw.intellij.stockwatch.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;
import top.shenluw.intellij.stockwatch.StockSummary;

import javax.swing.*;

public class SearchStockDialogUI extends DialogWrapper {
    private JPanel contentPane;
    protected JBList<StockSummary> resultList;
    protected JLabel previewLabel;
    protected JTextField searchTextField;
    protected JRadioButton trendMinuteRadioButton;
    protected JRadioButton trendDailyRadioButton;
    protected JRadioButton trendMonthlyRadioButton;
    protected JRadioButton trendWeeklyRadioButton;

    protected SearchStockDialogUI(@Nullable Project project) {
        super(project);
        setTitle("股票信息搜索");
        init();
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

}
