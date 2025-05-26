package com.github.gt921.deprecatedapichecker.ui;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.Box;
import java.awt.FlowLayout;
import javax.swing.JScrollPane;

import org.jetbrains.annotations.NotNull;

import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.github.gt921.deprecatedapichecker.service.DeprecatedApiService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.PlatformDataKeys;

public class ProjectInfoToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // 顶部项目信息
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        String appId = settings.getAppId();
        String unitId = settings.getUnitId();
        JLabel appIdLabel = new JLabel("应用ID: " + (appId != null ? appId : ""));
        JLabel unitIdLabel = new JLabel("单元ID: " + (unitId != null ? unitId : ""));

        // 配置文件路径
        String configFilePath = "";
        String configFileTime = "";
        DeprecatedApiService service = project.getService(DeprecatedApiService.class);
        VirtualFile configFile = null;
        try {
            java.lang.reflect.Method m = service.getClass().getDeclaredMethod("findConfigFile", Project.class);
            m.setAccessible(true);
            configFile = (VirtualFile) m.invoke(service, project);
        } catch (Exception ignored) {}
        if (configFile != null && configFile.exists()) {
            configFilePath = configFile.getPath();
            configFileTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(configFile.getTimeStamp()));
        }
        JLabel configFileLabel = new JLabel("配置文件: " + (configFilePath.isEmpty() ? "未找到" : configFilePath));
        JLabel configFileTimeLabel = new JLabel(configFileTime.isEmpty() ? "" : ("更新时间: " + configFileTime));

        // 基本信息面板
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(appIdLabel);
        infoPanel.add(unitIdLabel);
        infoPanel.add(configFileLabel);
        infoPanel.add(configFileTimeLabel);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel infoBorderPanel = new JPanel(new BorderLayout());
        infoBorderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("基本信息"));
        infoBorderPanel.add(infoPanel, BorderLayout.CENTER);

        // 接口信息面板
        JButton jumpToCodeBtn = new JButton("跳转代码");
        jumpToCodeBtn.setEnabled(false);
        JButton refreshBtn = new JButton("刷新");
        JPanel tableTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableTopPanel.add(jumpToCodeBtn);
        tableTopPanel.add(refreshBtn);

        // 基本信息面板的时间标签引用
        JLabel lastReadTimeLabel = new JLabel();
        updateLastReadTime(lastReadTimeLabel);
        infoPanel.add(lastReadTimeLabel);

        // API清单表格+分页
        List<DeprecatedApi> allApis = settings.getDeprecatedApis();
        ApiTableModel tableModel = new ApiTableModel(allApis);
        JTable table = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return super.getColumnClass(column);
            }
        };
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // 让表格支持手动拖拽调整所有列宽
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false); // 可选
        // 设置表格行高为1.5倍默认值
        table.setRowHeight((int)(table.getRowHeight() * 1.5));
        JScrollPane tableScroll = new JScrollPane(table);

        // 选择列点击逻辑
        table.getModel().addTableModelListener(e -> {
            int selected = tableModel.getSelectedRow();
            boolean enable = selected != -1;
            jumpToCodeBtn.setEnabled(enable);
        });

        // 选择列单选逻辑
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 0) {
                    tableModel.selectRow(row);
                }
            }
        });

        // 按钮点击事件
        jumpToCodeBtn.addActionListener(e -> {
            int row = tableModel.getSelectedRow();
            DeprecatedApi api = tableModel.getApiAtRow(row);
            if (api != null && api.getClassName() != null && api.getMethodName() != null) {
                openMethodInEditor(project, api.getClassName(), api.getMethodName());
            }
        });

        // 刷新按钮逻辑
        refreshBtn.addActionListener(e -> {
            DeprecatedApiService.reloadConfig(project);
            DeprecatedApiSettings settingsNew = DeprecatedApiSettings.getInstance(project);
            List<DeprecatedApi> newApis = settingsNew.getDeprecatedApis();
            tableModel.setApis(newApis);
            updateLastReadTime(lastReadTimeLabel);
            // 重新获取appId/unitId/configFile等信息
            appIdLabel.setText("应用ID: " + (settingsNew.getAppId() != null ? settingsNew.getAppId() : ""));
            unitIdLabel.setText("单元ID: " + (settingsNew.getUnitId() != null ? settingsNew.getUnitId() : ""));
            String newConfigFilePath = "";
            String newConfigFileTime = "";
            DeprecatedApiService newService = project.getService(DeprecatedApiService.class);
            VirtualFile newConfigFile = null;
            try {
                java.lang.reflect.Method m = newService.getClass().getDeclaredMethod("findConfigFile", Project.class);
                m.setAccessible(true);
                newConfigFile = (VirtualFile) m.invoke(newService, project);
            } catch (Exception ignored) {}
            if (newConfigFile != null && newConfigFile.exists()) {
                newConfigFilePath = newConfigFile.getPath();
                newConfigFileTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(newConfigFile.getTimeStamp()));
            }
            configFileLabel.setText("配置文件: " + (newConfigFilePath.isEmpty() ? "未找到" : newConfigFilePath));
            configFileTimeLabel.setText(newConfigFileTime.isEmpty() ? "" : ("更新时间: " + newConfigFileTime));
        });

        // 分页控件
        JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton prevBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        JLabel pageInfo = new JLabel();
        pagePanel.add(prevBtn);
        pagePanel.add(pageInfo);
        pagePanel.add(nextBtn);

        ActionListener updatePage = e -> {
            pageInfo.setText("第 " + (tableModel.getCurrentPage() + 1) + " / " + tableModel.getTotalPages() + " 页");
            prevBtn.setEnabled(tableModel.getCurrentPage() > 0);
            nextBtn.setEnabled(tableModel.getCurrentPage() < tableModel.getTotalPages() - 1);
        };
        prevBtn.addActionListener(e -> { tableModel.prevPage(); updatePage.actionPerformed(null); });
        nextBtn.addActionListener(e -> { tableModel.nextPage(); updatePage.actionPerformed(null); });
        updatePage.actionPerformed(null);

        // 表格及分页面板
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablePanel.add(tableTopPanel, BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        tablePanel.add(pagePanel, BorderLayout.SOUTH);
        JPanel tableBorderPanel = new JPanel(new BorderLayout());
        tableBorderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("接口信息"));
        tableBorderPanel.add(tablePanel, BorderLayout.CENTER);

        // 主面板垂直排列
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(infoBorderPanel);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(tableBorderPanel);

        // 外层加内边距
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
        outerPanel.add(mainPanel, BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(outerPanel, "项目信息", false);
        toolWindow.getContentManager().addContent(content);

        // 表格右键菜单：复制单元格内容
        javax.swing.JPopupMenu popupMenu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("复制");
        popupMenu.add(copyItem);
        table.setComponentPopupMenu(popupMenu);
        copyItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            if (row >= 0 && col >= 0) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(value.toString());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
            }
        });
        // 鼠标右键选中单元格
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) selectCell(e);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) selectCell(e);
            }
            private void selectCell(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(col, col);
                }
            }
        });
    }

    // 分页表格模型
    private static class ApiTableModel extends AbstractTableModel {
        private final String[] columns = {"选择", "类名", "方法名", "无修改天数", "无调用天数", "最近提交"};
        private final List<DeprecatedApi> allApis;
        private List<DeprecatedApi> pageApis;
        private final int pageSize = 15;
        private int currentPage = 0;
        private int totalPages = 1;
        private int selectedRow = -1; // 当前页选中行
        public ApiTableModel(List<DeprecatedApi> apis) {
            this.allApis = apis != null ? apis : new java.util.ArrayList<>();
            updatePage();
        }
        private void updatePage() {
            int total = allApis.size();
            totalPages = (total + pageSize - 1) / pageSize;
            if (totalPages == 0) totalPages = 1;
            int from = currentPage * pageSize;
            int to = Math.min(from + pageSize, total);
            if (from > to) {
                from = 0;
                currentPage = 0;
                to = Math.min(pageSize, total);
            }
            pageApis = allApis.subList(from, to);
            selectedRow = -1;
            fireTableDataChanged();
        }
        public void prevPage() {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        }
        public void nextPage() {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updatePage();
            }
        }
        public int getCurrentPage() { return currentPage; }
        public int getTotalPages() { return totalPages; }
        public int getSelectedRow() { return selectedRow; }
        public void selectRow(int row) {
            if (row >= 0 && row < pageApis.size()) {
                selectedRow = row;
                fireTableDataChanged();
            }
        }
        @Override public int getRowCount() { return pageApis.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            DeprecatedApi api = pageApis.get(row);
            switch (col) {
                case 0: return row == selectedRow;
                case 1: return api.getClassName();
                case 2: return api.getMethodName();
                case 3: return api.getNoModifyDays();
                case 4: return api.getNoCallDays();
                case 5:
                    DeprecatedApi.CommitInfo c = api.getLastCommit();
                    if (c == null) return "";
                    return (c.getDate() != null ? c.getDate() : "") + " by " + (c.getAuthor() != null ? c.getAuthor() : "") + (c.getMessage() != null ? (": " + c.getMessage()) : "");
                default: return "";
            }
        }
        @Override public boolean isCellEditable(int row, int col) { return col == 0; }
        @Override public void setValueAt(Object aValue, int row, int col) {
            if (col == 0 && aValue instanceof Boolean && (Boolean) aValue) {
                selectRow(row);
            }
        }
        public DeprecatedApi getApiAtRow(int row) {
            if (row >= 0 && row < pageApis.size()) {
                return pageApis.get(row);
            }
            return null;
        }
        public void setApis(List<DeprecatedApi> apis) {
            this.allApis.clear();
            this.allApis.addAll(apis != null ? apis : new java.util.ArrayList<>());
            this.currentPage = 0;
            updatePage();
        }
    }

    // 打开并定位方法
    private void openMethodInEditor(Project project, String className, String methodName) {
        com.intellij.psi.search.GlobalSearchScope scope = com.intellij.psi.search.GlobalSearchScope.allScope(project);
        com.intellij.psi.PsiClass[] classes = com.intellij.psi.JavaPsiFacade.getInstance(project)
            .findClasses(className, scope);
        for (com.intellij.psi.PsiClass psiClass : classes) {
            for (com.intellij.psi.PsiMethod method : psiClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    com.intellij.openapi.fileEditor.OpenFileDescriptor descriptor =
                        new com.intellij.openapi.fileEditor.OpenFileDescriptor(
                            project,
                            method.getContainingFile().getVirtualFile(),
                            method.getTextOffset()
                        );
                    descriptor.navigate(true);
                    return;
                }
            }
        }
        // 未找到时可弹窗提示
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(null, "未找到对应方法: " + className + "." + methodName, "跳转失败", javax.swing.JOptionPane.WARNING_MESSAGE);
        });
    }

    // 更新最后读取时间方法
    private void updateLastReadTime(JLabel label) {
        label.setText("最后读取时间: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
    }
} 