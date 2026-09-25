@echo off
echo =========================================
echo Compilation du Framework Java 8 (JAR)
echo =========================================

REM Nettoyage et packaging via Maven
call mvn clean package

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a échoué. Veuillez vérifier les erreurs ci-dessus.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCÈS] Le framework a été compilé avec succès !
echo Le fichier JAR se trouve dans le dossier 'target/'.
pause