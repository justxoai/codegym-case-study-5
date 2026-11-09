@echo off
setlocal

set "JAR=%~1"
set "DIR=%~2"
set "PORT=%~3"
if "%PORT%"=="" set "PORT=8081"

if "%JAR%"=="" (
    echo Usage: deploy.bat ^<jar^> ^<deploy-dir^> [port]
    exit /b 1
)
if "%DIR%"=="" (
    echo Usage: deploy.bat ^<jar^> ^<deploy-dir^> [port]
    exit /b 1
)

if not exist "%DIR%" mkdir "%DIR%"

if exist "%DIR%\app.pid" (
    for /f %%p in (%DIR%\app.pid) do (
        echo Stopping old instance %%p
        taskkill /F /PID %%p >nul 2>&1
    )
    timeout /t 3 /nobreak >nul
)

copy /Y "%JAR%" "%DIR%\clinicbook.jar" >nul
if errorlevel 1 exit /b 1

set "JENKINS_NODE_COOKIE=dontKillMe"
cd /d "%DIR%"
powershell -NoProfile -Command "$p = Start-Process -FilePath 'java' -ArgumentList '-jar','clinicbook.jar','--server.port=%PORT%' -RedirectStandardOutput 'clinicbook.log' -RedirectStandardError 'clinicbook.err.log' -WindowStyle Hidden -PassThru; $p.Id | Out-File -Encoding ascii app.pid"

echo Started ClinicBook on port %PORT%, log: %DIR%\clinicbook.log
exit /b 0
