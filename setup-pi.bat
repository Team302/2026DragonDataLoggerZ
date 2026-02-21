
@echo off
setlocal

set USER=frc302
set HOST=dragondataloggerz.local
set LOGIN=%USER%@%HOST%
set KEY=./keys/id_ed25519

REM Check if Pi already setup

REM Robust check for marker file and SSH errors
set MARKER_CHECK_FILE=__pi_marker_check.txt
del /f /q %MARKER_CHECK_FILE% 2>nul
ssh -i %KEY% -T %LOGIN% "test -f /home/frc302/this_pi_has_been_setup && echo MARKER_EXISTS" > %MARKER_CHECK_FILE% 2>&1
set CHECK_RESULT=%ERRORLEVEL%
findstr /C:"MARKER_EXISTS" %MARKER_CHECK_FILE% >nul
if %ERRORLEVEL%==0 (
    echo %HOST% already setup; aborting.
    del /f /q %MARKER_CHECK_FILE%
    exit /b 1
)
if not %CHECK_RESULT%==0 (
    echo SSH connection or command failed. See details below:
    type %MARKER_CHECK_FILE%
    del /f /q %MARKER_CHECK_FILE%
    exit /b 2
)
del /f /q %MARKER_CHECK_FILE%

echo Setting up %HOST%...

REM Copy service file
scp -i %KEY% ./deploy/pi-logger/etc/systemd/system/pilogger.service %LOGIN%:/tmp/pilogger.service
scp -i %KEY% ./deploy/pi-logger/home/frc302/setup-pi.sh %LOGIN%:/home/frc302/setup-pi.sh

REM Run setup script remotely
ssh -i %KEY% %LOGIN% "chmod 755 /home/frc302/setup-pi.sh && sudo /home/frc302/setup-pi.sh"
if not %ERRORLEVEL%==0 (
    echo Remote setup script failed with error %ERRORLEVEL%.
    exit /b 3
)

echo %HOST% setup complete.

endlocal
