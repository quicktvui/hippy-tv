# hippy-tv

`hippy-tv` 是[quicktvui](https://github.com/quicktvui/quicktvui)框架中`Native`层运行库。它主要工作是对腾讯开源项目 *[*hippy*](https://github.com/Tencent/Hippy)* 进行针对电视设备的优化及二次扩展。
```mermaid
graph LR
A[quicktvui] -- run on --> B[quicktvui-sdk]
B -- depends on --> C[hippy-tv]
```

> ℹ️ [quicktvui](https://github.com/quicktvui/quicktvui)由三部分组成：[quicktvui-sdk](https://github.com/quicktvui/quicktvui-sdk)（安卓RuntimeSDK）、[hippy-tv](https://github.com/quicktvui/hippy-tv)（基于hippy的tv扩展）、[quicktvui](https://github.com/quicktvui/quicktvui)（Vue 快应用框架）。 本仓库为核心代码仓库，若需查看完整安卓端运行效果和实际应用示例，请前往 [quicktvui-sdk](https://github.com/quicktvui/quicktvui-sdk) 项目。

> ⚠️ 目前仅支持Android >= 17 版本设备

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
按需修改 `hippy-tv/android/sdk/gradle.properties` 文件中的发布相关配置：

```properties
EXT_JS_ENGINE_LIB=false
NDK_VERSION=21.4.7075529
INCLUDE_SUPPORT_UI=true
INCLUDE_ABI_X86=true
android.useAndroidX=false
#是否动态加载so，如果为true，则不会打包到aar中
ENABLE_SO_DOWNLOAD=true
##需要的so架构,ENABLE_SO_DOWNLOAD为false时生效
INCLUDE_ABI_ARM64_V8A=false
INCLUDE_ABI_ARMEABI=false
INCLUDE_ABI_ARMEABI_V7A=false
INCLUDE_ABI_X86_64=false
COMPILE_SDK_VERSION=29
PUBLISH_ARTIFACT_ID=hippy-tv
MIN_SDK_VERSION=17
ARCHIVES_BASE_NAME=hippy-tv
#打debug版本时，将此行注释掉
V8_RELEASE=x5-lite
V8_TAG=recommend
PUBLISH_VERSION_DESC=Hippy library for Android TV
VERSION_CODE=1
android.enableD8=true
TARGET_SDK_VERSION=29
SKIP_CMAKE_AND_NINJA=false
VERSION_NAME=0.2.0
PUBLISH_GIT_URL=https\://github.com/Tencent/Hippy
#maven groupId
PUBLISH_GROUP_ID=com.quicktvui
#版本提交号
COMMIT=""
#hippy-tv核心版本号，vue会在程序中读取判断部分api是否兼容
CORE_VERSION=0.2.0
INPUT_PREFERENCE=focus

```
⚠️ `debug`将`V8_RELEASE`注释掉，将`ENABLE_SO_DOWNLOAD`改为false,然后按需将各个SO架构的开关打开。
例如：
```properties
EXT_JS_ENGINE_LIB=false
NDK_VERSION=21.4.7075529
INCLUDE_SUPPORT_UI=true
INCLUDE_ABI_X86=true
android.useAndroidX=false
#是否动态加载so，如果为true，则不会打包到aar中
ENABLE_SO_DOWNLOAD=false
##需要的so架构,ENABLE_SO_DOWNLOAD为false时生效
INCLUDE_ABI_ARM64_V8A=true
INCLUDE_ABI_ARMEABI=true
INCLUDE_ABI_ARMEABI_V7A=true
INCLUDE_ABI_X86_64=true
COMPILE_SDK_VERSION=29
PUBLISH_ARTIFACT_ID=hippy-tv-debug
MIN_SDK_VERSION=17
ARCHIVES_BASE_NAME=hippy-tv-debug
#打debug版本时，将此行注释掉
#V8_RELEASE=x5-lite
V8_TAG=recommend
PUBLISH_VERSION_DESC=Hippy library for Android TV
VERSION_CODE=1
android.enableD8=true
TARGET_SDK_VERSION=29
SKIP_CMAKE_AND_NINJA=false
VERSION_NAME=0.2.0
PUBLISH_GIT_URL=https\://github.com/Tencent/Hippy
#maven groupId
PUBLISH_GROUP_ID=com.quicktvui
#版本提交号
COMMIT=""
#hippy-tv核心版本号，vue会在程序中读取判断部分api是否兼容
CORE_VERSION=0.2.0
INPUT_PREFERENCE=focus
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
- ✅ 列表组件重构，更方便使用 *(即将release)*
- ◻️ 鸿蒙平台适配（HarmonyOS）*进行中*
- ◻️ 列表滚动体验优化
- 🚧 ul组件支持Grid布局
- ◻️ 更多css样式支持
- ◻️ 支持svg图片
- ◻️ 焦点相关API优化
- ◻️ 组件扩展文档

### 中优先级 (Medium Priority)
- ◻️ 合并 Hippy 最新版本



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


### **Q&A（常见问题解答）**

#### **Q1：Hippy 相对于传统安卓开发有哪些优势？**
**A：**  
传统安卓开发存在以下问题：
- **开发效率低**：XML + Java/Kotlin 开发周期长，调试复杂。
- **无法动态更新**：原生代码无法实时修复线上问题。
- **升级部署慢**：需要将APK提交各大应用商店审核后上线，部署周期长。

Hippy 的优势：  
✅ **前端技术栈**：基于 JavaScript/TypeScript，复用 Web 生态（React/Vue），提升开发效率。  
✅ **动态化能力**：js动态语言特性，天然支持动态化，相较于插件化等native更新方式，无论开发还是部署、加载体验都很顺畅。  

---

#### **Q2：Hippy 的性能相较于安卓原生如何？**
**A：**
- **渲染性能**：Hippy 采用 **Native 渲染**（与 Flutter/React Native 类似），UI 组件最终映射为原生控件，性能接近原生。
- **瓶颈分析**：
  - **JS 层**：Vue/React 的虚拟 DOM 管理可能成为性能瓶颈（尤其在复杂页面）。
  - **通信开销**：JS 与 Native 的桥接通信（Bridge）在频繁交互时可能有延迟。
  - **启动速度**：相较于原生hippy增加了js引擎启动的时间成本（大部分电视在1S左右),引擎启动之后时间主要与js程序本身，dom少的情况下，与原生相差不大。
- **优化建议**：
  - 减少不必要的 DOM 节点（如扁平化组件结构）。
  - 性能敏感模块（如动画、复杂且数量庞大的UI组件）可通过原生实现。
  - 减少频繁的 JS-Native 通信，复杂的通信场景考虑整体用原生实现。

**结论**：在 DOM 较少的场景下，Hippy 性能与原生基本持平；复杂场景需针对性优化，或者将复杂逻辑用原生实现，性能也能追平原生。

---

#### **Q3：Hippy 是否支持热更新？**
**A：**  
✅ **支持**！Hippy 的 JS Bundle 可通过 CDN 或服务端动态下发，实现热更新（无需应用商店审核）。

---

#### **Q4：Hippy 适合哪些业务场景？**
**A：**
- **高频迭代功能**：如活动页、运营弹窗。
- **动态化要求高**：需快速修复线上问题的核心模块。
- **原生开发增加动态化能力**：使原生增加动态化能力，减少发版频率。

---

#### **Q5：可否原生和hippy结合的方式开发？**
**A：** 可以，这是目前推荐的开发方式。例如将复杂、对性能要求较高的页面、组件利用原生实现，通过js去组织加载，这样既能保证性能，又有了动态化的能力。
目前常见俩种结合的开发方式：
1. JS为入口： js做为主入口，将复杂的ui组件、模块利用Native实现并提供给hippy调用。
2. Native为入口：可以将需要动态化的ui模块做为一个`jsView`当做一个标准`View`嵌入到Activity当中。

--- 

如需更多问题解答，请参考 [Hippy 官方文档](https://hippyjs.org/) 或提交 Issue。


## 贡献

> ℹ️ 本项目已成功应用于多个实际 TV 应用中，但整体仍处于快速演进阶段，使用时请注意版本变化。

> ⚠️ 欢迎你 Fork 本项目，自由修改并根据自身需求进行定制开发。

如你有问题或建议，欢迎通过 Issue 与我们沟通反馈。

---

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源发布。
