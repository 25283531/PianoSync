# PianoSync（儿童学琴增强版）

PianoSync 是一款通过 USB MIDI 连接电子琴/电钢琴，以"下落音符"方式跟弹练习的 Android 应用。

> 本项目基于 [https://github.com/clquwu/PianoSync](https://github.com/clquwu/PianoSync) 进行二次开发与汉化，面向儿童学琴场景增加了唱名标注、五线谱视窗和"等待正确音符"等功能，并提供简体中文界面。

---

## ✨ 本分支新增内容

### 1. 琴键唱名标注（1 2 3 4 5 6 7）
- 在每个白键上显示大号唱名数字（C 大调：C=1、D=2 … B=7），大幅降低儿童识键门槛。
- 高/低八度用数字上/下方的**纵向圆点**表示，符合简谱规范。
- 可在「设置 → 显示唱名（1-7）」中开关，设置自动持久化。
- 相关实现：`MusicTheory.kt`、`EnhancedWhiteKey`、`OctaveDots`。

### 2. 顶部五线谱识谱视窗
- 播放页顶部新增大谱表（高音谱号 + 低音谱号），音符随乐曲横向滚动，将"游戏下落"与"专业识谱"结合。
- 当前正在弹奏的音符**高亮显示**：右手红色、左手蓝色，并带发光效果；未弹音符为白色，已弹音符为灰色。
- 播放指针位于屏幕中央，滚动速度与乐曲 BPM 同步，形成「视谱 → 下落方块 → 数字键位 → 真实电子琴」的学习闭环。
- 纯 Jetpack Compose Canvas 自绘，无额外 WebView/JS 依赖，兼容 Compose 1.6。
- 相关实现：`StaffNotationView.kt`、`MusicTheory.kt`。

### 3. "等待正确音符"练习模式
- 开启后，乐曲播放到应弹音符时，若用户没有弹对，**乐曲和乐谱会自动暂停**；弹对正确音符（和弦需全部按对）后才继续播放。
- 等待时在播放线附近显示红色提示「请弹奏正确的音符…」。
- 特别适合儿童逐音跟练，不再因手忙脚乱而错过整段。
- 可在「设置 → 等待正确音符」中开关，默认关闭。
- 相关实现：`MidiPlaybackManager.pauseForWait()/resumeFromWait()`、`NoteFallVisualizer` 判定逻辑。

### 4. 简体中文化
- 新增 `values-zh-rCN/strings.xml`，覆盖全部界面文案，音乐术语采用惯用译法。
- 应用会**自动跟随系统语言**：简体中文系统显示中文，其他语言回退英文（原有法语资源保留）。

### 5. CI / 构建
- 新增 GitHub Actions 工作流 `.github/workflows/android.yml`，每次推送/PR 自动编译 Debug 与 Release APK 并作为 Artifact 上传。
- Release 签名支持通过环境变量或 `keystore.properties` 配置；未配置时自动回退 Debug 签名，保证始终可产出可安装包。

---

## 📱 功能截图

| 主菜单 | 设置 | 进度记录 |
|-----------|---------------|----------------|
| ![Main Menu](https://i.postimg.cc/yx19pq3B/Screenshot-20250529-170710.png) | ![Practice Mode](https://i.postimg.cc/cCF3VDf3/Screenshot-20250529-170723.png) | ![Song Selection](https://i.postimg.cc/J7xjDhSh/Screenshot-20250529-170749.png) |

| 无 UI 钢琴界面 | 带 UI 钢琴界面 |
|----------------|----------|
| ![Piano Interface](https://i.postimg.cc/Wbfr8HNL/Screenshot-20250529-170814.png) | ![Settings](https://i.postimg.cc/VLNnj8cW/Screenshot-20250529-170819.png) |

---

## 🛠️ 开发与构建

### 环境要求
- Android Studio
- JDK 17
- Kotlin（版本以 `gradle/libs.versions.toml` 为准）

### 本地运行
1. Clone 仓库并用 Android Studio 打开；
2. 等待 Gradle Sync 完成；
3. 连接支持 MIDI 的电子琴（或使用屏幕虚拟键盘），点击 Run。

### 命令行构建
```bash
# Debug APK
./gradlew :app:assembleDebug
# Release APK（未配置签名时使用 debug 密钥）
./gradlew :app:assembleRelease
```
产物位于 `app/build/outputs/apk/`。

### Release 正式签名（可选）
在项目根目录创建 `keystore.properties`（已被 `.gitignore` 忽略）：
```properties
storeFile=../your-release.keystore
storePassword=你的密钥库密码
keyAlias=你的别名
keyPassword=你的密钥密码
```
或在 CI 中配置 `KEYSTORE_BASE64`、`KEYSTORE_STORE_PASSWORD`、`KEYSTORE_KEY_ALIAS`、`KEYSTORE_KEY_PASSWORD` Secrets。

---

## 🤝 参与贡献

感谢你考虑为 PianoSync 做贡献！

### 提交 Issue / Bug 报告
- 先搜索是否已有相同问题；
- 提供清晰标题、复现步骤、设备与 MIDI 设备型号、相关日志。

### 开发流程
1. Fork 仓库；
2. 基于 `dev-android`（或 `main`）创建特性分支，例如 `feature/add-xxx` 或 `bugfix/fix-xxx`；
3. 完成改动并自测；
4. 提交 Pull Request，说明改动内容与动机，UI 改动请附上截图。

### 代码规范
- 遵循 Kotlin 官方编码规范与项目现有风格；
- 使用有意义的命名，保持简洁；
- 优先复用现有模块与组件，保持架构一致。

### 沟通方式
- 上游邮箱：raphaelboullaylefur@proton.me
- 上游 Discord：clarityhs

---

## 📄 说明

本项目在原项目基础上进行二次开发，遵循其开源协议。原项目版权归原作者所有，本分支的增强功能版权归本分支贡献者所有。
