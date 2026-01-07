# 火尖枪 Android 应用

## 1. 项目概述

火尖枪是一款为网络安全爱好者与专业渗透测试人员设计的 Android 应用程序。它集成了多种网络攻击与安全测试工具，旨在提供一个移动端的便捷测试平台。

### 1.1 核心功能、目标用户与使用场景

#### 核心功能

- **HTTP 攻击**：允许用户配置并发送自定义的 HTTP 请求，支持 GET、POST 等多种方法。可用于 Web 应用程序的安全性测试，如参数爆破、接口漏洞探测等。
- **MySQL 攻击**：_(此功能正在积极开发中)_ 未来将提供针对 MySQL 数据库的常用攻击能力，例如弱口令爆破、SQL 注入辅助测试等。

#### 目标用户

- 网络安全研究员
- 渗透测试工程师
- 安全技术爱好者
- 希望了解网络协议与攻击技术的开发者

#### 典型使用场景

- 对 Web 应用进行授权下的安全评估与漏洞验证。
- 学习 HTTP 协议及相关攻防技术。
- _(未来)_ 进行数据库安全测试与学习。

### 1.2 技术栈说明

本项目采用现代 Android 开发技术栈，注重性能、稳定性与可维护性。

| 组件           | 技术选型                            | 说明                                     |
| :------------- | :---------------------------------- | :--------------------------------------- |
| **开发语言**   | Kotlin (2.0.21)                     | JetBrains 推出的现代化、安全的编程语言。 |
| **UI 框架**    | Android Jetpack Compose             | 用于构建原生、声明式 UI 的现代工具包。   |
| **异步处理**   | Kotlin Coroutines                   | 简化异步编程，提供高效的并发操作。       |
| **网络请求**   | OkHttp (4.12.0)                     | 强大的 HTTP 客户端库。                   |
| **数据序列化** | Kotlinx Serialization (1.6.3)       | 用于 JSON 等格式的序列化与反序列化。     |
| **架构组件**   | Android Lifecycle ViewModel (2.7.0) | 以生命周期感知的方式管理 UI 相关数据。   |
| **构建工具**   | Gradle (8.11.2)                     | 项目构建与依赖管理。                     |

### 1.3 项目架构

本项目遵循 **MVVM (Model-View-ViewModel)** 架构模式，以实现清晰的职责分离，提升代码的可测试性与可维护性。

- **Model (模型层)**
  - 包含核心数据结构和业务逻辑。
  - 主要文件：`HTTPRequest.kt`、`PasswordAttackRequest.kt`、`RequestBody.kt` 等，定义了请求、攻击配置等数据结构。
- **View (视图层)**
  - 由 Jetpack Compose 构建的用户界面，负责数据展示和用户交互。
  - 主要文件：`HttpAttackPage.kt`、`MySQLAttackPage.kt`、`OutputView.kt`。
- **ViewModel (视图模型层)**
  - 作为 View 和 Model 之间的桥梁，持有 UI 状态，处理界面逻辑。
  - 主要文件：`AttackViewModel.kt`，它通过调用 `Controller` 层的方法来执行业务操作。
- **Controller (控制器层)**
  - 封装具体的、可复用的业务逻辑和网络操作。
  - 主要文件：`HttpRequestController.kt`、`PasswordAttackController.kt`、`Downloader.kt`。

## 2. 环境要求

在开始开发或运行本项目前，请确保您的系统满足以下要求：

- **操作系统**：macOS, Windows, 或 Linux (需支持 Android Studio)。
- **Java 开发套件 (JDK)**：OpenJDK 11 或更高版本。
  ```bash
  # 在终端中检查当前 JDK 版本
  java -version
  ```
