@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Start-Platform.ps1"
if errorlevel 1 (
  echo No se pudo completar el inicio. Revisa el mensaje anterior.
  pause
  exit /b 1
)
start "" "http://127.0.0.1:55193/login"
