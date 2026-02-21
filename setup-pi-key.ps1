$ErrorActionPreference = "Stop"

$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$key = ".\keys\id_ed25519"
$authFile = ".\deploy\pi-logger\home\frc302\.ssh\authorized_keys"

$login = "$user@$hostName"

# Create .ssh directory and set permissions
ssh $login "mkdir -p ~/.ssh && chmod 700 ~/.ssh"
if ($LASTEXITCODE -ne 0) { throw "Failed to create .ssh directory" }

# Copy authorized_keys file 
scp $authFile "${login}:~/.ssh/authorized_keys"
if ($LASTEXITCODE -ne 0) { throw "Failed to copy authorized_keys" }

# Set permissions
ssh $login "chmod 600 ~/.ssh/authorized_keys"
if ($LASTEXITCODE -ne 0) { throw "Failed to set permissions on authorized_keys" }

Write-Host "Pi key setup complete."

