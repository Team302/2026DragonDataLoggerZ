$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$login = "$user@$hostName"
$key = "./keys/id_ed25519"

# Check if Pi already setup
ssh -i $key -T $login "test -f /home/frc302/this_pi_has_been_setup"
if ($LASTEXITCODE -eq 0) {
    Write-Host "$hostName already setup; aborting."
    exit 1
}

Write-Host "Setting up $hostName..."

# Copy service file
scp -i $key ./deploy/pi-logger/etc/systemd/system/pilogger.service `
    ${login}:/etc/systemd/system/pilogger.service
scp -i $key ./deploy/pi-logger/home/frc302/setup-pi.sh `
    ${login}:/home/frc302/setup-pi.sh

# Run setup commands
ssh -i $key $login "chmod 755 /home/frc302/setup-pi.sh && sudo /home/frc302/setup-pi.sh"

Write-Host "$hostName setup complete."
