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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        DeprecatedApiSettings settings = DeprecatedApiSettings.getInstance(project);
        String response = null;
        boolean loaded = false;
        if ("remote".equals(settings.getLoadMode())) {
            if (settings.isUseMock()) {
                response = DeprecatedApiClient.mockQueryApiList();
            } else {
                response = DeprecatedApiClient.realQueryApiList(
                    settings.getServerUrl(),
                    settings.getAppId(),
                    settings.getUnitId(),
                    0, // unuseratio
                    1000, // limit
                    0 // offset
                );
                LOG.info("[API加载] 远端模式，接口返回内容前500字符：" + (response != null ? response.substring(0, Math.min(500, response.length())) : "null"));
            }
            loaded = response != null;
        } else if ("local".equals(settings.getLoadMode())) {
            String filePath = settings.getLocalFilePath();
            LOG.info("[API加载] 本地文件模式，加载文件路径：" + filePath);
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    String json = new String(Files.readAllBytes(Paths.get(filePath)), java.nio.charset.StandardCharsets.UTF_8);
                    LOG.info("[API加载] 本地文件内容前500字符：" + (json.length() > 500 ? json.substring(0, 500) : json));
                    response = json;
                    loaded = true;
                } catch (Exception e) {
                    LOG.error("读取本地废弃API文件失败: " + filePath, e);
                }
            }
        }
        if (response != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
                com.fasterxml.jackson.databind.JsonNode apis = root.has("deprecatedApis") ? root.get("deprecatedApis") : (root.has("data") ? root.get("data").get("deprecatedApis") : null);
                if (apis != null) {
                    java.util.List<DeprecatedApi> apiList = java.util.Arrays.asList(objectMapper.treeToValue(apis, DeprecatedApi[].class));
                    settings.setDeprecatedApis(apiList);
                    LOG.info("Loaded " + apiList.size() + " deprecated APIs from " + settings.getLoadMode() + " interface");
                    loaded = true;
                } else {
                    settings.setDeprecatedApis(java.util.Collections.emptyList());
                    LOG.warn("API响应中未找到 deprecatedApis 字段");
                }
            } catch (Exception e) {
                LOG.error("Error parsing API response", e);
            }
        } else {
            settings.setDeprecatedApis(java.util.Collections.emptyList());
            LOG.warn("API response is null");
        }
        // 更新时间戳
        if (loaded) {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            settings.setLastLoadTime(now);
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
            service.loadDefaultConfig(project);
        }
    }

    // 新建 DeprecatedApiClient 独立类
    class DeprecatedApiClient {
        public static String mockQueryApiList() {
            try (java.io.InputStream is = DeprecatedApiClient.class.getClassLoader().getResourceAsStream("deprecated-apis.json")) {
                if (is == null) return null;
                java.util.Scanner scanner = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8.name());
                String json = scanner.useDelimiter("\\A").next();
                scanner.close();
                return "{\n" +
                        "  \"code\": \"0\",\n" +
                        "  \"msg\": \"success\",\n" +
                        "  \"data\": {\n" +
                        "    \"appid\": \"demo-app\",\n" +
                        "    \"unitid\": \"demo-unit\",\n" +
                        "    \"deprecatedApis\": " + json.substring(json.indexOf('['), json.lastIndexOf(']') + 1) + "\n" +
                        "  }\n" +
                        "}";
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String realQueryApiList(String url, String appid, String unitid, int unuseratio, int limit, int offset) {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                String body = String.format("{\"appid\":\"%s\",\"unitid\":\"%s\",\"unuseratio\":%d,\"limit\":%d,\"offset\":%d}",
                        appid == null ? "" : appid, unitid == null ? "" : unitid, unuseratio, limit, offset);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (java.io.InputStream is = conn.getInputStream()) {
                        java.util.Scanner scanner = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8.name());
                        String resp = scanner.useDelimiter("\\A").next();
                        scanner.close();
                        return resp;
                    }
                } else {
                    LOG.warn("HTTP请求失败，状态码:" + code);
                }
            } catch (Exception e) {
                LOG.error("真实接口请求异常", e);
            }
            return null;
        }
    }
} 