@echo off
setlocal

set USER=frc302
set HOST=dragondataloggerz.local
set LOGIN=%USER%@%HOST%
set KEY=./keys/id_ed25519

echo Deploying to %HOST%...

REM Copy the jar file
scp -i %KEY% ./build/libs/PiLogger-linuxarm64-cross.jar %LOGIN%:/tmp/PiLogger-linuxarm64-cross.jar

REM Copy the deploy script
scp -i %KEY% ./deploy/pi-logger/home/frc302/deploy-pi.sh %LOGIN%:/home/frc302/deploy-pi.sh

REM Make the script executable and run it
ssh -i %KEY% %LOGIN% "chmod +x /home/frc302/deploy-pi.sh && /home/frc302/deploy-pi.sh"

echo %HOST% deploy complete.

endlocal
