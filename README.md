# 我们的记录

一个用来记录两个人日常的小 App：随手拍一张照片，自动带上时间和地点，像社交动态一样排成信息流。

## 界面预览

| 信息流 | 编辑发布 | 地图选点 |
| --- | --- | --- |
| <img src="docs/feed.png" width="240" /> | <img src="docs/composer.png" width="240" /> | <img src="docs/map.png" width="240" /> |

## 现在能做什么

- 📷 **拍照**：CameraX 相机预览 + 快门；只有前置/只有后置摄像头的设备会自动适配
- 📍 **地点**：进入编辑页自动定位，先显示坐标再解析地址；可以点地点进地图拖动微调，也能手动改名
- 📝 **发布**：照片 + 配文 + 时间 + 地点写进本地数据库，按时间倒序展示；新记录发布后自动回到列表顶部
- ❤️ **信息流**：Facebook 风格卡片，支持点赞、删除（照片文件一并删除）
- 🔒 **纯本地**：不需要登录；拒绝定位权限也照样能发；误触返回会弹「放弃这条记录？」二次确认

## 技术栈

| 模块 | 选择 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单模块 MVVM，手写 `AppContainer` 做依赖注入 |
| 数据库 | Room 2.8（KSP 注解处理） |
| 相机 | CameraX 1.6（core/camera2/lifecycle/view） |
| 定位 | `FusedLocationProviderClient`，无 GMS 时自动退到系统 `LocationManager` |
| 地址解析 | 系统 `Geocoder`，失败后回退到免 Key 的 [Photon](https://photon.komoot.io)（OSM） |
| 地图 | osmdroid + OpenStreetMap.de 瓦片（**不需要 API Key**） |
| 图片 | Coil Compose |
| 导航 | navigation-compose |

## 目录结构

```
app/src/main/java/com/example/myapplication/
├── MyApplication.kt          # Application + AppContainer（数据库/仓库/定位/照片存储）
├── MainActivity.kt
├── data/
│   ├── Post.kt               # Room 实体：照片、配文、地点、时间、点赞
│   ├── PostDao.kt
│   ├── AppDatabase.kt
│   ├── PostRepository.kt
│   ├── PhotoStore.kt         # 草稿 → 正式照片的文件管理
│   ├── PlaceSelection.kt
│   └── location/             # LocationClient / PlaceResolver + 两个接口（方便测试）
└── ui/
    ├── AppNavHost.kt         # feed → capture → compose → pickPlace
    ├── feed/                 # 信息流
    ├── camera/               # 相机
    ├── composer/             # 编辑发布 + 地点行
    ├── location/             # osmdroid 选点
    └── components/           # PostCard、权限工具
```

## 运行

### Android Studio
直接点 Run 即可（`local.properties` 里要指向你的 SDK）。

### 命令行
本项目的 Gradle 守护进程要求 **JDK 25**（见 `gradle/gradle-daemon-jvm.properties`），Android Studio 自带的 JBR 就是 25：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export GRADLE_USER_HOME="$PWD/.gradle-user-home"   # 可选：把缓存放进项目里
./gradlew :app:assembleDebug
```

APK 产物：`app/build/outputs/apk/debug/app-debug.apk`

## 测试

```bash
./gradlew :app:testDebugUnitTest         # JVM 单元测试（仓库、ViewModel、照片存储）
./gradlew :app:connectedDebugAndroidTest # 需要连上模拟器/真机（Room DAO）
```

## 在模拟器上手动验证

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.example.myapplication android.permission.CAMERA
adb shell pm grant com.example.myapplication android.permission.ACCESS_FINE_LOCATION
adb emu geo fix 116.3975 39.9087        # 注意顺序是 经度 纬度
adb shell am start -n com.example.myapplication/.MainActivity
```

> - 模拟器用的是虚拟相机，拍出来是合成画面。
> - 如果模拟器网络异常，检查 `adb shell settings get global http_proxy`：某些环境会给模拟器塞一个 `127.0.0.1:xxxx` 代理，导致瓦片和地址解析都失败，可用 `adb shell settings put global http_proxy :0` 清掉。
> - 地图瓦片走 OpenStreetMap.de；地址解析在系统 Geocoder 不可用时会调 Photon。两者都失败时地点显示为「纬度, 经度」，点地点右边的铅笔可以手动改名。
> - 国产 ROM 如果没有 Google 服务，定位会自动改用系统 LocationManager（GPS/网络定位），不需要 GMS。

## 已知限制（v1 范围）

- 数据只存在本机，没有账号和云端同步，所以还做不到两台手机共享同一条时间线
- 没有评论、滤镜、视频
- 地图依赖网络瓦片；完全离线时只能按坐标选点，文字仍可手动填写

## 后续可以加的功能

- 两个人配对 + 云端同步（需要后端和登录）
- 评论 / 表情回应
- 按地点聚合的"回忆地图"
- 相册时间线、纪念日提醒、桌面小组件
