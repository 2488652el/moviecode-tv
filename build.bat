@echo off
echo =============================================
echo   MovieCode TV - Build Script
echo =============================================
echo.

REM Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 17+.
    echo Download: https://adoptium.net/
    exit /b 1
)

REM Check for Android SDK
if not exist "%ANDROID_HOME%\platforms" (
    echo [ERROR] Android SDK not found.
    echo Please set ANDROID_HOME environment variable.
    echo Download: https://developer.android.com/studio
    exit /b 1
)

REM Check for Gradle
where gradle >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Gradle not found in PATH, using wrapper...
    if not exist "gradlew.bat" (
        echo [ERROR] gradlew.bat not found.
        echo Please run: gradle wrapper
        exit /b 1
    )
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

REM Build debug APK
echo [STEP 1/3] Cleaning project...
call %GRADLE_CMD% clean

echo.
echo [STEP 2/3] Building debug APK...
call %GRADLE_CMD% assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed!
    exit /b 1
)

echo.
echo [STEP 3/3] Verifying APK...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo =============================================
    echo   BUILD SUCCESSFUL!
    echo =============================================
    echo.
    echo APK Location:
    echo   %cd%\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo File size:
    powershell -Command "(Get-Item 'app\build\outputs\apk\debug\app-debug.apk').Length / 1MB | ForEach-Object { '{0:N2} MB' -f $_ }"
    echo.
) else (
    echo [ERROR] APK not found!
    exit /b 1
)

pause
