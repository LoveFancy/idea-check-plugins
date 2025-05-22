# Deprecated API Checker

一个用于检测项目中废弃接口使用的 IntelliJ IDEA 插件。

## 功能特点

- 检测项目中的废弃接口使用情况
- 支持自定义废弃接口清单
- 在 Problems 窗口中显示废弃接口使用警告
- 支持快速跳转到问题代码位置

## 系统要求

- IntelliJ IDEA 2024.1 或更高版本
- macOS 操作系统

## 安装说明

### 方法一：从 JetBrains 插件市场安装

1. 打开 IntelliJ IDEA
2. 进入 Settings/Preferences -> Plugins
3. 在 Marketplace 中搜索 "Deprecated API Checker"
4. 点击 Install 安装插件
5. 重启 IDE

### 方法二：手动安装

1. 下载最新的插件包（.zip 文件）
2. 打开 IntelliJ IDEA
3. 进入 Settings/Preferences -> Plugins
4. 点击齿轮图标，选择 "Install Plugin from Disk..."
5. 选择下载的插件包
6. 重启 IDE

## 使用方法

1. 安装插件后，在 IDE 的 Tools 菜单中找到 "Deprecated API Checker" 设置项
2. 在设置界面中配置废弃接口清单
3. 插件会自动扫描项目中的代码
4. 在 Problems 窗口中查看检测结果
5. 点击问题可以快速跳转到对应的代码位置

## 废弃接口清单配置

废弃接口清单使用 JSON 格式，示例：

```json
{
  "deprecatedApis": [
    {
      "className": "com.example.OldService",
      "methodName": "deprecatedMethod"
    }
  ]
}
```

## 开发说明

### 构建项目

```bash
./gradlew build
```

### 运行测试

```bash
./gradlew test
```

### 生成插件包

```bash
./gradlew buildPlugin
```

生成的插件包位于 `build/distributions` 目录下。

## 许可证

MIT License 
