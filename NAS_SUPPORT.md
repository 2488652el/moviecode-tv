# NAS Support for MovieCode TV

## 新增功能

本更新为 Android TV 版添加了 NAS 协议支持。

### 支持的协议

| 协议 | 状态 | 说明 |
|------|------|------|
| SMB/CIFS | ✅ 已实现 | Windows 文件共享 |
| WebDAV | ✅ 已实现 | HTTP 文件访问 |
| 本地目录 | ✅ 已实现 | 设备本地存储 |
| NFS | 🔄 计划中 | Linux/Unix 文件系统 |

## 新增文件

```
app/src/main/java/com/moviecode/tv/data/repository/
├── NasModels.kt      # 数据模型 (NasConfig, FileInfo)
└── NasRepository.kt  # NAS 操作逻辑
```

## 依赖更新

### build.gradle.kts (项目根目录)
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
}
```

### app/build.gradle.kts
```kotlin
// NAS Support
implementation("eu.agno3.jcifs:jcifs-ng:2.1.8")  // SMB
implementation("com.squareup.okhttp3:okhttp:4.12.0")  // WebDAV
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

## 使用方法

### 1. SMB 连接
```kotlin
val repository = NasRepository(context)
val result = repository.testSmbConnection(
    host = "192.168.1.100",
    port = 445  // 默认
)
```

### 2. WebDAV 连接
```kotlin
val result = repository.testWebdavConnection(
    url = "https://your-nas.com/webdav",
    username = "user",
    password = "pass"
)
```

### 3. 本地目录
```kotlin
val result = repository.testLocalPath("/storage/emulated/0/Movies")
val files = repository.listLocalFiles("/storage/emulated/0/Movies")
```

## TODO

- [ ] 完成 SMB 文件浏览功能
- [ ] 完成 WebDAV 文件解析
- [ ] 添加 Settings 页面 NAS 配置 UI
- [ ] 添加 NAS 连接状态持久化
- [ ] 实现视频播放时的流媒体传输
