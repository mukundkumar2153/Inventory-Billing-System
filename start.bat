@echo off
title SmartStore POS & Inventory
cd /d "%~dp0"

echo =========================================================
echo   Starting SmartStore POS & Inventory System...
echo =========================================================

set CP=.;C:\Users\mukund kumar\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.19\tomcat-embed-core-10.1.19.jar;C:\Users\mukund kumar\.m2\repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\mukund kumar\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar

echo [1/3] Compiling Java code...
javac -cp "%CP%" Main.java App.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Opening browser at http://localhost:8080/Main ...
start "" "http://localhost:8080/Main"

echo [3/3] Starting Server...
echo ---------------------------------------------------------
echo   SmartStore is running on http://localhost:8080/Main
echo   Close this window to stop the server.
echo ---------------------------------------------------------

java -cp "%CP%" App
pause
