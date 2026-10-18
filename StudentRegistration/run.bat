@echo off
setlocal
title Student Registration - Build and Run

REM ===================== EDIT THESE 2 LINES IF NEEDED =====================
set "TOMCAT=C:\Program Files\Apache Software Foundation\Tomcat 9.0"
set "MVN=C:\Users\mukund kumar\.vscode\extensions\oracle.oracle-java-27.0.0\nbcode\java\maven\bin\mvn.cmd"
REM =========================================================================

REM --- Auto-elevate to Administrator if not already ---
net session >nul 2>&1
if errorlevel 1 (
    echo Requesting Administrator access...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"

echo ================================================================
echo  Student Registration - Build and Run
echo ================================================================
echo.

echo [1/3] Building WAR with Maven...
call "%MVN%" clean package -q
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed! Run Maven manually to see errors:
    echo   mvn clean package
    pause
    exit /b 1
)
echo  Build successful.
echo.

echo [2/3] Deploying WAR to Tomcat...
if not exist "%TOMCAT%\bin\catalina.bat" (
    echo [ERROR] Tomcat not found at:
    echo   %TOMCAT%
    echo Edit the TOMCAT line in run.bat
    pause
    exit /b 1
)
if exist "%TOMCAT%\webapps\StudentRegistration" rmdir /s /q "%TOMCAT%\webapps\StudentRegistration"
copy /y "target\StudentRegistration.war" "%TOMCAT%\webapps\" >nul
if errorlevel 1 (
    echo [ERROR] Could not copy WAR to Tomcat webapps.
    pause
    exit /b 1
)
echo  WAR deployed successfully.
echo.

echo [3/3] Starting Tomcat...
echo.
echo ================================================================
echo  App running at: http://localhost:8080/StudentRegistration/
echo  Press Ctrl+C to stop Tomcat.
echo ================================================================
echo.

start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:8080/StudentRegistration/"
call "%TOMCAT%\bin\catalina.bat" run