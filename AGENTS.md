# AGENTS.md

本文件是本仓库的开发约定。**人（包括未来的你）和 AI 代理都必须遵守**，尤其是第 1～6 节。
规则有变化时，先改这个文件再改代码。

## 1. 第一原则：所有改动都必须进版本库

- 仓库根目录即项目根目录，禁止在子目录里另建 git 仓库。
- 每完成一个**可验证**的改动就提交一次，不允许"攒到最后一起提交"。
- 收工时 `git status` 必须是干净的；只有被 `.gitignore` 明确忽略的文件可以留在工作区。
- 没提交的代码等于不存在。不要用"我本地改好了"代替提交。

## 2. 提交前必须通过的检查

本仓库的 Gradle 守护进程要求 **JDK 25**（见 `gradle/gradle-daemon-jvm.properties`），统一用 Android Studio 自带的 JBR：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export GRADLE_USER_HOME="$PWD/.gradle-user-home"
export ANDROID_USER_HOME="$PWD/.android-home"

./gradlew :app:assembleDebug :app:testDebugUnitTest
```

- 有设备/模拟器时，涉及数据层的改动还要跑 `./gradlew :app:connectedDebugAndroidTest`。
- **构建失败、测试失败一律不得提交。**
- 涉及相机、定位、地图等真机能力时，必须在模拟器或真机上走一遍主流程，并把验证结论写进提交说明。

## 3. 提交信息规范（Conventional Commits）

格式：`<type>(<scope>): <主题>`

- `type`：`feat` | `fix` | `refactor` | `perf` | `test` | `docs` | `chore` | `build` | `ci` | `revert`
- `scope`（可选）：`camera` / `composer` / `feed` / `location` / `data` / `ui` / `build` / `docs`
- 主题行用中文、动词开头、不超过 50 字、句尾不加句号。
- 正文写「为什么改、影响范围、怎么验证」，不要复述 diff。
- **一个提交只做一件事**：重构与功能、格式化与逻辑改动必须分两次提交。
- 破坏性变更用 `feat!:` 或在正文加 `BREAKING CHANGE:` 说明。

示例：

```
fix(location): 无 GMS 机型定位失败时回退到系统 LocationManager

fused provider 在无 Google 服务的机器上抛异常，导致地点永远为空。
改为依次尝试 fused → 系统最后已知位置 → 系统单次定位。
验证：模拟器拍照发布流程正常；服务不可用时能退化到手动选点。
```

## 4. 分支模型

- `main` 随时保持可构建、可运行。
- 功能/修复在分支上做：`feat/<简述>`、`fix/<简述>`、`chore/<简述>`、`docs/<简述>`。
- 合并回 `main` 用 `git merge --no-ff`（保留分支上下文），合并后删除分支。
- 分支名用小写英文和连字符，不用中文、空格。
- 纯文档、改文案这类小改动可以直接提交到 `main`，但同样要通过第 2 节的检查。

## 5. 版本号与发版

- 版本号遵循 SemVer，写在 `app/build.gradle.kts` 的 `versionName`。
- `versionCode` **每次对外发包必须 +1**；纯本地调试的提交不用改。
- 发版必须打附注 tag，且 tag 与 `versionName` 一致：

```bash
git tag -a v1.1.0 -m "v1.1.0"
git push origin main --tags
```

- 已推送的 tag 禁止移动或删除。
- 发版前更新 `CHANGELOG.md`，并在真机上跑一遍主流程（拍照 → 定位 → 发布 → 重启查看）。
- 发版提交格式：`chore(release): vX.Y.Z`。

## 6. 绝对禁止

- 提交构建产物或本地环境：`build/`、`.gradle/`、`.gradle-user-home/`、`.android-sdk/`、`.android-home/`、`.avd/`、`.kotlin/`、`dist/`、`local.properties`、`.idea/`、`*.apk`、`*.aab`、`*.jks`、`*.keystore`。
- 提交密钥、token、API Key；需要 Key 的东西一律放 `local.properties`（已在忽略列表）或环境变量。
- 对 `main` 执行 `git push --force`；对已推送的提交做 `--amend` 或 `rebase`。
- 未经确认的破坏性操作：`git reset --hard`、`git clean -fdx`、删除分支、删除 tag、`filter-branch`。
- 通过改 `.gitignore` 来掩盖误提交（正确做法是移除误提交的文件并说明原因）。
- 把多个互不相关的改动塞进同一个提交。

## 7. 对 AI 代理的额外要求

**开工前**

- 先看 `git status --short` 和 `git log --oneline -5`，确认工作区状态。
- 发现未提交的改动时先确认归属，不要直接覆盖或提交别人的 WIP。

**工作中**

- 按计划拆步骤，**每完成一步立刻提交**；小步提交优于大提交。
- 行为有变化时，同步更新 `README.md`，发版时更新 `CHANGELOG.md`。
- 提交说明里写清验证方式（命令或手动步骤）。

**收工前**

- 保证 `git status` 干净；确实没做完的，提交成 `wip:` 前缀并说明剩余工作。
- 回复中给出本次的 commit hash 和改动文件清单。

## 8. 提交信息模板

```
<type>(<scope>): <主题>

<为什么改 / 改了什么 / 影响范围>
验证：<执行过的命令或手动步骤>
```

## 9. 目录与产物约定

| 路径 | 用途 | 是否入库 |
| --- | --- | --- |
| `app/src/{main,test,androidTest}` | 源码与测试 | ✅ |
| `docs/` | 截图等文档素材（单文件尽量 < 2MB） | ✅ |
| `dist/` | 本地打出的 APK | ❌ |
| `.gradle-user-home/`、`.android-sdk/`、`.avd/` | 本机构建/模拟器环境，可随时删除 | ❌ |

## 10. 远端（还没有 remote 时）

```bash
git remote add origin <仓库地址>
git push -u origin main
```

之后每次推送前先 `git pull --rebase origin main`，保持线性历史。
