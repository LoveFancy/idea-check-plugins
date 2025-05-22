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

    public static DeprecatedApiSettings getInstance(Project project) {
        return project.getService(DeprecatedApiSettings.class);
    }

    public List<DeprecatedApi> getDeprecatedApis() {
        return deprecatedApis;
    }

    public void setDeprecatedApis(List<DeprecatedApi> deprecatedApis) {
        this.deprecatedApis = deprecatedApis;
    }

    @Override
    public @Nullable DeprecatedApiSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DeprecatedApiSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
} 