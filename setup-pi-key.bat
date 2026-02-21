@echo off
setlocal

set USER=frc302
set HOST=dragondataloggerz.local
set LOGIN=%USER%@%HOST%
set KEY=./keys/id_ed25519
set AUTHFILE=./deploy/pi-logger/home/frc302/.ssh/authorized_keys


echo Creating .ssh directory on Pi...
ssh %LOGIN% "mkdir -p ~/.ssh && chmod 700 ~/.ssh"
if errorlevel 1 (
    echo Failed to create .ssh directory
    exit /b 1
)


echo Copying authorized_keys...
scp %AUTHFILE% %LOGIN%:~/.ssh/authorized_keys
if errorlevel 1 (
    echo Failed to copy authorized_keys
    exit /b 1
)


echo Setting permissions on authorized_keys...
ssh %LOGIN% "chmod 600 ~/.ssh/authorized_keys"
if errorlevel 1 (
    echo Failed to set permissions on authorized_keys
    exit /b 1
)

echo Pi key setup complete.

endlocal