- **Android Studio**：推荐安装最新稳定版 [Android Studio](https://developer.android.com/studio)。
- **Gradle**：项目已包含 Gradle Wrapper (`gradlew`)，无需单独安装。

## 3. 安装与运行

### 3.1 开发环境搭建

1.  **克隆仓库**

    ```bash
    git clone https://github.com/ctkqiang/huo_jian_qiang_android.git
    cd huo_jian_qiang_android
    ```

    _请将上述 URL 替换为实际的项目仓库地址。_

2.  **使用 Android Studio 打开项目**

    - 启动 Android Studio。
    - 选择 `Open an existing Android Studio project`。
    - 导航到克隆的 `huo_jian_qiang_android` 目录并打开。

3.  **同步项目与依赖**
    Android Studio 会自动开始 Gradle 同步。如果失败，请尝试：
    - `File` -> `Sync Project with Gradle Files`。
    - `File` -> `Invalidate Caches / Restart...` -> `Invalidate and Restart`。

### 3.2 运行应用

1.  **连接设备**：通过 USB 连接已开启`开发者选项`和`USB调试`的 Android 设备，或在 Android Studio 中启动模拟器。
2.  **启动应用**：在 Android Studio 工具栏选择目标设备，点击绿色的 `Run ‘app’` 按钮。

## 4. 使用说明

### 4.1 功能导航

应用底部导航栏包含三个主要模块：`HTTP攻击`、`MYSQL攻击`（开发中）、`输出`。

### 4.2 HTTP 攻击操作指南

1.  **进入功能页**：点击底部导航栏的 `HTTP攻击`。
2.  **配置请求**：
    - **URL**：在 `输入URL` 框输入目标地址（如 `http://example.com/test`）。
    - **参数**：在 `请求参数` 框输入。GET 请求参数格式为 `key1=value1&key2=value2`；POST 请求通常也为相同格式（会被解析为表单数据）。
      _提示：可使用 `{{user}}` 和 `{{pass}}` 作为占位符，用于未来的密码爆破功能。_
    - **方法**：从下拉菜单中选择 `GET` 或 `POST`。
3.  **发起请求**：点击 `发送` 按钮。请求过程和结果将实时显示在 `输出` 页面。

### 4.3 MySQL 攻击

此功能尚在开发中。点击 `MYSQL攻击` 选项卡可查看相关占位信息。

### 4.4 查看输出

点击 `输出` 选项卡，可查看所有操作的日志、HTTP 响应详情及攻击结果。

### 4.5 示例展示

_(此处可放置应用界面截图或操作演示 GIF，以便直观展示)_

## 5. 开发指南

### 5.1 项目代码结构

```
app/src/main/java/xin/ctkqiang/huo_jian_qiang_android/
├── controller/                 # 业务逻辑控制器
│   ├── Downloader.kt           # 文件下载
│   ├── HttpRequestController.kt # HTTP 请求控制
│   └── PasswordAttackController.kt # 密码攻击控制
├── model/                      # 数据模型与 ViewModel
│   ├── AttackViewModel.kt      # 攻击相关 UI 状态管理
│   ├── HTTPRequest.kt          # HTTP 方法枚举
│   ├── PasswordAttackRequest.kt # 密码攻击配置
│   ├── RequestBody.kt          # 通用请求体
│   ├── RequestHeader.kt        # 请求头
│   └── ... (其他数据类)
├── pages/                      # Compose UI 页面
│   ├── HttpAttackPage.kt       # HTTP 攻击页面
│   ├── MySQLAttackPage.kt      # MySQL 攻击页面
│   └── OutputView.kt           # 输出日志页面
└── ui/theme/                   # 应用主题定义
    ├── Color.kt
    ├── Theme.kt
    └── ...
```

### 5.2 分支管理策略

推荐使用基于功能分支的工作流：

- **`main` 分支**：保护主干，存放可发布的稳定代码。
- **功能分支 (`feature/*`)**：从 `main` 创建，用于开发新功能。
- **修复分支 (`bugfix/*`)**：从 `main` 创建，用于修复问题。
- **流程**：通过 Pull Request (PR) 将功能/修复分支合并回 `main` 分支，并经过代码审查。

### 5.3 代码规范

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)。
- 遵循 [Jetpack Compose 最佳实践](https://developer.android.com/jetpack/compose/guidelines)。
- 使用 Android Studio 的 `Reformat Code` 功能保持格式统一。

### 5.4 测试

- **单元测试**：位于 `app/src/test/`。使用 JUnit 框架。
- **UI 测试**：位于 `app/src/androidTest/`。使用 AndroidX Test 和 Compose Testing 框架。
- **运行测试**：在 Android Studio 中右键点击测试文件或目录并选择 `Run ‘Tests in ...’`。

## 6. 部署与发布

### 6.1 生成发布版本

要将应用发布到应用商店（如 Google Play），需要生成签名的 Android App Bundle (AAB) 或 APK。

1.  **准备签名密钥**：如无现有密钥，可通过 Android Studio (`Build` -> `Generate Signed Bundle / APK...`) 创建新的密钥库（.jks 文件），并妥善保管。
2.  **配置 Gradle 签名信息**：在 `app/build.gradle.kts` 中配置 `signingConfigs`。
    ```kotlin
    android {
        signingConfigs {
            create("release") {
                storeFile = file("your_keystore.jks")
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
        buildTypes {
            release {
                ...
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    ```
    _切勿将密码直接硬编码在构建文件中，建议使用环境变量或 Gradle 属性文件管理。_
3.  **生成 Release AAB**：在 Android Studio 中再次选择 `Generate Signed Bundle / APK`，选择 `Android App Bundle` 和 `release` 变体，使用配置好的密钥进行签名。
4.  **发布**：将生成的 `.aab` 文件上传至 [Google Play Console](https://play.google.com/console) 或其他分发平台。

### 6.2 CI/CD 集成建议

可考虑集成 CI/CD 平台（如 GitHub Actions）以实现自动化：

- **CI 流程**：在 PR 创建时，自动运行代码检查、单元测试和 UI 测试。
- **CD 流程**：当代码合并到 `main` 分支时，自动构建 Release AAB 并发布到测试轨道。

## 7. 贡献指南

我们欢迎并感谢所有形式的贡献！请按以下步骤参与：

1.  **Fork 仓库**：点击 GitHub 页面的 `Fork` 按钮。
2.  **克隆你的 Fork**：
    ```bash
    git clone https://github.com/ctkqiang/huo_jian_qiang_android.git
    ```
3.  **创建功能分支**：
    ```bash
    git checkout -b feature/你的新功能
    ```
4.  **进行开发与提交**：遵循代码规范，并撰写清晰的提交信息。
5.  **推送分支**：
    ```bash
    git push origin feature/你的新功能
    ```
6.  **发起 Pull Request (PR)**：在你的 Fork 仓库页面发起 PR 到原项目的 `main` 分支。请在描述中详细说明变更内容。
7.  **参与审查**：根据维护者的反馈对代码进行修改。

## 8. 常见问题 (FAQ)

- **Q: Gradle 同步失败？**

  - A: 检查网络，尝试 `File -> Sync Project with Gradle Files` 或 `File -> Invalidate Caches / Restart...`。确保 JDK 版本符合要求。

- **Q: 应用在设备上崩溃？**

  - A: 在 Android Studio 的 `Logcat` 窗口中查看实时日志，通常会有红色的错误堆栈信息指示崩溃原因。

- **Q: HTTP 请求无响应？**
  - A: 检查 URL 是否正确、网络是否通畅，并查看 `输出` 页面中的错误日志。

---

# 项目状态：临时中止
