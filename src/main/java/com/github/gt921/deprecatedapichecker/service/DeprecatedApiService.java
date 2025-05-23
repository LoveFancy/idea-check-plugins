package com.github.gt921.deprecatedapichecker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import java.io.IOException;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class DeprecatedApiService implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(DeprecatedApiService.class);
    private final ObjectMapper objectMapper;
    private static final String CONFIG_FILE_PATH = "src/main/resources/deprecated-apis.json";

    public DeprecatedApiService() {
        this.objectMapper = new ObjectMapper();
        LOG.info("DeprecatedApiService initialized");
    }

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.info("Starting DeprecatedApiService for project: " + project.getName());
        try {
            loadDefaultConfig(project);
        } catch (Exception e) {
            LOG.error("Error loading default config for project: " + project.getName(), e);
        }
        return Unit.INSTANCE;
    }

    private void loadDefaultConfig(Project project) {
        LOG.info("Loading default config for project: " + project.getName());
        VirtualFile configFile = findConfigFile(project);
        if (configFile != null) {
            LOG.info("Found config file: " + configFile.getPath());
            loadDeprecatedApisFromJson(project, configFile);
        } else {
            LOG.warn("No config file found in project: " + project.getName());
        }
    }

    private VirtualFile findConfigFile(Project project) {
        // 首先在项目根目录下查找
        VirtualFile rootDir = project.getBaseDir();
        LOG.info("Searching for config file in root dir: " + rootDir.getPath());
        
        VirtualFile configFile = rootDir.findFileByRelativePath(CONFIG_FILE_PATH);
        if (configFile != null && configFile.exists()) {
            LOG.info("Found config file in root dir: " + configFile.getPath());
            return configFile;
        }

        // 在模块目录下查找
        LOG.info("Searching for config file in module dirs");
        for (VirtualFile moduleDir : rootDir.getChildren()) {
            if (moduleDir.isDirectory()) {
                LOG.info("Checking module dir: " + moduleDir.getPath());
                configFile = moduleDir.findFileByRelativePath(CONFIG_FILE_PATH);
                if (configFile != null && configFile.exists()) {
                    LOG.info("Found config file in module dir: " + configFile.getPath());
                    return configFile;
                }
            }
        }

        LOG.warn("Config file not found in project: " + project.getName());
        return null;
    }

    public void loadDeprecatedApisFromJson(Project project, VirtualFile jsonFile) {
        try {
            LOG.info("Loading deprecated APIs from: " + jsonFile.getPath());
            DeprecatedApiList apiList = objectMapper.readValue(jsonFile.getInputStream(), DeprecatedApiList.class);
            DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
            List<DeprecatedApi> apis = apiList.getDeprecatedApis();
            settings.setDeprecatedApis(apis);
            LOG.info("Loaded " + apis.size() + " deprecated APIs");
            for (DeprecatedApi api : apis) {
                LOG.info("Loaded deprecated API: " + api.getClassName() + "." + 
                        (api.getMethodName() != null ? api.getMethodName() : "<class>"));
            }
        } catch (IOException e) {
            LOG.error("Error loading deprecated APIs from: " + jsonFile.getPath(), e);
        }
    }

    // 用于JSON反序列化的内部类
    private static class DeprecatedApiList {
        private List<DeprecatedApi> deprecatedApis;

        public List<DeprecatedApi> getDeprecatedApis() {
            return deprecatedApis;
        }

        public void setDeprecatedApis(List<DeprecatedApi> deprecatedApis) {
            this.deprecatedApis = deprecatedApis;
        }
    }

    public static void reloadConfig(Project project) {
        DeprecatedApiService service = project.getService(DeprecatedApiService.class);
        if (service != null) {
            VirtualFile configFile = service.findConfigFile(project);
            if (configFile != null) {
                service.loadDeprecatedApisFromJson(project, configFile);
            }
        }
    }
} 