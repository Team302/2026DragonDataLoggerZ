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
    ${login}:/tmp/pilogger.service
scp -i $key ./deploy/pi-logger/home/frc302/setup-pi.sh `
    ${login}:/home/frc302/setup-pi.sh

# Run setup commands
ssh -i $key $login "chmod 755 /home/frc302/setup-pi.sh && sudo /home/frc302/setup-pi.sh"

Write-Host "$hostName setup complete."

# SIG # Begin signature block
# MIIFXgYJKoZIhvcNAQcCoIIFTzCCBUsCAQExCzAJBgUrDgMCGgUAMGkGCisGAQQB
# gjcCAQSgWzBZMDQGCisGAQQBgjcCAR4wJgIDAQAABBAfzDtgWUsITrck0sYpfvNR
# AgEAAgEAAgEAAgEAAgEAMCEwCQYFKw4DAhoFAAQUd3O8FZ9FPz8O/N2mxkGRxPyv
# KXWgggL+MIIC+jCCAeKgAwIBAgIQdCBf4a6Zb7lBH19DbDbPKjANBgkqhkiG9w0B
# AQsFADAVMRMwEQYDVQQDDApGUkNEZXZDZXJ0MB4XDTI2MDIyMTE4MDIzM1oXDTI3
# MDIyMTE4MjIzM1owFTETMBEGA1UEAwwKRlJDRGV2Q2VydDCCASIwDQYJKoZIhvcN
# AQEBBQADggEPADCCAQoCggEBAMFnR+P68YFywkrNQ/Kmt2xZ4coBzoAFrzIFsJLZ
# P8zpMIPI6jhFF1ClIMuPC6yvRw49AyEwUduudgI+jOZUv9HVjgdNR2LB4F3Jv8/1
# kfHxJEqz1YvDN2qe4lASdN63HaheYOvbZx6d59Tc/LEF7RexLTvFb5qBMDsOgWs9
# gUKFSU5NNA66ep3xU4e7HFzTiDw1W6L1CVJQaRTUhwffavghd4sgihaVVC89zPXU
# V6yXRNEfF/eiZ8zfaF5H05+GsP9DyXdTbL8cTkgdE1gKzStLf9PZ9zUeCqJXe6EG
# fz0DaJOlwoqHE5KE2jmku0/rpMtRiQqvGS0w/g1DK6vmCm0CAwEAAaNGMEQwDgYD
# VR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoGCCsGAQUFBwMDMB0GA1UdDgQWBBTtZa+6
# hXiMBsUlqYMVD/OdBYdzkDANBgkqhkiG9w0BAQsFAAOCAQEAtBUnR67+OQfwtfBD
# HyzYSQm0gDRo72Nn5SvBkEOt5q/UL9lvUSaaJdtF2CNfjsDCBnl7PBPBU60inhxa
# Z+pvHvNZoTH0mAt+lKD+yF4+ktCbNgmBgPK5arPK90anGD8BmR9ezo5vwoZOpDzS
# xCiBiiPXgt11hLtDwS/YL6LNgEL2i+4tKoqVooVT94KCnDFp6tyeof4x/ZJ2sMIH
# 2lQTXymD+Sq57UqPz5soln2Aer09t4XltH1RpZtPgpKghNQXtc4/PC4nRX9fp/I2
# SWrBGsGJDYnfy8Qzy1WvbhBGSaJaKlBVMN+4/wOd2BeaCLLx0ISwH0XkZkOkmLG3
# qmF2hjGCAcowggHGAgEBMCkwFTETMBEGA1UEAwwKRlJDRGV2Q2VydAIQdCBf4a6Z
# b7lBH19DbDbPKjAJBgUrDgMCGgUAoHgwGAYKKwYBBAGCNwIBDDEKMAigAoAAoQKA
# ADAZBgkqhkiG9w0BCQMxDAYKKwYBBAGCNwIBBDAcBgorBgEEAYI3AgELMQ4wDAYK
# KwYBBAGCNwIBFTAjBgkqhkiG9w0BCQQxFgQU+5GgiDQnvIApOysPbWi1Zazy3/0w
# DQYJKoZIhvcNAQEBBQAEggEAgK+yrCUSZInbBhM8a6DheVrLCdwKSzNId1DZbBrH
# V4jzsKhI/PMZO8dODeOa6nE6a2Y7uH/9bRZud2cKRqd543XpNjYIxEKhcyFWFAUF
# kBUxVXuPpuuJ9I5rySUfhb16FOP2oxlrQDxihnFRbv//imwAOPq8uN1bXXyLPqP/
# Df/SzLZ4wzAUubG4tvbjhTywB5Mc94z8sf2IPsVfn7tIrCk1yl+2fcF0dGNB6Jzu
# aoVbWafqYdn4vejYp+JTFji60+9EqNFdfEgFj7bVBhLEauVXklOhrdtctj7/Ag7f
# jtfcGFRy7owq+fezHTeIIzdsC2vEAy50PH4I4ejFqRWZZg==
# SIG # End signature block
