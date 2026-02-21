$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$login = "$user@$hostName"
$key = "./keys/id_ed25519"

ssh -i $key $login