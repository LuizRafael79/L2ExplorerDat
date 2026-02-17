@echo off
setlocal enabledelayedexpansion
title L2ExplorerDat Console

set JAVA_CMD=
where java >nul 2>&1
if %errorlevel%==0 (
    set JAVA_CMD=java
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set JAVA_CMD=%JAVA_HOME%\bin\java.exe
    )
)
if not defined JAVA_CMD (
    echo ERROR: Java not found in PATH or JAVA_HOME
    pause
    exit /b 1
)

for /f "tokens=2 delims== " %%V in ('"%JAVA_CMD% -version 2>&1 | findstr /i "version""') do set RAW_VERSION=%%V
set RAW_VERSION=%RAW_VERSION:"=%
for /f "delims=. tokens=1" %%M in ("%RAW_VERSION%") do set JAVA_MAJOR=%%M

if not defined JAVA_MAJOR (
    echo ERROR: Could not detect Java version
    pause
    exit /b 1
)

if %JAVA_MAJOR% LSS 25 (
    echo ERROR: Java %JAVA_MAJOR% detected. Java 25+ required.
    pause
    exit /b 1
)

echo Using Java %JAVA_MAJOR%

"%JAVA_CMD%" ^
 -splash:images\splash.png ^
 -Dfile.encoding=UTF-8 ^
 -Djava.util.logging.manager=org.l2explorer.log.AppLogManager ^
 --enable-native-access=ALL-UNNAMED ^
 -Xms1g ^
 -Xmx2g ^
 -jar libs\L2ExplorerDat.jar ^
 -debug

endlocal
pause