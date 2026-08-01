@echo off
rem Stage 3 relay launcher (Windows).
rem Usage:  relay.bat <TV_IP> [port]
rem Example: relay.bat 192.168.1.50
set TV=%~1
set PORT=%~2
if "%PORT%"=="" set PORT=8080
if "%TV%"=="" (
    echo Usage: relay.bat ^<TV_IP^> [port]  - e.g. relay.bat 192.168.1.50
    py -3 "%~dp0relay.py" --port %PORT%
) else (
    py -3 "%~dp0relay.py" --tv %TV% --port %PORT%
)
pause
