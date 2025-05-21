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
