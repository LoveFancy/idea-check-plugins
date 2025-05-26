package com.github.gt921.deprecatedapichecker.settings;

import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "DeprecatedApiSettings",
    storages = @Storage("deprecated-api-checker.xml")
)
public class DeprecatedApiSettings implements PersistentStateComponent<DeprecatedApiSettings> {
    private List<DeprecatedApi> deprecatedApis = new ArrayList<>();
    private String appId;
    private String unitId;
    /**
     * 服务器地址，完整URL（如：http://168.64.1.1:8080/unusecode/queryApiList）
     */
    private String serverUrl = "http://168.64.1.1:8080/unusecode/queryApiList";
    private boolean useMock = true;
    /**
     * 加载方式 remote: 远端服务器, local: 本地文件
     */
    private String loadMode = "remote";
    /**
     * 本地文件路径
     */
    private String localFilePath;
    /**
     * 最近一次API配置加载时间 yyyy-MM-dd HH:mm:ss
     */
    private String lastLoadTime;

    public static DeprecatedApiSettings getInstance(Project project) {
        return project.getService(DeprecatedApiSettings.class);
    }

    public List<DeprecatedApi> getDeprecatedApis() {
        return deprecatedApis;
    }

    public void setDeprecatedApis(List<DeprecatedApi> deprecatedApis) {
        this.deprecatedApis = deprecatedApis;
    }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public boolean isUseMock() { return useMock; }
    public void setUseMock(boolean useMock) { this.useMock = useMock; }
    public String getLoadMode() { return loadMode; }
    public void setLoadMode(String loadMode) { this.loadMode = loadMode; }
    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }
    public String getLastLoadTime() { return lastLoadTime; }
    public void setLastLoadTime(String lastLoadTime) { this.lastLoadTime = lastLoadTime; }

    @Override
    public @Nullable DeprecatedApiSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DeprecatedApiSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
} 