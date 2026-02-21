@echo off
setlocal

set USER=frc302
set HOST=dragondataloggerz.local
set LOGIN=%USER%@%HOST%
set KEY=./keys/id_ed25519

ssh -i "%KEY%" "%LOGIN%"

endlocal
