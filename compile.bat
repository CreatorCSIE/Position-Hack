@echo off
rem ============================================================
rem  Position Hack build script (JDK 8)
rem  Usage:
rem    compile.bat        compile main sources and package position-hack.jar
rem    compile.bat test   also compile mock tests into build-test\ for run.bat mock
rem ============================================================
setlocal

set "JDK8=C:\Program Files\Java\jdk1.8.0_202"
set "TOOLS_SRC=%JDK8%\lib\tools.jar"
set "DLL_SRC=%JDK8%\jre\bin\attach.dll"
set "ROOT=%~dp0"
cd /d "%ROOT%"

if not exist "%JDK8%\bin\javac.exe" (
    echo [ERROR] JDK8 not found at: %JDK8%
    echo         Please edit the JDK8 variable at the top of compile.bat
    exit /b 1
)

set "SRC=src\org\github\creatorcsie\positionhack"
set "BUILD=build"
set "OUT=%ROOT%position-hack.jar"
set "TMPLIST=%TEMP%\position-hack-sources.txt"
set "TMPMF=%TEMP%\position-hack-manifest.mf"
set "TMPTESTLIST=%TEMP%\position-hack-test-sources.txt"
set "LIBDIR=lib"
set "TOOLS=%LIBDIR%\tools.jar"
set "NATIVES=natives"
set "DLL=%NATIVES%\attach.dll"

echo [1/6] Preparing runtime dependencies (lib\tools.jar + natives\attach.dll)...
rem 发布物依赖两个 JDK 自带的运行时资源，会随 release zip 一起分发：
rem   1. lib\tools.jar   提供 com.sun.tools.attach / sun.tools.attach 等 attach API，
rem                       JRE 没有这些类；
rem   2. natives\attach.dll 是 WindowsAttachProvider 静态初始化时
rem                       System.loadLibrary("attach") 所需的原生库，JRE 里也没有。
rem 每次编译都强制从 JDK 覆盖复制，避免旧文件残留（之前出现过复制的文件不完整，
rem 导致 %1 not a valid Win32 application / error in opening zip file）。
if not exist "%LIBDIR%" mkdir "%LIBDIR%"
if not exist "%TOOLS_SRC%" (
    echo [ERROR] tools.jar not found in JDK: %TOOLS_SRC%
    exit /b 1
)
copy /y "%TOOLS_SRC%" "%TOOLS%" >nul
if errorlevel 1 (
    echo [ERROR] Failed to copy tools.jar
    exit /b 1
)
if not exist "%NATIVES%" mkdir "%NATIVES%"
if not exist "%DLL_SRC%" (
    echo [WARN] attach.dll not found in JDK, multiprocess attach will fail
) else (
    copy /y "%DLL_SRC%" "%DLL%" >nul
    if errorlevel 1 (
        echo [ERROR] Failed to copy attach.dll
        exit /b 1
    )
)

echo [2/6] Cleaning build dir...
rem 清理失败（文件被占用）时继续，直接覆盖编译/打包
if exist "%BUILD%" rmdir /s /q "%BUILD%" 2>nul
mkdir "%BUILD%" 2>nul

echo [3/6] Collecting sources...
rem 中间文件（源文件清单 / MANIFEST.MF）写在 %TEMP%，不落入 build，也不会被卷进 jar
dir /b /s /a-d "%SRC%\*.java" > "%TMPLIST%"

echo [4/6] Compiling (Java 7 bytecode for old Minecraft JVMs)...
"%JDK8%\bin\javac.exe" -encoding UTF-8 -source 1.7 -target 1.7 -cp "%TOOLS%" -d "%BUILD%" "@%TMPLIST%"
if errorlevel 1 (
    echo [ERROR] Compile failed
    exit /b 1
)

echo [5/6] Packaging %OUT%...
(
    echo Main-Class: org.github.creatorcsie.positionhack.loader.Main
    echo Agent-Class: org.github.creatorcsie.positionhack.agent.AgentMain
    echo Can-Retransform-Classes: true
    echo.
) > "%TMPMF%"
rem 只打包编译产物目录 build\org（class 文件），sources.txt / MANIFEST.MF 不入 jar
"%JDK8%\bin\jar.exe" cfm "%OUT%" "%TMPMF%" -C "%BUILD%" org
if errorlevel 1 (
    echo [ERROR] Packaging failed
    exit /b 1
)
del "%TMPMF%" 2>nul

echo [6/6] Done: %OUT%
echo           release with:  %TOOLS%  and  %DLL%  (keep them next to the jar)

if /i "%~1"=="test" (
    echo.
    echo [TEST] Compiling mock tests...
    mkdir "build-test" 2>nul
    dir /b /s /a-d "test\*.java" > "%TMPTESTLIST%"
    "%JDK8%\bin\javac.exe" -encoding UTF-8 -source 1.7 -target 1.7 -cp "%BUILD%;%TOOLS%" -d "build-test" "@%TMPTESTLIST%"
    if errorlevel 1 (
        echo [ERROR] Mock compile failed
        exit /b 1
    )
    del "%TMPTESTLIST%" 2>nul
    echo [TEST] Done: build-test\
)

endlocal