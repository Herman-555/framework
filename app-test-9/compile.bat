@echo off
pushd "%~dp0"

echo =========================================
echo Compilation de app-test-9 (WAR Java 25)
echo =========================================

pushd ..
call compile.bat
IF %ERRORLEVEL% NEQ 0 (
    popd
    popd
    exit /b %ERRORLEVEL%
)
popd

copy /Y ..\target\framework-9.0.jar src\main\webapp\WEB-INF\lib\framework-9.0.jar >nul
IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Le JAR framework-9.0.jar est introuvable.
    pause
    popd
    exit /b 1
)

call mvn clean package

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue. Veuillez verifier les erreurs ci-dessus.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCES] L'application a ete compilee avec succes !
echo Le fichier WAR se trouve dans le dossier 'target/app-test-9.war'.
pause
popd
