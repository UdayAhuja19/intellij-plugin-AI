@echo off
REM Run the IntelliJ plugin in a sandbox IDE
REM Works on Windows
REM Note: Build requires Java 17 (Gradle/Kotlin toolchain limitation)

setlocal enabledelayedexpansion

echo Looking for Java...

REM Try Java 17 first (required for Gradle), then 21
for %%v in (17 21) do (
    REM Oracle JDK
    if exist "C:\Program Files\Java\jdk-%%v\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-%%v"
        echo Found Java %%v at: !JAVA_HOME!
        goto :found
    )
    
    REM OpenJDK via Adoptium/Eclipse Temurin
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-%%v*") do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_HOME=%%d"
            echo Found Java %%v at: !JAVA_HOME!
            goto :found
        )
    )
    
    REM Microsoft OpenJDK
    for /d %%d in ("C:\Program Files\Microsoft\jdk-%%v*") do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_HOME=%%d"
            echo Found Java %%v at: !JAVA_HOME!
            goto :found
        )
    )
    
    REM Zulu JDK
    for /d %%d in ("C:\Program Files\Zulu\zulu-%%v*") do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_HOME=%%d"
            echo Found Java %%v at: !JAVA_HOME!
            goto :found
        )
    )
)

REM Check for Java 25 with warning
for %%v in (25) do (
    if exist "C:\Program Files\Java\jdk-%%v\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-%%v"
        echo WARNING: Only Java 25 found. Gradle may have compatibility issues.
        echo          For best results, install Java 17 from https://adoptium.net/
        goto :found
    )
)

REM If nothing found, try using existing JAVA_HOME
if defined JAVA_HOME (
    echo Using existing JAVA_HOME: %JAVA_HOME%
    goto :found
)

REM Try to find java in PATH
where java >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('where java') do (
        set "JAVA_BIN=%%i"
        goto :foundpath
    )
)

echo Java not found. Please install Java 17.
echo Download from: https://adoptium.net/
exit /b 1

:foundpath
REM Extract JAVA_HOME from java binary path
for %%i in ("%JAVA_BIN%") do set "JAVA_HOME=%%~dpi.."
echo Using Java from PATH: %JAVA_HOME%

:found
REM Verify Java version
for /f "tokens=3" %%g in ('"%JAVA_HOME%\bin\java" -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%g"
)
set "JAVA_VER=%JAVA_VER:"=%"
for /f "tokens=1 delims=." %%a in ("%JAVA_VER%") do set "JAVA_MAJOR=%%a"

echo Java version: %JAVA_MAJOR%

if %JAVA_MAJOR% LSS 17 (
    echo Java 17 or higher is required ^(found version %JAVA_MAJOR%^)
    exit /b 1
)

if %JAVA_MAJOR% GEQ 25 (
    echo.
    echo WARNING: Java 25 detected. Gradle 8.x doesn't fully support Java 25 yet.
    echo          If build fails, please install Java 17 from https://adoptium.net/
    echo.
)

echo Starting IntelliJ plugin sandbox...
echo.

REM Run Gradle
gradlew.bat runIde

endlocal
