package com.github.gt921.deprecatedapichecker.ui;

import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.github.gt921.deprecatedapichecker.service.DeprecatedApiService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.Objects;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeprecatedApiConfigurable implements Configurable {
    private final Project project;
    private JPanel mainPanel;
    private JTextField appIdField;
    private JTextField unitIdField;
    private JTextField serverUrlField;
    private JComboBox<String> loadModeCombo;
    private JTextField localFileField;
    private JButton fileChooseBtn;
    private JPanel serverUrlPanel;
    private JPanel localFilePanel;
    private JPanel serverUrlRowPanel;
    private JPanel localFileRowPanel;

    public DeprecatedApiConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Deprecated API Checker";
    }

    @Override
    public JComponent createComponent() {
        int fieldWidth = 350;
        int fieldHeight = 28;
        appIdField = new JTextField();
        appIdField.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        unitIdField = new JTextField();
        unitIdField.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        serverUrlField = new JTextField();
        serverUrlField.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        loadModeCombo = new JComboBox<>(new String[]{"远端服务器", "本地文件"});
        localFileField = new JTextField();
        localFileField.setPreferredSize(new Dimension(fieldWidth - 80, fieldHeight));
        fileChooseBtn = new JButton("选择...");
        localFilePanel = new JPanel(new BorderLayout());
        localFilePanel.add(localFileField, BorderLayout.CENTER);
        localFilePanel.add(fileChooseBtn, BorderLayout.EAST);
        serverUrlPanel = new JPanel(new BorderLayout());
        serverUrlPanel.add(serverUrlField, BorderLayout.CENTER);
        serverUrlRowPanel = new JPanel(new BorderLayout());
        serverUrlRowPanel.add(new JLabel("服务器地址:"), BorderLayout.WEST);
        serverUrlRowPanel.add(serverUrlPanel, BorderLayout.CENTER);
        localFileRowPanel = new JPanel(new BorderLayout());
        localFileRowPanel.add(new JLabel("本地文件:"), BorderLayout.WEST);
        localFileRowPanel.add(localFilePanel, BorderLayout.CENTER);
        loadModeCombo.addActionListener(e -> updateModeFields());
        JPanel appInfoPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("加载方式:", loadModeCombo)
                .addLabeledComponent("应用ID:", appIdField)
                .addLabeledComponent("单元ID:", unitIdField)
                .addComponent(serverUrlRowPanel)
                .addComponent(localFileRowPanel)
                .getPanel();
        appInfoPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        appInfoPanel.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));
        JPanel mainPanelWrap = new JPanel(new BorderLayout());
        mainPanelWrap.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        mainPanelWrap.add(appInfoPanel, BorderLayout.NORTH);
        mainPanel = mainPanelWrap;
        updateModeFields();
        fileChooseBtn.addActionListener(e -> chooseLocalFile());
        return mainPanel;
    }

    private void updateModeFields() {
        boolean isRemote = loadModeCombo.getSelectedIndex() == 0;
        serverUrlRowPanel.setVisible(isRemote);
        localFileRowPanel.setVisible(!isRemote);
        serverUrlField.setEnabled(isRemote);
        localFileField.setEnabled(!isRemote);
        fileChooseBtn.setEnabled(!isRemote);
    }

    private void chooseLocalFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int ret = chooser.showOpenDialog(mainPanel);
        if (ret == JFileChooser.APPROVE_OPTION) {
            localFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    @Override
    public boolean isModified() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        boolean appIdChanged = !Objects.equals(appIdField.getText().trim(), settings.getAppId());
        boolean unitIdChanged = !Objects.equals(unitIdField.getText().trim(), settings.getUnitId());
        boolean serverUrlChanged = !Objects.equals(serverUrlField.getText().trim(), settings.getServerUrl());
        boolean loadModeChanged = !Objects.equals(loadModeCombo.getSelectedIndex() == 0 ? "remote" : "local", settings.getLoadMode());
        boolean localFileChanged = !Objects.equals(localFileField.getText().trim(), settings.getLocalFilePath());
        return appIdChanged || unitIdChanged || serverUrlChanged || loadModeChanged || localFileChanged;
    }

    @Override
    public void apply() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        settings.setAppId(appIdField.getText().trim());
        settings.setUnitId(unitIdField.getText().trim());
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setLoadMode(loadModeCombo.getSelectedIndex() == 0 ? "remote" : "local");
        settings.setLocalFilePath(localFileField.getText().trim());
    }

    @Override
    public void reset() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        appIdField.setText(settings.getAppId() != null ? settings.getAppId() : "");
        unitIdField.setText(settings.getUnitId() != null ? settings.getUnitId() : "");
        serverUrlField.setText(settings.getServerUrl() != null ? settings.getServerUrl() : "http://168.64.1.1:8080/unusecode/queryApiList");
        loadModeCombo.setSelectedIndex("remote".equals(settings.getLoadMode()) ? 0 : 1);
        localFileField.setText(settings.getLocalFilePath() != null ? settings.getLocalFilePath() : "");
        updateModeFields();
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
} 