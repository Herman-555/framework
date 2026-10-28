@echo off
setlocal
pushd "%~dp0"

echo =========================================
echo Compilation de app-test-10 (Java 8, JDK 25)
echo =========================================

pushd ..
call compile.bat
if errorlevel 1 (
    popd
    popd
    exit /b 1
)
copy /Y target\framework-10.0.jar app-test-10\src\main\webapp\WEB-INF\lib\framework-10.0.jar >nul
popd
if errorlevel 1 exit /b 1

call mvn clean package
if errorlevel 1 (
    echo [ERREUR] La compilation de app-test-10 a echoue.
    popd
    exit /b 1
)

echo [SUCCES] target\app-test-10.war
popd
exit /b 0
