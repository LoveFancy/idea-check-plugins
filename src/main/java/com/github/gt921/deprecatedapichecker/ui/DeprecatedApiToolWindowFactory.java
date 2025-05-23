package com.github.gt921.deprecatedapichecker.ui;

import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DeprecatedApiToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder();
        List<DeprecatedApi> apis = DeprecatedApiSettings.getInstance(project).getDeprecatedApis();
        if (apis.isEmpty()) {
            sb.append("暂无废弃 API 配置\n");
        } else {
            for (DeprecatedApi api : apis) {
                sb.append("类: ").append(api.getClassName()).append("\n");
                sb.append("方法: ").append(api.getMethodName() != null ? api.getMethodName() : "<类本身>").append("\n");
                if (api.getNoModifyDays() > 0) {
                    sb.append("累计无修改时长: ").append(api.getNoModifyDays()).append("天\n");
                }
                if (api.getNoCallDays() > 0) {
                    sb.append("累计无调用时长: ").append(api.getNoCallDays()).append("天\n");
                }
                if (api.getLastCommit() != null) {
                    DeprecatedApi.CommitInfo commit = api.getLastCommit();
                    sb.append("最近一次提交: ")
                      .append(commit.getDate() != null ? commit.getDate() : "")
                      .append(" by ")
                      .append(commit.getAuthor() != null ? commit.getAuthor() : "")
                      .append(": ")
                      .append(commit.getMessage() != null ? commit.getMessage() : "");
                    if (commit.getId() != null) {
                        sb.append(" [").append(commit.getId()).append("]");
                    }
                    sb.append("\n");
                }
                sb.append("-----------------------------\n");
            }
        }
        textArea.setText(sb.toString());
        panel.add(new JBScrollPane(textArea), BorderLayout.CENTER);
        Content content = ContentFactory.getInstance().createContent(panel, "废弃 API 列表", false);
        toolWindow.getContentManager().addContent(content);
    }
} 