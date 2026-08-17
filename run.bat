@echo off
rem ============================================================
rem  Position Hack run script (JRE only, no JDK needed)
rem  Usage:
rem    run.bat                launch the injector GUI
rem    run.bat --attach PID   inject into the given JVM directly
rem    run.bat mock           run the mock validation (needs: compile.bat test)
rem ============================================================
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

rem 优先使用 JDK/bin/java.exe（如有），否则回退到 JRE。
rem attach API 由 lib\tools.jar 提供，attach.dll 由 natives\ 提供，无需 JDK。
set "JAVA=java"
if exist "C:\Program Files\Java\jdk1.8.0_202\bin\java.exe" (
    set "JAVA=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe"
) else if exist "C:\Program Files\Java\jre1.8.0_431\bin\java.exe" (
    set "JAVA=C:\Program Files\Java\jre1.8.0_431\bin\java.exe"
) else (
    where java >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Java not found. Please install Java 8 or newer.
        exit /b 1
    )
)

if /i "%~1"=="mock" (
    if not exist "build-test\org\github\creatorcsie\positionhack\mock\MockMain.class" (
        echo [ERROR] Mock tests are not compiled yet. Run: compile.bat test
        exit /b 1
    )
    "%JAVA%" -cp "build;build-test" org.github.creatorcsie.positionhack.mock.MockMain
    exit /b %errorlevel%
)

set "JAR=%ROOT%position-hack.jar"
if not exist "%JAR%" (
    echo [ERROR] %JAR% not found. Run compile.bat first.
    exit /b 1
)
if not exist "lib\tools.jar" (
    echo [ERROR] lib\tools.jar not found. Run compile.bat first.
    exit /b 1
)

rem java.library.path 指向 natives\，供 System.loadLibrary("attach") 找到 attach.dll
"%JAVA%" -Djava.library.path=natives -cp "%JAR%;lib\tools.jar" org.github.creatorcsie.positionhack.loader.Main %*
exit /b %errorlevel%