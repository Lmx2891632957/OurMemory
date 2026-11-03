# 新对话交接提示词

> 用法：新开一个对话，把下面「---」之间的内容整段贴进去。

---

你在继续开发「我们的记录」——一个**本地优先**的情侣生活记录 Android App（拍照 → 定位 → 发布 → 信息流）。项目已有可用版本 v1.1.1，用户已在真机验证通过。

## 一、先做的事（不要跳过）

1. `git status`、`git log --oneline -10`，确认工作区干净、在 `main` 分支
2. 读 `AGENTS.md`（开发与版本管理规则，**必须遵守**）
3. 读 `docs/prd.md`、`docs/ui-flows.md`、`docs/data-model.md`（产品与设计已确认）
4. 读 `README.md` 了解构建方式

## 二、项目与仓库

- 本地目录：`/Users/Zhuanz/AndroidStudioProjects/MyApplication`
- GitHub：`https://github.com/Lmx2891632957/OurMemory`（`main` 已推送）
- 远端走 **SSH**（`git@github.com:...`），HTTPS 在本机被墙；`core.sshCommand` 已配好指向 `.git-ssh/id_ed25519`
- 推送前先 `git pull --rebase origin main`

## 三、当前状态（v1.1.1）

已完成并验证：CameraX 拍照、高德定位与逆地理、地图选点（独立 Activity，含「附近地点」列表）、发布、信息流、点赞、删除、隐私合规页、本地持久化。

单机闭环已跑通，**现在的任务是按设计文档把体验做厚**，不是补地基。

## 四、技术栈与版本（不要随意升级）

- AGP **9.5.0** + Gradle **9.6.0** + Kotlin 2.2.10（AGP 内置 Kotlin，**不要再加 `org.jetbrains.kotlin.android` 插件**）+ KSP 2.3.13
- compileSdk / targetSdk **37**，minSdk 24
- Compose BOM 2026.02.01、Room 2.8.5、CameraX 1.6.2、navigation-compose 2.10.2
- 高德：`com.amap.api:3dmap:10.0.600` + `app/libs/amap-search-9.7.1-slim.jar`
  - 为什么用 slim jar：10.0.600 与 search 9.7.1 重复打包 `AMapUtilCoreApi` / `NetProxy` 两个类，slim jar 剥掉了这两个
  - **不要换回 9.8.3**：它的 native 库在 Android 16 上销毁 GL 引擎会 SIGABRT
  - 任何高德版本变更都要重新检查重复类，并真机验证「选点 → 使用此地点 → 返回」不崩溃

## 五、环境约定（照抄，否则会卡住）

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export GRADLE_USER_HOME="$PWD/.gradle-user-home"
export ANDROID_USER_HOME="$PWD/.android-home"

./gradlew :app:assembleDebug :app:testDebugUnitTest      # 提交前必须通过
```

- 沙箱限制：`~/.gradle`、`~/Library/Android/sdk` **不可写**；不要安装/更新 SDK 组件
- 高德 Key 在 `local.properties` 的 `AMAP_KEY`（**不入库、不要打印、不要提交**）
- 产物：`dist/*.apk`（已 gitignore），装手机用 `adb install -r`
- 模拟器：用户的 AVD 可能被占用；可以自己起无窗口模拟器（`.android-sdk` + `.avd`，需把 `HOME` 重定向到项目内目录并传 `-sysdir`）；**真机验证以用户为准**

## 六、已知坑（都是踩过的）

1. **高德 MapView 生命周期**：不能在 Compose 里销毁，否则 GL 线程二次 `nativeDestroy` → SIGABRT。选点页是独立 `LocationPickerActivity`，`onResume/onPause/onDestroy` 只能在 Activity 里调。
2. **逆地理必须 `extensions = "all"`**，否则返回里没有 POI，只能拿到街道级 `formatAddress`。
3. Kotlin 编译守护进程在沙箱会报 `FileSystemException`（无害，会自动回退进程内编译）。
4. 不要提交：`build/`、`.gradle-user-home/`、`.android-sdk/`、`.android-home/`、`.avd/`、`.emu-home/`、`dist/`、`local.properties`、`.idea/`。
5. 桌面验证：`adb exec-out screencap -p > /tmp/x.png` 自查 UI；坐标换算按 1080×2400 真机分辨率。

## 七、产品决策（已与用户确认，不要推翻）

- **单机优先**，数据模型预留同步字段（`remoteId`/`authorId`/`updatedAt`/`deletedAt`/`syncState`），**暂不做账号与后端**
- **明确不做**：公开社交、视频滤镜、账号体系、**编辑已发布记录**（发错了删除重发）
- **P0（现在做）**：多图记录（≤9 张）、详情页（只读 + 删除）
- **P1（以后再）**：相册、地图回忆、心情、搜索、导出备份

## 八、下一步任务（严格按顺序，每步独立提交）

1. **数据层迁移**：Room v1 → v2
   - `posts` 加同步字段；新建 `post_photos` 表（`postId` 外键 + 级联删除 + 索引 + `position`）
   - 旧数据：`photoPath` 迁成 `post_photos` 一条（position=0），`updatedAt` 用 `createdAt` 回填
   - 写 `MigrationTestHelper` 测试，**禁止 destructive migration**
2. **Repository**：`create(drafts: List<File>, caption, place, mood?)`；`observePosts(): Flow<List<PostWithPhotos>>`；`PhotoStore` 支持多图；删除时清理全部照片文件
3. **发布页**：多图添加/删除/排序、九宫格预览、单图/多图布局
4. **信息流**：多图自适应排版（1/2/3+）、按天分组且分组头吸顶
5. **详情页**：大图左右滑、完整文字、地点（点击高亮地图点）、右上角删除（二次确认）；**不做编辑**
6. 每步：构建 + 测试通过 → 小步提交（Conventional Commits，中文主题，正文写验证方式）→ 装机验证 → 更新 `CHANGELOG.md`

## 九、工作方式

- 先看文档再写代码；设计文档与需求冲突时**先问用户**，不要自行扩大范围
- 小步提交，一个提交只做一件事；收尾报告 commit hash 与改动文件
- 涉及 UI 的改动必须截图自查；涉及数据库的改动必须有迁移测试
- 用户偏好：中文沟通、先结论后原因、喜欢先看方案再动手、主要在真机验证（国产机，无 GMS）

开始前，先向我复述你理解的 P0 范围和第一步要做的事，再动手。

---
