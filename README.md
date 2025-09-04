# NetworkMonitorDemo

一键 GitHub Actions 在线打包 APK：提交后到 **Actions → Build APK → Run workflow**，完成后在 **Artifacts** 下载 `app-debug.apk`。

- 生产域名：`https://fans.91haoka.cn`
- 测试域名：`https://fans.gantanhaokeji.top`
- 支持自定义域名与推送周期（3–3600 秒）
- 日志含中文说明、ISO8601 时间、设备标识（IMEI*或*ANDROID_ID）、Wi-Fi/蜂窝指标（尽力获取 RSRP/RSRQ/SINR）

> 如果你之前报 `chmod +x gradlew: No such file or directory`，使用此仓库结构与工作流即可解决（不依赖 gradle wrapper）。


> 已修复：Room 需要 schemaDirectory，已在 `app/build.gradle.kts` 配置并添加 `app/schemas/` 目录。
