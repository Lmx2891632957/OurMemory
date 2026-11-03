# 更新日志

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.0.0] - 2026-11-03

### Added

- 拍照记录：CameraX 相机预览与快门，自动适配只有前置或只有后置摄像头的设备
- 地点：进入编辑页自动定位，先显示坐标再异步解析地址（系统 Geocoder，失败回退 Photon）
- 地图选点：osmdroid + OpenStreetMap.de 瓦片（免 API Key），支持拖动取点与手动改名
- 发布与信息流：Room 本地存储、Facebook 风格卡片、相对时间、点赞、删除（照片文件一并删除）
- 权限处理：相机/定位被拒绝时的引导，未授权定位也能发布
- 无 Google 服务的机型自动回退到系统 `LocationManager` 定位
- 单元测试（仓库/ViewModel/照片存储）与 Room 仪器测试
