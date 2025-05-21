# hippy-tv

`hippy-tv` 是[quicktvui](https://github.com/quicktvui/quicktvui)框架中`Native`层运行库。它主要工作是对腾讯开源项目 *[*hippy*](https://github.com/Tencent/Hippy)* 进行针对电视端程序的优化及二次扩展。
```mermaid
graph LR
A[quicktvui] -- run on --> B[quicktvui-sdk]
B -- depends on --> C[hippy-tv]
```

> ⚠️ 本项目由三部分组成：[quicktvui-sdk](https://github.com/quicktvui/quicktvui-sdk)（安卓RuntimeSDK）、[hippy-tv](https://github.com/quicktvui/hippy-tv)（基于hippy的tv扩展）、[quicktvui](https://github.com/quicktvui/quicktvui)（Vue 快应用框架）。本仓库为核心代码仓库，若需查看完整安卓端运行效果和实际应用示例，请前往 [quicktvui-sdk](https://github.com/quicktvui/quicktvui-sdk) 项目。

> ⚠️ 目前仅支持安卓TV

---

## 功能
* ✏️ **修改 hippy 部分 API 行为**：对底层渲染与事件响应机制进行了适配优化。
* 🎯 **引入焦点系统**：支持遥控器方向键导航、焦点记忆、边界控制等，增强 TV 操作体验。
* 🧾 **更快的列表组件**：高性能虚拟列表，支持遥控分页滚动和动态渲染。
* 🧩 **完善各类组件模块**：补充瀑布流等 TV 常用UI组件

> *📌 注意！本项目基于 Hippy **`v2.8.2`** 版本 Fork，并在此基础上进行 TV 场景定制与功能增强。目前与`hippy`偏离较大，暂不考虑合并新版hippy后续将会在大版本变动时与hippy最新版本合并。*
---


## 快速开始
在 Android Studio 中编译
1. 打开 Android Studio：
   * 启动 Android Studio
   * 选择 "Open an Existing Project"

2. 选择项目目录：
   * 导航到 hippy-tv 项目的根目录
   * 选择 build.gradle 或 settings.gradle 文件所在目录
   * 点击 "Open"

3. 同步Gradle：
   * Android Studio 会自动开始同步 Gradle
   * 如果没有自动同步，点击工具栏的 "Sync Project with Gradle Files" 按钮

4. 编译项目：

   * 同步完成后，打开右侧的 Gradle 面板（View → Tool Windows → Gradle）
   * 导航到项目 → Tasks → build
   * 双击 "build" 任务执行编译
   * 或者，在终端运行：

```bash
./gradlew android-sdk:build
```

---

## 发布
### 第一步：配置本地账号信息
在项目的 `hippy-tv/android/sdk/local.properties` 文件中添加您自己仓库及账号信息：
```properties
MAVEN_REPO_URL_RELEASE=your-repo-url-release
MAVEN_REPO_URL_SNAPSHOT=your-repo-url-snapshot
MAVEN_USERNAME=your-username
MAVEN_PASSWORD=your-password
```
### 第二步：配置发布信息
修改 `hippy-tv/android/sdk/gradle.properties` 文件中的发布相关配置：

```properties
PUBLISH_ARTIFACT_ID=hippy-tv
ARCHIVES_BASE_NAME=hippy-tv
VERSION_CODE=1
VERSION_NAME=0.2.0
PUBLISH_GROUP_ID=com.quicktvui

```
### 第三步：执行发布命令
在项目根目录下执行以下 Gradle 命令：
```bash
./gradlew android-sdk:publish
```
或者通过 Android Studio 的 Gradle 面板：
1. 打开右侧 Gradle 面板
2. 导航到 `hippy-tv → android-sdk → Tasks → publishing`
3. 双击 publish 任务


---

## TODO

### 高优先级 (High Priority)
- ✅ FastList列表组件重构，更方便使用 *(即将release)*
- ◻️ 列表滚动体验优化
- ul组件支持Grid布局
- 🚧 更多css样式支持
- ◻️ 支持svg图片
- ◻️ block焦点相关属性优化

### 中优先级 (Medium Priority)
- ◻️ 文档自动化生成
- ◻️ 合并 Hippy 最新版本
### 规划中 (Planned)
- ◻️ 鸿蒙（HarmonyOS）初步适配

## 文档 Documentation

敬请期待，或参考代码中的注释和示例工程。

---

## 版本说明

本项目采用**三段式版本命名规范**（即 `主版本号.次版本号.修订号`），遵循[语义化版本](https://semver.org/)原则：

- **`主版本号 (MAJOR)`**：重大变更或不兼容的API修改
- **`次版本号 (MINOR)`**：新增功能且向下兼容
- **`修订号 (PATCH)`**：问题修复或微小改进，完全兼容

### 版本兼容性指南
| 版本范围        | 兼容性说明                     |
|-----------------|------------------------------|
| `1.x.x`         | 同一主版本内保证API兼容性      |
| `x.2.x` → `x.3.x` | 可安全升级，包含新功能        |
| `x.x.1` → `x.x.2` | 强烈建议升级，仅含错误修复    |


## 贡献

> ℹ️ 本项目已成功应用于多个实际 TV 应用中，但整体仍处于快速演进阶段，使用时请注意版本变化。

> ⚠️ 欢迎你 Fork 本项目，自由修改并根据自身需求进行定制开发。

如你有问题或建议，欢迎通过 Issue 与我们沟通反馈。

---

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源发布。
