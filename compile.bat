@echo off
REM Suppression du dossier bin et du jar existant
if exist bin rd /s /q bin
if exist framework.jar del /f /q framework.jar

REM Création du dossier de sortie
mkdir bin

REM Compilation des sources Java
javac -source 17 -target 17 -cp "lib/jakarta.servlet-api-6.0.0.jar" -d bin src/framework/*.java

REM Création de l'archive JAR
jar cvf framework.jar -C bin .