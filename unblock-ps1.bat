@echo off
REM ==========================================
REM Batch file to unblock all PowerShell scripts in current directory
REM ==========================================

echo Unblocking all .ps1 scripts in %CD% and subfolders...

powershell -Command "Get-ChildItem -Path '.' -Recurse -Include *.ps1 | Unblock-File"

echo Done unblocking all scripts.
pause