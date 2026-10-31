@echo off
setlocal
cd /d "%~dp0"

echo =========================================
echo Packaging du frmaework-11.0.jar (bytecode Java 8, JDK 25)
echo =========================================

call mvn clean package
if errorlevel 1 (
    echo.
    echo [ERREUR] La creation du package a echoue.
    exit /b 1
)

echo.
echo [SUCCES] Package cree : target\frmaework-11.0.jar
exit /b 0
