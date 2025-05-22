package com.github.gt921.deprecatedapichecker.ui;

import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DeprecatedApiConfigurable implements Configurable {
    private final Project project;
    private JPanel mainPanel;
    private JBList<DeprecatedApi> apiList;
    private DefaultListModel<DeprecatedApi> listModel;
    private JTextField classNameField;
    private JTextField methodNameField;

    public DeprecatedApiConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Deprecated API Checker";
    }

    @Override
    public JComponent createComponent() {
        listModel = new DefaultListModel<>();
        apiList = new JBList<>(listModel);
        apiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        classNameField = new JTextField();
        methodNameField = new JTextField();

        JButton addButton = new JButton("添加");
        addButton.addActionListener(e -> addDeprecatedApi());

        JButton removeButton = new JButton("删除");
        removeButton.addActionListener(e -> removeDeprecatedApi());

        JPanel inputPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("类名:", classNameField)
                .addLabeledComponent("方法名:", methodNameField)
                .addComponent(addButton)
                .addComponent(removeButton)
                .getPanel();

        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBScrollPane(apiList))
                .addComponent(inputPanel)
                .getPanel();

        return mainPanel;
    }

    private void addDeprecatedApi() {
        String className = classNameField.getText().trim();
        String methodName = methodNameField.getText().trim();

        if (className.isEmpty() || methodName.isEmpty()) {
            Messages.showWarningDialog(project, "类名和方法名不能为空", "输入错误");
            return;
        }

        DeprecatedApi api = new DeprecatedApi(className, methodName);
        listModel.addElement(api);
        classNameField.setText("");
        methodNameField.setText("");
    }

    private void removeDeprecatedApi() {
        int selectedIndex = apiList.getSelectedIndex();
        if (selectedIndex != -1) {
            listModel.remove(selectedIndex);
        }
    }

    @Override
    public boolean isModified() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        List<DeprecatedApi> currentApis = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            currentApis.add(listModel.get(i));
        }
        return !currentApis.equals(settings.getDeprecatedApis());
    }

    @Override
    public void apply() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        List<DeprecatedApi> apis = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            apis.add(listModel.get(i));
        }
        settings.setDeprecatedApis(apis);
    }

    @Override
    public void reset() {
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        listModel.clear();
        for (DeprecatedApi api : settings.getDeprecatedApis()) {
            listModel.addElement(api);
        }
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
} 