# 更新日志

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added

- 发布页支持多图：最多 9 张，可继续拍、删除、左右调整顺序，1 张大图 / 2 张并排 / 3 张以上九宫格
- 信息流按天分组（今天 / 昨天 / M月d日），分组头滚动吸顶；卡片按 1/2/3+ 张自适应排版，正文最多 3 行

### Changed

- 数据库升级到 v2：新增 `post_photos` 表（支持多图）与同步预留字段
  （`remoteId` / `authorId` / `deviceId` / `coordSystem` / `updatedAt` / `deletedAt` / `syncState`）。
  v1 记录自动迁移，旧照片变成第一张，不丢数据

## [1.1.1] - 2026-11-03

### Fixed

- 修复地图选点后「使用此地点」闪退：高德地图 SDK 由 9.8.3 升级到 10.0.600，
  9.8.3 的 native 库在 Android 16 上销毁 GL 引擎会二次执行 nativeDestroy 导致 SIGABRT
- 修复选点页拿不到「附近地点」：逆地理请求漏了 `extensions=all`，返回里没有 POI

### Changed

- 选点页检索半径 200m → 500m，按距离排序取最近 20 个 POI，标签取最近的一个
- 地图默认缩放 17 → 18

### Notes

- 10.0.600 与搜索 SDK 9.7.1 重复打包两个类，使用剥离后的本地 jar
  `app/libs/amap-search-9.7.1-slim.jar`

## [1.1.0] - 2026-11-03

### Added

- 接入高德开放平台：定位、逆地理编码、地图全部换成高德 SDK，国内不依赖 Google 服务
- 首次启动的隐私合规页：同意后才初始化高德 SDK（官方强制要求）
- 地图选点页支持「附近地点」快捷选择（来自逆地理返回的 POI）
- 定位一次调用即可返回地址与 POI 名称，省掉一次逆地理请求

### Changed

- 逆地理改为高德 `GeocodeSearch`，返回中文地址 + 附近 POI
- 地图坐标系统一为 GCJ-02，与高德底图对齐

### Removed

- 移除 osmdroid 与 OpenStreetMap.de 瓦片方案
- 移除 Photon 逆地理兜底（改由高德统一提供）

### Notes

- 需在 `local.properties` 配置 `AMAP_KEY`，Key 不入库
- v1.0.0 的旧记录是 WGS-84 坐标，换到 GCJ-02 底图后会有几百米偏移；测试数据建议清空

## [1.0.0] - 2026-11-03

### Added

- 拍照记录：CameraX 相机预览与快门，自动适配只有前置或只有后置摄像头的设备
- 地点：进入编辑页自动定位，先显示坐标再异步解析地址（系统 Geocoder，失败回退 Photon）
- 地图选点：osmdroid + OpenStreetMap.de 瓦片（免 API Key），支持拖动取点与手动改名
- 发布与信息流：Room 本地存储、Facebook 风格卡片、相对时间、点赞、删除（照片文件一并删除）
- 权限处理：相机/定位被拒绝时的引导，未授权定位也能发布
- 无 Google 服务的机型自动回退到系统 `LocationManager` 定位
- 单元测试（仓库/ViewModel/照片存储）与 Room 仪器测试
