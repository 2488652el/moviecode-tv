# MovieCode TV - 构建指南

## 环境状态

✅ **已完成安装:**
- **Java JDK 17.0.18** - 位于 `F:\AndroidDev\jdk\jdk-17.0.18+8`

⏳ **需要手动完成:**
- **Android SDK** - 由于网络限制需要手动下载安装

---

## 步骤1: 下载Android SDK命令行工具

### 方法1: 使用浏览器直接下载
1. 打开浏览器访问: https://developer.android.com/studio#command-line-tools-only
2. 下载 Windows 版 command-line tools
3. 将下载的文件重命名为 `commandlinetools.zip`

### 方法2: 使用镜像下载
如果上述地址无法访问，尝试以下镜像:
- 腾讯镜像: https://mirrors.cloud.tencent.com/android/repository/
- 华为镜像: https://repo.huaweicloud.com/AndroidStudio/
- 阿里云: https://mirrors.aliyun.com/android/repository/

---

## 步骤2: 安装Android SDK

### 2.1 解压SDK工具
```powershell
# 创建SDK目录
New-Item -ItemType Directory -Force -Path "F:\AndroidDev\sdk"

# 解压命令行工具到指定位置
Expand-Archive -Path "下载的commandlinetools.zip路径" -DestinationPath "F:\AndroidDev\sdk" -Force
```

### 2.2 手动创建目录结构
确保目录结构如下:
```
F:\AndroidDev\sdk\
├── cmdline-tools\
│   └── latest\
│       ├── bin\
│       │   ├── sdkmanager.bat
│       │   └── ...
│       └── lib\
│           └── ...
```

---

## 步骤3: 安装必需的SDK组件

打开命令提示符(PowerShell)并运行:

```powershell
# 设置环境变量
$env:JAVA_HOME = "F:\AndroidDev\jdk\jdk-17.0.18+8"
$env:PATH = "$env:PATH;$env:JAVA_HOME\bin"

# 接受许可协议
F:\AndroidDev\sdk\cmdline-tools\latest\bin\sdkmanager.bat --licenses

# 安装必需的组件
F:\AndroidDev\sdk\cmdline-tools\latest\bin\sdkmanager.bat `
    "platform-tools" `
    "platforms;android-34" `
    "build-tools;34.0.0"
```

---

## 步骤4: 设置用户环境变量(可选)

打开系统属性 → 环境变量，添加用户变量:

```
JAVA_HOME = F:\AndroidDev\jdk\jdk-17.0.18+8
ANDROID_HOME = F:\AndroidDev\sdk
PATH 添加 = %JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools
```

---

## 步骤5: 构建APK

```powershell
# 进入项目目录
Set-Location F:\test\moviecode-tv

# 使用Gradle构建
.\gradlew.bat assembleDebug
```

或直接双击运行 `build.bat` 脚本

---

## APK输出位置

构建成功后，APK文件位于:
```
F:\test\moviecode-tv\app\build\outputs\apk\debug\app-debug.apk
```

---

## 常见问题

### Q1: sdkmanager无法运行
**A:** 确保JAVA_HOME环境变量已正确设置

### Q2: 许可协议无法接受
**A:** 以管理员身份运行PowerShell，然后执行:
```powershell
echo y | F:\AndroidDev\sdk\cmdline-tools\latest\bin\sdkmanager.bat --licenses
```

### Q3: 构建失败提示找不到Gradle
**A:** 项目已包含Gradle Wrapper，确保网络连接后会自动下载

---

## 技术支持

如遇问题，请检查:
1. Java版本: `java -version` (需要17.x)
2. SDK路径: 确保local.properties中sdk.dir正确
3. 环境变量: JAVA_HOME和ANDROID_HOME

---

**最后更新:** 2026-03-19
