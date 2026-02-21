$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$login = "$user@$hostName"
$key = "./keys/id_ed25519"

Write-Host "Deploying to $hostName..."

# Copy service file
scp -i $key ./build/libs/PiLogger-linuxarm64-cross.jar `
    ${login}:/tmp/PiLogger-linuxarm64-cross.jar
scp -i $key ./deploy/pi-logger/home/frc302/deploy-pi.sh `
    ${login}:/home/frc302/deploy-pi.sh

ssh -i $key $login "chmod +x /home/frc302/deploy-pi.sh && /home/frc302/deploy-pi.sh"

Write-Host "$hostName deploy complete."
