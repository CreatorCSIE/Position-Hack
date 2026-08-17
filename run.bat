@echo off
rem ============================================================
rem  Position Hack run script (JRE only, no JDK needed)
rem  Usage:
rem    run.bat                launch the injector GUI
rem    run.bat --attach PID   inject into the given JVM directly
rem    run.bat mock           run the mock validation (needs: compile.bat test)
rem ============================================================
setlocal EnableDelayedExpansion

set "ROOT=%~dp0"
cd /d "%ROOT%"

rem Java detection order:
rem   1. JAVA_HOME (common on CI / servers)
rem   2. auto-probe %ProgramFiles%\Java for jdk1.8* / jre1.8*
rem   3. java on PATH
rem attach API comes from lib\tools.jar and native attach.dll from natives\, no JDK needed.
set "JAVA="
if defined JAVA_HOME if exist "!JAVA_HOME!\bin\java.exe" set "JAVA=!JAVA_HOME!\bin\java.exe"
if not defined JAVA (
    for /d %%d in ("%ProgramFiles%\Java\jdk1.8*" "%ProgramFiles%\Java\jre1.8*") do (
        if not defined JAVA if exist "%%d\bin\java.exe" set "JAVA=%%d\bin\java.exe"
    )
)
if not defined JAVA (
    where java >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Java not found. Please install Java 8 or newer.
        exit /b 1
    )
    set "JAVA=java"
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

rem java.library.path points at natives\ so System.loadLibrary("attach") finds attach.dll
"%JAVA%" -Djava.library.path=natives -cp "%JAR%;lib\tools.jar" org.github.creatorcsie.positionhack.loader.Main %*
exit /b %errorlevel%