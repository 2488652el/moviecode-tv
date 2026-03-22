# MovieCode TV - Android TV 媒体中心

<div align="center">
  
**🍎 Apple TV 风格的 Android TV 媒体中心应用**

[![Android TV](https://img.shields.io/badge/Android%20TV-34-green.svg)](https://developer.android.com/tv)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-1.5-orange.svg)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.1.0-blue.svg)](https://github.com/2488652el/moviecode-tv/releases)

**[Windows 客户端 →](https://github.com/2488652el/MovieCode-Windows)** | **[Android TV 客户端](#)**

</div>

---

## ✨ 功能特性

| 功能 | 描述 |
|------|------|
| 🍎 **Apple TV 风格 UI** | 深色主题，流畅海报墙动画 |
| 📺 **TV 遥控器适配** | D-pad 导航，焦点管理优化 |
| 🎬 **电影/剧集/动漫** | TMDB 元数据同步 (7天缓存) |
| 🔍 **实时搜索** | 支持电影、电视剧搜索 |
| ▶️ **视频播放** | ExoPlayer (Media3) 支持，8级倍速 |
| 📁 **本地扫描** | 自动识别本地媒体文件 |
| ⚙️ **NAS 连接** | 设置页面支持 NAS 配置 |
| 🖼️ **精美海报** | 自动从 TMDB 加载高清海报 |
| ⭐ **个性化推荐** | 基于观看历史推荐相似内容 |
| 👨‍👩‍👧 **家长控制** | PIN 码保护、年龄分级过滤 |
| 📥 **离线下载** | 下载管理、进度跟踪 |
| 🌙 **暗黑模式** | 自动定时切换深色主题 |
| 📜 **播放历史** | 续播提示、历史记录管理 |

## 🚀 v2.1.0 新功能

### Sprint 6: 离线下载
- 📥 下载管理器 - 统一的下载队列管理
- ⏸️ 暂停/继续/取消 - 灵活的下载控制
- 📊 实时进度 - 详细的下载状态显示
- 🔄 失败重试 - 自动重试失败的下载任务

### Sprint 5: 家长控制
- 🔐 4位 PIN 码保护 (遥控器输入)
- 🎯 年龄分级过滤 (G/PG/PG-13/R/NC-17)
- 🎭 媒体类型过滤 (电影/剧集/动漫)
- ⏰ 每日观看时长限制

### Sprint 4: 个性化推荐
- 🤖 智能推荐算法 - 基于观看历史
- 🔍 相似内容发现 - Jaccard 相似度匹配
- 🆕 最新上映 - 热门新内容推荐
- 📺 TV 遥控器优化的 Tab 导航

### Sprint 3: 用户体验优化
- 📍 续播提示弹窗
- 📜 播放历史记录
- 🎬 海报进度标签
- 🌙 自动暗黑模式

## 🆚 与 Windows 客户端对比

| 特性 | Windows | Android TV |
|------|---------|-------------|
| 运行平台 | Windows 10/11 | Android TV / Fire TV |
| 交互方式 | 键鼠/触控 | 遥控器/D-pad |
| UI 框架 | React + Tailwind | Jetpack Compose |
| 桌面框架 | Tauri (Rust) | 原生 Android |
| NAS 协议 | SMB/NFS/WebDAV | 待扩展 |

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 平台 | Android TV (Leanback) |
| 语言 | Kotlin |
| UI | Jetpack Compose + Compose TV |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 网络请求 | Retrofit + OkHttp |
| 图片加载 | Coil |
| 视频播放 | ExoPlayer (Media3) |
| 状态管理 | StateFlow + ViewModel |
| 本地存储 | DataStore + Room |

## 📁 项目结构

```
moviecode-tv/
├── app/src/main/java/com/moviecode/tv/
│   ├── MainActivity.kt           # 主入口
│   ├── MovieCodeApp.kt          # Application 类
│   ├── data/
│   │   ├── api/                 # TMDB API 接口
│   │   │   ├── TmdbApi.kt
│   │   │   └── model/          # 数据模型
│   │   ├── local/               # 本地数据库
│   │   └── repository/          # 数据仓库
│   │       ├── MediaRepository.kt       # TMDB 数据
│   │       ├── MediaScannerRepository.kt # 本地扫描
│   │       ├── HistoryRepository.kt     # 历史记录
│   │       ├── RecommendationRepository.kt # 推荐
│   │       ├── ParentalControlRepository.kt # 家长控制
│   │       └── DownloadRepository.kt     # 下载
│   ├── domain/model/            # 领域模型
│   ├── di/                      # Hilt 依赖注入
│   └── ui/
│       ├── components/           # TV 专用组件
│       │   ├── MediaPosterCard.kt   # 海报卡片
│       │   ├── TvNavigationRail.kt  # 导航栏
│       │   ├── ResumeDialog.kt      # 续播弹窗
│       │   └── ProgressPosterCard.kt # 进度海报
│       ├── screens/             # 页面
│       │   ├── HomeScreen.kt       # 首页
│       │   ├── DetailScreen.kt      # 详情页
│       │   ├── PlayerScreen.kt     # 播放器
│       │   ├── SettingsScreen.kt    # 设置页
│       │   ├── HistoryScreen.kt     # 历史记录
│       │   ├── RecommendScreen.kt  # 推荐发现
│       │   ├── ParentalControlScreen.kt # 家长控制
│       │   └── DownloadScreen.kt   # 下载管理
│       └── theme/                # 主题配置
├── build.gradle.kts              # Gradle 配置
├── BUILD_GUIDE.md               # 构建指南
└── README.md
```

## 🔧 构建指南

### 前置要求

- **Java JDK 17+** - [下载地址](https://adoptium.net/)

- **Android SDK** - [下载地址](https://developer.android.com/studio#command-line-tools-only)

### 环境配置

详见 [BUILD_GUIDE.md](./BUILD_GUIDE.md)

### 快速构建

```bash
# 进入项目目录
cd moviecode-tv

# 使用 Gradle 构建
./gradlew assembleDebug

# 或运行构建脚本 (Windows)
./build.bat
```

### APK 输出位置

```
app/build/outputs/apk/debug/app-debug.apk
```

## 📱 安装到 Android TV

1. 启用 TV 的"开发者选项"和"USB 调试"

2. 通过 ADB 安装:
   ```bash
   adb install app-debug.apk
   ```
3. 或将 APK 复制到 U 盘，通过文件管理器安装

## 🔑 TMDB API

本项目使用 [TMDB](https://www.themoviedb.org/) API 获取影视元数据。


> 如需使用自己的 API Key，请在 [TMDB](https://www.themoviedb.org/settings/api) 注册获取（免费）。

## 📋 更新日志

### v2.1.0 (2026-03-22)
- ✨ 新增离线下载功能
- ✨ 新增家长控制 (PIN保护/年龄分级)
- ✨ 新增个性化推荐
- ✨ 新增播放历史管理
- ✨ 新增自动暗黑模式
- 🐛 修复已知问题

### v2.0.0 (2026-03-22)
- 🎨 全新 UI 设计
- 🚀 Jetpack Compose 重构
- ⚡ 性能优化

## 📦 相关项目

| 项目 | 仓库 | 平台 |
|------|------|------|
| MovieCode (Windows) | [MovieCode-Windows](https://github.com/2488652el/MovieCode-Windows) | Windows |
| MovieCode TV | 本项目 | Android TV |

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🙏 致谢

- [TMDB](https://www.themoviedb.org/) - 提供精彩的影视元数据
- [Jetpack Compose](https://developer.android.com/compose) - 现代 Android UI 工具包
- [ExoPlayer](https://developer.android.com/exoplayer) - 强大的媒体播放器
