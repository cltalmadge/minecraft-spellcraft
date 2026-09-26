@echo off
setlocal
set "GRADLE_VERSION=9.7.1"
set "BASE=%USERPROFILE%\.gradle\spellcraft-bootstrap\gradle-%GRADLE_VERSION%"
set "DIST_DIR=%BASE%\gradle-%GRADLE_VERSION%"
set "ZIP=%BASE%\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%DIST_DIR%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  if not exist "%ZIP%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%URL%' -OutFile '%ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%BASE%' -Force"
  if errorlevel 1 exit /b 1
)

call "%DIST_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
