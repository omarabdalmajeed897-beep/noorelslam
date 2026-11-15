@rem Gradle startup script for Windows
@echo off
setlocal
set DIRNAME=%~dp0
if exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
  java -Xmx64m -Xms64m -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>&1
if %ERRORLEVEL%==0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Open this NoorAlIslamAndroid folder directly in Android Studio, then click Build ^> Build APK(s).
pause
