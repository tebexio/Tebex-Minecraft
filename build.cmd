@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat build %*
exit /b %errorlevel%
